package com.kaiyun.modules.ui.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaiyun.modules.ui.entity.Ui;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UiMapper extends BaseMapper<Ui> {
    @Select("SELECT * FROM ui WHERE id = #{id} FOR UPDATE")
    Ui selectForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM ui WHERE asset_key = 'home-health' FOR UPDATE")
    Ui selectDefaultForUpdate();
}
