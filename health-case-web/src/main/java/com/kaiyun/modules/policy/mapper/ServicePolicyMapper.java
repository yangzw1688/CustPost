package com.kaiyun.modules.policy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaiyun.modules.policy.entity.ServicePolicy;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ServicePolicyMapper extends BaseMapper<ServicePolicy> {
    @Select({"<script>", "SELECT policy_no FROM service_policy WHERE policy_no IN",
            "<foreach collection='numbers' item='number' open='(' separator=',' close=')'>#{number}</foreach>", "</script>"})
    List<String> selectExistingPolicyNumbers(@Param("numbers") List<String> numbers);
}
