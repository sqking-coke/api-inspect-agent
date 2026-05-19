package com.inspect.agent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspect.agent.common.Result;
import com.inspect.agent.dto.BatchStatusRequest;
import com.inspect.agent.entity.ApiInspectCase;
import com.inspect.agent.service.CaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 巡检用例管理接口。
 */
@RestController
@RequestMapping("/agent/inspect/case")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    /** 新增或更新用例（有 id 则更新，无 id 则新增） */
    @PostMapping("/save")
    public Result<Map<String, Long>> save(@RequestBody ApiInspectCase entity) {
        caseService.saveOrUpdate(entity);
        return Result.ok("保存成功", Map.of("id", entity.getId()));
    }

    /** 删除用例 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        caseService.removeById(id);
        return Result.ok("删除成功", null);
    }

    /** 分页查询用例列表 */
    @GetMapping("/list")
    public Result<Page<ApiInspectCase>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(caseService.pageQuery(page, size, groupName, status, keyword));
    }

    /** 获取单个用例详情 */
    @GetMapping("/{id}")
    public Result<ApiInspectCase> detail(@PathVariable Long id) {
        return Result.ok(caseService.getById(id));
    }

    /** 批量更新用例状态 */
    @PutMapping("/batch-status")
    public Result<Void> batchStatus(@RequestBody BatchStatusRequest request) {
        caseService.batchUpdateStatus(request.getIds(), request.getStatus());
        return Result.ok("操作成功", null);
    }
}