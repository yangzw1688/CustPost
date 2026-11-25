package com.kaiyun.modules.cases.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("service_record_image")
public class ServiceRecordImage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 服务记录ID
     */
    private Long serviceRecordId;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 服务器文件名
     */
    private String fileName;

    /**
     * OBS 对象键或历史本地路径，不对前端公开。
     */
    @JsonIgnore
    private String filePath;

    /**
     * 数据库保存 obs:// 标识，查询时转换为临时签名链接；兼容历史本地 URL。
     */
    private String fileUrl;

    /**
     * 排序
     */
    private Integer sort;

    private LocalDateTime createTime;

    /** 从文件名推导类型，图片与 PDF 共用附件表，无需增加数据库列。 */
    public String getFileType() {
        return fileName != null && fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf") ? "pdf" : "image";
    }
}
