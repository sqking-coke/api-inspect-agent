package com.inspect.agent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspect.agent.entity.ApiInspectCase;
import com.inspect.agent.mapper.ApiInspectCaseMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 巡检用例服务，提供用例的增删改查和批量状态管理。
 */
@Service
public class CaseService extends ServiceImpl<ApiInspectCaseMapper, ApiInspectCase> {

    /**
     * 分页查询用例，支持按分组、状态、关键词筛选。
     *
     * @param groupName 分组名称，可选
     * @param status    状态筛选，可选
     * @param keyword   关键词（匹配用例名或 URL），可选
     */
    public Page<ApiInspectCase> pageQuery(int page, int size, String groupName, Integer status, String keyword) {
        var wrapper = lambdaQuery();
        if (groupName != null && !groupName.isBlank()) {
            wrapper.eq(ApiInspectCase::getGroupName, groupName);
        }
        if (status != null) {
            wrapper.eq(ApiInspectCase::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(ApiInspectCase::getCaseName, keyword)
                    .or().like(ApiInspectCase::getApiUrl, keyword));
        }
        wrapper.orderByDesc(ApiInspectCase::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    /**
     * 批量更新用例状态。
     *
     * @param ids    目标用例 ID 列表
     * @param status 目标状态（0=禁用，1=启用）
     */
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        lambdaUpdate().in(ApiInspectCase::getId, ids)
                .set(ApiInspectCase::getStatus, status)
                .update();
    }
}