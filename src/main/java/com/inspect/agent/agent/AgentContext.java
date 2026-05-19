package com.inspect.agent.agent;

import com.inspect.agent.dto.InspectResult;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 巡检任务运行上下文，承载任务执行期间的状态和统计信息。
 * <p>使用 {@link CopyOnWriteArrayList} 保证并发写入安全。
 */
@Data
public class AgentContext {

    /** 任务 ID（数据库主键） */
    private Long taskId;

    /** 任务编号 */
    private String taskNo;

    /** 执行模式（SERIAL / PARALLEL） */
    private String executeMode;

    /** 总用例数 */
    private int totalCount;

    /** 已完成数（含成功与失败） */
    private volatile int completedCount;

    /** 成功数 */
    private volatile int successCount;

    /** 失败数 */
    private volatile int failCount;

    /** 是否已收到停止指令 */
    private volatile boolean stopped;

    /** 任务开始时间 */
    private LocalDateTime startTime;

    /** 任务结束时间 */
    private LocalDateTime endTime;

    /** 执行结果列表，线程安全 */
    private final List<InspectResult> results = new CopyOnWriteArrayList<>();

    /** 添加一条执行结果，更新计数器 */
    public void addResult(InspectResult result) {
        results.add(result);
        completedCount++;
        if (result.isSuccess()) {
            successCount++;
        } else {
            failCount++;
        }
    }

    /** @return 是否全部用例已执行完毕 */
    public boolean isFinished() {
        return completedCount >= totalCount;
    }

    /** @return 任务已用耗时（毫秒），未开始时返回 0 */
    public long getDurationMs() {
        if (startTime == null) return 0;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return Duration.between(startTime, end).toMillis();
    }
}