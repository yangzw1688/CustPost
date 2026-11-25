package com.kaiyun.modules.cases.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.cases.dto.ServiceRecordSaveDTO;
import com.kaiyun.modules.cases.dto.CaseProgressUpdate;
import com.kaiyun.modules.cases.dto.CaseReviewInput;
import com.kaiyun.common.pagination.AdminListQuery;
import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.modules.cases.entity.ServiceRecord;
import com.kaiyun.modules.cases.entity.ServiceRecordImage;
import com.kaiyun.modules.cases.mapper.ServiceRecordImageMapper;
import com.kaiyun.modules.cases.mapper.ServiceRecordMapper;
import com.kaiyun.infrastructure.storage.ObsStorageService;
import com.kaiyun.mq.CasePushQueue;
import com.kaiyun.modules.cases.service.CaseSubmissionKeys;
import com.kaiyun.modules.policy.service.ServicePolicyService;
import com.kaiyun.modules.project.service.ProjectService;
import com.kaiyun.modules.cases.service.ServiceRecordService;
import com.kaiyun.modules.cases.service.ServiceRecordValidator;
import com.kaiyun.modules.cases.vo.AdminServiceRecordListVO;
import com.kaiyun.modules.cases.vo.ServiceRecordDetailVO;
import com.kaiyun.modules.cases.vo.CaseAttachmentSummaryVO;
import com.kaiyun.modules.cases.vo.ClaimPushCaseVO;
import com.kaiyun.modules.cases.vo.PaidCaseVO;
import com.kaiyun.modules.cases.vo.ServiceRecordListVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** 病例提交与查询；数据库保存业务信息，OBS 保存附件内容。 */
@Service
public class ServiceRecordServiceImpl implements ServiceRecordService {
    private static final Logger log = LoggerFactory.getLogger(ServiceRecordServiceImpl.class);
    private final ServiceRecordMapper serviceRecordMapper;
    private final ServiceRecordImageMapper imageMapper;
    private final ObsStorageService storage;
    private final ServicePolicyService policies;
    private final CaseSubmissionKeys submissionKeys;
    private final ProjectService projects;
    private final CasePushQueue pushQueue;
    private final JdbcTemplate jdbc;

