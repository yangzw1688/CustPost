package com.kaiyun.modules.ui.service;

import com.kaiyun.infrastructure.storage.ObsStorageService;
import com.kaiyun.modules.cases.service.ServiceRecordValidator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.ui.dto.UiSaveDTO;
import com.kaiyun.modules.ui.entity.Ui;
import com.kaiyun.modules.ui.mapper.UiMapper;
import com.kaiyun.modules.ui.vo.UiVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** 界面内容管理；替换图片时同时协调数据库更新和 OBS 对象清理。 */
@Service
public class UiService {
    private static final Logger log = LoggerFactory.getLogger(UiService.class);
    private final UiMapper mapper;
    private final ObsStorageService storage;
    private final UiPublishedPages publishedPages;
    public UiService(UiMapper mapper, ObsStorageService storage, UiPublishedPages publishedPages) {
        this.mapper = mapper;
        this.storage = storage;
        this.publishedPages = publishedPages;
    }

    @Transactional(readOnly = true)
    public List<UiVO> list(String slot, boolean management) {
        UiValidator.slot(slot);
        return mapper.selectList(new LambdaQueryWrapper<Ui>().eq(Ui::getSlot, slot)
                .eq(!management, Ui::getEnabled, true).orderByAsc(Ui::getSortOrder, Ui::getId)
                .last("LIMIT 100")).stream().map(this::view).collect(Collectors.toList());
    }

    public List<UiVO> pageImages(Set<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyList();
        return mapper.selectList(new LambdaQueryWrapper<Ui>().in(Ui::getId, ids).eq(Ui::getEnabled, true)
                .isNotNull(Ui::getObjectKey).ne(Ui::getObjectKey, "")).stream().map(this::view).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long save(UiSaveDTO input, MultipartFile file) throws IOException {
        UiValidator.validate(input);
        String extension = file == null ? null : ServiceRecordValidator.validateImages(new MultipartFile[] {file}).get(0);
        if (file != null) storage.validateConfiguration();
        if (input.getId() != null && !input.getEnabled()) publishedPages.requireUnreferencedImage(input.getId());
        Ui record = input.getId() == null ? new Ui() : require(input.getId());
        if (input.getId() != null && !record.getAssetKey().equals(input.getAssetKey())) {
            throw new BusinessException(400, "已有配置的标识不可修改");
        }
        String oldKey = record.getObjectKey();
        BeanUtils.copyProperties(input, record);
        record.setUpdateTime(LocalDateTime.now());
        if (file != null) {
            String key = "ui/" + UUID.randomUUID().toString().replace("-", "") + extension;
            compensate(key, oldKey);
            storage.upload(key, file);
            record.setObjectKey(key);
        }
        if (input.getId() == null) {
            record.setCreateTime(record.getUpdateTime());
            if (mapper.insert(record) != 1 || record.getId() == null) throw new IllegalStateException("界面配置保存失败");
        } else if (mapper.updateById(record) != 1) throw new IllegalStateException("界面配置更新失败");
        return record.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        publishedPages.requireUnreferencedImage(id);
        Ui record = require(id);
        if (hasKey(record.getObjectKey())) compensate(null, record.getObjectKey());
        if (mapper.deleteById(id) != 1) throw new IllegalStateException("界面配置删除失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean importDefaultImage() throws IOException {
        Ui record = mapper.selectDefaultForUpdate();
        if (record == null) throw new BusinessException(409, "请先初始化 ui 表的默认配置");
        if (hasKey(record.getObjectKey())) return false;
        storage.validateConfiguration();
        MultipartFile file = new DefaultImage();
        String extension = ServiceRecordValidator.validateImages(new MultipartFile[] {file}).get(0);
        String key = "ui/" + UUID.randomUUID().toString().replace("-", "") + extension;
        compensate(key, null);
        storage.upload(key, file);
        record.setObjectKey(key);
        record.setUpdateTime(LocalDateTime.now());
        if (mapper.updateById(record) != 1) throw new IllegalStateException("默认图片保存失败");
        return true;
    }

    private Ui require(Long id) {
        if (id == null || id <= 0) throw new BusinessException(400, "图片编号不正确");
        Ui record = mapper.selectForUpdate(id);
        if (record == null) throw new BusinessException(404, "界面配置不存在");
        return record;
    }
    private UiVO view(Ui record) {
        UiVO result = new UiVO();
        BeanUtils.copyProperties(record, result);
        String imageUrl = hasKey(record.getObjectKey()) ? storage.signedUrl(record.getObjectKey()) : "";
        if (imageUrl.isEmpty() && "home_banner".equals(record.getSlot()) && "home-health".equals(record.getAssetKey())) {
            imageUrl = "/images/caringhealth.png";
        }
        result.setImageUrl(imageUrl);
        return result;
    }
    private boolean hasKey(String key) { return key != null && !key.isEmpty(); }
    private void compensate(String newKey, String oldKey) {
        // 提交成功才删除旧图，回滚则删除新图；未知状态保留两者供核对。
        if (!TransactionSynchronizationManager.isSynchronizationActive()) throw new IllegalStateException("界面图片写入必须在事务中执行");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                String key = status == STATUS_COMMITTED ? oldKey : status == STATUS_ROLLED_BACK ? newKey : null;
                if (status == STATUS_UNKNOWN) log.error("界面配置提交结果未知，请核对 OBS 对象，newKey={}, oldKey={}", newKey, oldKey);
                if (hasKey(key)) {
                    try { storage.delete(key); }
                    catch (Exception e) { log.error("界面图片清理失败，objectKey={}", key, e); }
                }
            }
        });
    }
    /** 将内置首页图片适配为上传文件，供管理端首次导入 OBS 使用。 */
    private static class DefaultImage implements MultipartFile {
        private final ClassPathResource resource = new ClassPathResource("ui-seed/caringhealth.png");
        public String getName() { return "file"; }
        public String getOriginalFilename() { return "caringhealth.png"; }
        public String getContentType() { return "image/png"; }
        public boolean isEmpty() { return false; }
        public long getSize() { try { return resource.contentLength(); } catch (IOException e) { throw new IllegalStateException("默认图片资源缺失", e); } }
        public byte[] getBytes() throws IOException { try (InputStream in = getInputStream()) { return org.springframework.util.StreamUtils.copyToByteArray(in); } }
        public InputStream getInputStream() throws IOException { return resource.getInputStream(); }
        public void transferTo(File dest) throws IOException { try (InputStream in = getInputStream()) { Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING); } }
    }
}
