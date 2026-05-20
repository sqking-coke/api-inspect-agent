package com.inspect.agent.scheduler;

import com.inspect.agent.agent.*;
import com.inspect.agent.config.*;
import com.inspect.agent.service.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;

/**
 * 定时巡检调度器，按 Cron 表达式自动触发巡检任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InspectScheduler {

    private final AgentCore agentCore;
    private final AgentConfig agentConfig;
    private final ConfigService configService;

    /**
     * 定时巡检入口方法，Cron 表达式从配置文件读取，默认每天凌晨 3:00。
     * <p>执行前检查 cron_enabled 数据库配置，为 false 时跳过。
     */
    @Scheduled(cron = "${agent.inspect.cron-expression:0 0 3 * * ?}")
    public void scheduledInspect() {
        String enabled = configService.getValue("cron_enabled");
        if (!"true".equals(enabled) && !agentConfig.isCronEnabled()) {
            return;
        }
        log.info("定时巡检触发");
        try {
            agentCore.startInspect("PARALLEL", null, 2);
        } catch (Exception e) {
            log.error("定时巡检执行失败", e);
        }
    }
}