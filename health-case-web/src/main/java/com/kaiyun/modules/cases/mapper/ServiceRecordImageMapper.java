package com.kaiyun.modules.cases.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaiyun.modules.cases.entity.ServiceRecordImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ServiceRecordImageMapper
        extends BaseMapper<ServiceRecordImage> {
    @Select("SELECT i.file_url FROM service_record_image i JOIN service_record r ON r.id = i.service_record_id "
            + "WHERE r.phone = #{phone} AND i.file_url = #{url}")
    List<String> findOwnedUrls(@Param("phone") String phone, @Param("url") String url);
}
