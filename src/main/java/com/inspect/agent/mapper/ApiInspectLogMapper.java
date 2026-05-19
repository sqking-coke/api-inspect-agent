package com.inspect.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspect.agent.entity.ApiInspectLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 巡检日志 Mapper。
 */
@Mapper
public interface ApiInspectLogMapper extends BaseMapper<ApiInspectLog> {
}