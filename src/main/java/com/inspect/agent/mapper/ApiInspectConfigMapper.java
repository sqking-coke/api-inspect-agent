package com.inspect.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspect.agent.entity.ApiInspectConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统配置 Mapper。
 */
@Mapper
public interface ApiInspectConfigMapper extends BaseMapper<ApiInspectConfig> {
}