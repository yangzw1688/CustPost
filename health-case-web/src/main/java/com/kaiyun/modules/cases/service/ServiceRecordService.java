package com.kaiyun.modules.cases.service;

import com.kaiyun.modules.cases.dto.ServiceRecordSaveDTO;
import com.kaiyun.modules.cases.dto.CaseProgressUpdate;
import com.kaiyun.modules.cases.dto.CaseReviewInput;
import com.kaiyun.common.pagination.AdminListQuery;
import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.modules.cases.entity.ServiceRecordImage;
import com.kaiyun.modules.cases.vo.AdminServiceRecordListVO;
import com.kaiyun.modules.cases.vo.CaseAttachmentSummaryVO;
import com.kaiyun.modules.cases.vo.ClaimPushCaseVO;
import com.kaiyun.modules.cases.vo.PaidCaseVO;
import com.kaiyun.modules.cases.vo.ServiceRecordDetailVO;
import com.kaiyun.modules.cases.vo.ServiceRecordListVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ServiceRecordService {

    /**
     * 提交病例
     */
    Long save(
            ServiceRecordSaveDTO dto,
            MultipartFile[] files,
            String loginPhone
    ) throws Exception;

    /**
     * 查询服务记录
     */
    List<ServiceRecordListVO> serviceRecordlist(String phone);

    List<AdminServiceRecordListVO> adminList();

    AdminListPage<AdminServiceRecordListVO> adminPage(AdminListQuery query);

    AdminListPage<PaidCaseVO> paidPage(AdminListQuery query);


    List<CaseAttachmentSummaryVO> casesWithAttachments();

    List<ClaimPushCaseVO> listClaimPushCases();

    List<Long> existingCaseIds(List<Long> ids);

    List<ServiceRecordImage> listAttachments(Long id);

    /**
     * 查询案件详情
     * @param id 案件ID
     * @return 案件详情
     */
    ServiceRecordDetailVO getDetail(Long id, String phone);

    void updateProgress(Long id, CaseProgressUpdate input, String operator);
    void review(Long id, CaseReviewInput input, String operator);
}