    public ServiceRecordServiceImpl(ServiceRecordMapper serviceRecordMapper,
            ServiceRecordImageMapper imageMapper, ObsStorageService storage, ServicePolicyService policies,
            CaseSubmissionKeys submissionKeys, ProjectService projects, CasePushQueue pushQueue, JdbcTemplate jdbc) {
        this.serviceRecordMapper = serviceRecordMapper;
        this.imageMapper = imageMapper;
        this.storage = storage;
        this.policies = policies;
        this.submissionKeys = submissionKeys;
        this.projects = projects;
        this.pushQueue = pushQueue;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(ServiceRecordSaveDTO dto, MultipartFile[] files, String loginPhone) throws Exception {
        // 联系电话由会话确定，保单选择与后续服务记录使用同一个归属手机号。
        dto.setPhone(loginPhone);
        LocalDate visitDate = ServiceRecordValidator.validateForm(dto);
        policies.requireOwnedPolicy(dto.getPolicyId(), loginPhone);
        projects.validateSubmission(dto.getPolicyId());
        List<String> extensions = ServiceRecordValidator.validateAttachments(files);
        Long existing = submissionKeys.reserve(dto.getRequestId(), dto, visitDate, files);
        if (existing != null) return existing;
        storage.validateConfiguration();

        // OBS 不参与数据库事务：提前注册补偿，在数据库确认回滚后清理本次上传。
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("病例提交必须在事务中执行");
        }
        List<String> uploadedKeys = new ArrayList<>(files.length);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    for (String key : uploadedKeys) {
                        try {
                            storage.delete(key);
                        } catch (Exception exception) {
                            log.error("OBS 回滚清理失败，objectKey={}", key, exception);
                        }
                    }
                } else if (status == STATUS_UNKNOWN && !uploadedKeys.isEmpty()) {
                    // 提交结果未知时保留对象，避免误删已成功入库的病例附件。
                    log.error("数据库提交结果未知，请核对 OBS 对象与病例记录，objectKeys={}", uploadedKeys);
                }
            }
        });

        ServiceRecord record = new ServiceRecord();
        record.setName(dto.getName());
        record.setPhone(dto.getPhone());
        record.setReporterName(dto.getReporterName());
        record.setReporterPhone(loginPhone);
        record.setPolicyId(dto.getPolicyId());
        record.setCaseTitle(dto.getCaseTitle());
        record.setCaseDescription(dto.getCaseDescription());
        record.setVisitDate(visitDate);
        record.setStatus("PENDING");
        LocalDateTime now = LocalDateTime.now();
        record.setCreateTime(now);
        record.setUpdateTime(now);
        if (serviceRecordMapper.insert(record) != 1 || record.getId() == null) {
            throw new IllegalStateException("病例记录未能保存");
        }
        Long recordId = record.getId();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String fileName = UUID.randomUUID().toString().replace("-", "") + extensions.get(i);
            String key = "case/" + recordId + "/" + fileName;
            // 上传超时也可能已写入 OBS，因此在发起请求前记录待清理的对象键。
            uploadedKeys.add(key);
            storage.upload(key, file);
            ServiceRecordImage image = new ServiceRecordImage();
            image.setServiceRecordId(recordId);
            image.setOriginalName(safeOriginalName(file.getOriginalFilename()));
            image.setFileName(fileName);
            image.setFilePath(key);
            image.setFileUrl("obs://" + key);
            image.setSort(i);
            image.setCreateTime(now);
            if (imageMapper.insert(image) != 1) throw new IllegalStateException("附件记录未能保存");
        }
        submissionKeys.complete(dto.getRequestId(), loginPhone, recordId);
        return recordId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceRecordListVO> serviceRecordlist(String phone) {
        return serviceRecordMapper.selectList(new LambdaQueryWrapper<ServiceRecord>()
                        .eq(ServiceRecord::getPhone, phone)
                        .orderByDesc(ServiceRecord::getCreateTime, ServiceRecord::getId))
                .stream().map(record -> {
                    ServiceRecordListVO result = new ServiceRecordListVO();
                    BeanUtils.copyProperties(record, result);
                    return result;
                }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminServiceRecordListVO> adminList() {
        return serviceRecordMapper.selectAdminList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminListPage<AdminServiceRecordListVO> adminPage(AdminListQuery query) {
        long total = serviceRecordMapper.countAdminPage(query);
        int page = query.pageWithin(total);
        return new AdminListPage<>(page, total, serviceRecordMapper.selectAdminPage(query, (page - 1L) * query.getSize()));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminListPage<PaidCaseVO> paidPage(AdminListQuery query) {
        if (query.getFrom() != null && query.getTo() != null && query.getFrom().isAfter(query.getTo())) {
            throw new BusinessException(400, "赔付确认开始日期不能晚于结束日期");
        }
        long total = serviceRecordMapper.countPaidCases(query);
        int page = query.pageWithin(total);
        return new AdminListPage<>(page, total, serviceRecordMapper.selectPaidCases(query, (page - 1L) * query.getSize()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaseAttachmentSummaryVO> casesWithAttachments() {
        return serviceRecordMapper.selectCasesWithAttachments();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimPushCaseVO> listClaimPushCases() {
        return serviceRecordMapper.selectClaimPushCases();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> existingCaseIds(List<Long> ids) {
        return serviceRecordMapper.selectList(new LambdaQueryWrapper<ServiceRecord>()
                        .select(ServiceRecord::getId).in(ServiceRecord::getId, ids))
                .stream().map(ServiceRecord::getId).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceRecordImage> listAttachments(Long id) {
        if (id == null || id <= 0) throw new BusinessException(400, "案件编号不正确");
        List<ServiceRecordImage> images = imageMapper.selectList(new LambdaQueryWrapper<ServiceRecordImage>()
                .eq(ServiceRecordImage::getServiceRecordId, id)
                .orderByAsc(ServiceRecordImage::getSort, ServiceRecordImage::getId));
        for (ServiceRecordImage image : images) {
            String url = image.getFileUrl();
            if (url != null && url.startsWith("obs://")) {
                // 只转换 obs:// 标识；历史本地附件继续使用原有访问路径。
                image.setFileUrl(storage.signedUrl(url.substring("obs://".length())));
            }
        }
        return images;
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRecordDetailVO getDetail(Long id, String phone) {
        if (id == null || id <= 0) throw new BusinessException(400, "案件编号不正确");
        ServiceRecordDetailVO detail = serviceRecordMapper.selectDetailById(id);
        if (detail == null || (phone != null && !phone.equals(detail.getPhone()))) {
            throw new BusinessException(404, "案件不存在");
        }
        if (detail.getPolicyId() != null) {
            com.kaiyun.modules.project.vo.ManagementViews.Project project = projects.projectForPolicy(detail.getPolicyId());
            if (project != null) {
                detail.setProjectId(project.getId());
                detail.setProjectName(project.getName());
                detail.setInsurerName(project.getInsurerName());
            }
        }
        detail.setImages(listAttachments(id));
        return detail;
    }

    @Override
    @Transactional
    public void updateProgress(Long id, CaseProgressUpdate input, String operator) {
        String next = input.getStatus();
        if (!Arrays.asList("PENDING", "PROCESSING", "COMPLETED", "REJECTED", "CANCELLED").contains(next)) {
            throw new BusinessException(400, "请选择有效的案件状态");
        }
        String remark = normalizeRemark(input.getProcessRemark());
        if (Arrays.asList("REJECTED", "CANCELLED").contains(next) && remark.isEmpty()) {
            throw new BusinessException(400, "请填写驳回或取消的原因");
        }
        ServiceRecord current = lockProgress(id, input.getExpectedStatus(), input.getExpectedRemark());
        String status = current.getStatus();
        boolean finishing = "PROCESSING".equals(status) && "COMPLETED".equals(next);
        if (input.isPaymentConfirmed() && !finishing) throw new BusinessException(400, "当前案件无法确认线下赔付");
        if (finishing) {
            if (!input.isPaymentConfirmed()) throw new BusinessException(400, "请在线下赔付后确认完成");
            if (jdbc.update("UPDATE case_payment_review SET payment_confirmed_by=?, payment_confirmed_at=CURRENT_TIMESTAMP "
                            + "WHERE case_id=? AND decision='APPROVED' AND payout_amount>0 AND payment_confirmed_at IS NULL",
                    operator, id) != 1) throw new BusinessException(409, "请先在领款人审核页审核通过并登记赔付金额");
        } else if (!next.equals(status) && !("PENDING".equals(status) && "CANCELLED".equals(next))) {
            throw new BusinessException(400, "请在领款人审核页完成审核");
        }
        if (next.equals(status) && remark.equals(Objects.toString(current.getProcessRemark(), ""))) return;
        persistProgress(current, next, remark);
    }

    @Override
    @Transactional
    public void review(Long id, CaseReviewInput input, String operator) {
        boolean approved = "APPROVED".equals(input.getDecision());
        if (!approved && !"REJECTED".equals(input.getDecision())) throw new BusinessException(400, "请选择审核结果");
        String remark = normalizeRemark(input.getRemark());
        if (!approved && remark.isEmpty()) throw new BusinessException(400, "请填写驳回原因");
        BigDecimal amount = approved ? approvedAmount(input.getPayoutAmount()) : null;
        ServiceRecord current = lockProgress(id, input.getExpectedStatus(), input.getExpectedRemark());
        if (!Arrays.asList("PENDING", "PROCESSING").contains(current.getStatus())
                || jdbc.queryForObject("SELECT COUNT(*) FROM case_payment_review WHERE case_id=?", Integer.class, id) > 0) {
            throw new BusinessException(409, "当前案件已审核或无法审核，请刷新列表");
        }
        jdbc.update("INSERT INTO case_payment_review (case_id, decision, payout_amount, review_remark, reviewer_phone, reviewed_at) "
                + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", id, input.getDecision(), amount, remark, operator);
        persistProgress(current, approved ? "PROCESSING" : "REJECTED", remark);
    }

    private BigDecimal approvedAmount(String value) {
        String amount = value == null ? "" : value.trim();
        if (!amount.matches("[0-9]{1,10}(\\.[0-9]{1,2})?")) {
            throw new BusinessException(400, "请输入有效的人民币赔付金额，最多两位小数");
        }
        BigDecimal result = new BigDecimal(amount);
        if (result.signum() <= 0) throw new BusinessException(400, "赔付金额必须大于零");
        return result;
    }

    private ServiceRecord lockProgress(Long id, String expectedStatus, String expectedRemark) {
        if (id == null || id <= 0) throw new BusinessException(400, "案件编号不正确");
        ServiceRecord current = serviceRecordMapper.selectProgressForUpdate(id);
        if (current == null) throw new BusinessException(404, "案件不存在");
        if (!Objects.equals(current.getStatus(), expectedStatus)
                || !Objects.toString(current.getProcessRemark(), "").equals(Objects.toString(expectedRemark, ""))) {
            throw new BusinessException(409, "案件处理进度已变更，请重新打开后再编辑");
        }
        return current;
    }

    private String normalizeRemark(String value) {
        String remark = value == null ? "" : value.trim();
        if (remark.length() > 200) throw new BusinessException(400, "备注最多200个字符");
        return remark;
    }

    private void persistProgress(ServiceRecord current, String next, String remark) {
        Long id = current.getId();
        serviceRecordMapper.update(null, new LambdaUpdateWrapper<ServiceRecord>().eq(ServiceRecord::getId, id)
                .set(ServiceRecord::getStatus, next).set(ServiceRecord::getProcessRemark, remark)
                .set(ServiceRecord::getUpdateTime, LocalDateTime.now()));
        // 与案件状态一起提交，避免进度回滚后仍发送通知。
        if (!next.equals(current.getStatus())) pushQueue.enqueueStatusChange(id, next);
    }

    private String safeOriginalName(String value) {
        if (value == null) return "image";
        String name = value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }
}
