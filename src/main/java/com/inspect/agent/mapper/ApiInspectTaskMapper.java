package com.inspect.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspect.agent.entity.ApiInspectTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 巡检任务 Mapper。
 */
@Mapper
public interface ApiInspectTaskMapper extends BaseMapper<ApiInspectTask> {
}