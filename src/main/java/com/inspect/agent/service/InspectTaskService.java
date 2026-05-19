package com.inspect.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspect.agent.entity.ApiInspectTask;
import com.inspect.agent.mapper.ApiInspectTaskMapper;
import org.springframework.stereotype.Service;

/**
 * 巡检任务服务，提供任务分页查询功能。
 */
@Service
public class InspectTaskService extends ServiceImpl<ApiInspectTaskMapper, ApiInspectTask> {

    /**
     * 分页查询任务列表，支持按状态和时间范围筛选。
     *
     * @param status    任务状态，可选
     * @param startDate 开始日期（yyyy-MM-dd），可选
     * @param endDate   结束日期（yyyy-MM-dd），可选
     */
    public Page<ApiInspectTask> pageQuery(int page, int size, Integer status,
                                           String startDate, String endDate) {

        LambdaQueryWrapper<ApiInspectTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ApiInspectTask::getTaskStatus, status);
        }
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(ApiInspectTask::getCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(ApiInspectTask::getCreateTime, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(ApiInspectTask::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }
}