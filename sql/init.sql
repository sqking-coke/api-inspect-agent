-- ============================================
-- 接口自动化巡检Agent - 完整数据库初始化脚本
-- 适用: MySQL 8.0+
-- ============================================

CREATE DATABASE IF NOT EXISTS `api_inspect_agent`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `api_inspect_agent`;

-- ============================================
-- 1. 接口巡检用例表
-- ============================================
DROP TABLE IF EXISTS `api_inspect_case`;
CREATE TABLE `api_inspect_case` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `case_name`      VARCHAR(128)  NOT NULL                 COMMENT '用例名称',
    `api_url`        VARCHAR(512)  NOT NULL                 COMMENT '接口完整地址',
    `method`         VARCHAR(16)   NOT NULL DEFAULT 'GET'   COMMENT 'GET/POST/PUT/DELETE',
    `request_header` TEXT          DEFAULT NULL             COMMENT '请求头JSON',
    `request_body`   TEXT          DEFAULT NULL             COMMENT '请求体JSON',
    `query_params`   TEXT          DEFAULT NULL             COMMENT 'URL查询参数JSON',
    `timeout`        INT           NOT NULL DEFAULT 5000    COMMENT '超时时间(ms)',
    `retry_count`    INT           NOT NULL DEFAULT 0       COMMENT '失败重试次数',
    `retry_interval` INT           NOT NULL DEFAULT 1000    COMMENT '重试间隔(ms)',
    `assert_rule`    TEXT          DEFAULT NULL             COMMENT '断言规则JSON',
    `group_name`     VARCHAR(64)   DEFAULT '默认分组'        COMMENT '接口分组',
    `description`    VARCHAR(512)  DEFAULT NULL             COMMENT '用例描述/备注',
    `priority`       TINYINT       NOT NULL DEFAULT 0       COMMENT '0-低 1-中 2-高 3-紧急',
    `status`         TINYINT       NOT NULL DEFAULT 1       COMMENT '0-禁用 1-启用',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_group_name` (`group_name`),
    INDEX `idx_status` (`status`),
    INDEX `idx_method` (`method`),
    INDEX `idx_group_status` (`group_name`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='接口巡检用例表';

-- ============================================
-- 2. 巡检任务记录表
-- ============================================
DROP TABLE IF EXISTS `api_inspect_task`;
CREATE TABLE `api_inspect_task` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `task_no`           VARCHAR(64)   NOT NULL                 COMMENT '任务编号 INSPECT-YYYYMMDD-NNN',
    `total_count`       INT           NOT NULL DEFAULT 0       COMMENT '总用例数',
    `success_count`     INT           NOT NULL DEFAULT 0       COMMENT '成功数',
    `fail_count`        INT           NOT NULL DEFAULT 0       COMMENT '失败数',
    `skip_count`        INT           NOT NULL DEFAULT 0       COMMENT '跳过数',
    `task_duration`     BIGINT        NOT NULL DEFAULT 0       COMMENT '任务总耗时(ms)',
    `execute_mode`      VARCHAR(16)   NOT NULL DEFAULT 'PARALLEL' COMMENT 'SERIAL/PARALLEL',
    `ai_summary`        LONGTEXT      DEFAULT NULL             COMMENT 'AI巡检总结报告(Markdown)',
    `ai_risk_items`     TEXT          DEFAULT NULL             COMMENT 'AI风险项汇总JSON',
    `error_rate`        DECIMAL(5,2)  DEFAULT 0.00            COMMENT '异常率(%)',
    `avg_response_time` INT           DEFAULT 0                COMMENT '平均响应时间(ms)',
    `task_status`       TINYINT       NOT NULL DEFAULT 0       COMMENT '0-执行中 1-已完成 2-已停止 3-异常中断',
    `trigger_type`      TINYINT       NOT NULL DEFAULT 1       COMMENT '1-手动 2-定时 3-API触发',
    `error_message`     VARCHAR(1024) DEFAULT NULL             COMMENT '任务级错误信息',
    `start_time`        DATETIME      DEFAULT NULL             COMMENT '任务开始时间',
    `end_time`          DATETIME      DEFAULT NULL             COMMENT '任务结束时间',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_task_no` (`task_no`),
    INDEX `idx_task_status` (`task_status`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_status_time` (`task_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='巡检任务记录表';

-- ============================================
-- 3. 接口巡检日志表
-- ============================================
DROP TABLE IF EXISTS `api_inspect_log`;
CREATE TABLE `api_inspect_log` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `task_id`         BIGINT        NOT NULL                 COMMENT '关联任务ID',
    `case_id`         BIGINT        NOT NULL                 COMMENT '关联用例ID',
    `case_name`       VARCHAR(128)  DEFAULT NULL             COMMENT '用例名称(冗余)',
    `api_url`         VARCHAR(512)  DEFAULT NULL             COMMENT '请求地址(冗余)',
    `method`          VARCHAR(16)   DEFAULT NULL             COMMENT '请求方式(冗余)',
    `request_header`  TEXT          DEFAULT NULL             COMMENT '实际请求头',
    `request_body`    TEXT          DEFAULT NULL             COMMENT '实际请求体',
    `response_header` TEXT          DEFAULT NULL             COMMENT '响应头',
    `response_body`   MEDIUMTEXT    DEFAULT NULL             COMMENT '响应体(最大16MB)',
    `status_code`     INT           DEFAULT NULL             COMMENT 'HTTP状态码',
    `response_time`   INT           DEFAULT 0                COMMENT '响应耗时(ms)',
    `success`         TINYINT       NOT NULL DEFAULT 0       COMMENT '0-失败 1-成功',
    `error_message`   VARCHAR(2048) DEFAULT NULL             COMMENT '错误信息',
    `error_type`      VARCHAR(64)   DEFAULT NULL             COMMENT '异常分类',
    `assert_detail`   TEXT          DEFAULT NULL             COMMENT '断言详细结果JSON',
    `retry_count`     INT           NOT NULL DEFAULT 0       COMMENT '实际重试次数',
    `ai_analysis`     TEXT          DEFAULT NULL             COMMENT 'AI对该条日志的分析',
    `ai_suggestion`   TEXT          DEFAULT NULL             COMMENT 'AI修复建议',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_case_id` (`case_id`),
    INDEX `idx_success` (`success`),
    INDEX `idx_error_type` (`error_type`),
    INDEX `idx_task_success` (`task_id`, `success`),
    INDEX `idx_task_error_type` (`task_id`, `error_type`),
    CONSTRAINT `fk_log_task` FOREIGN KEY (`task_id`) REFERENCES `api_inspect_task` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_log_case` FOREIGN KEY (`case_id`) REFERENCES `api_inspect_case` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='接口巡检日志表';

-- ============================================
-- 4. 系统配置表
-- ============================================
DROP TABLE IF EXISTS `api_inspect_config`;
CREATE TABLE `api_inspect_config` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `config_key`    VARCHAR(64)  NOT NULL                 COMMENT '配置键',
    `config_value`  TEXT         DEFAULT NULL             COMMENT '配置值',
    `config_type`   VARCHAR(32)  NOT NULL DEFAULT 'STRING' COMMENT 'STRING/INT/BOOL/JSON',
    `description`   VARCHAR(256) DEFAULT NULL             COMMENT '配置说明',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统配置表';

-- ============================================
-- 5. 初始化数据
-- ============================================
INSERT INTO `api_inspect_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('cron_expression',         '0 0 3 * * ?',                                                    'STRING', '定时巡检Cron表达式（每日凌晨3点）'),
('cron_enabled',            'false',                                                           'BOOL',   '是否启用定时巡检'),
('default_timeout',         '5000',                                                            'INT',    '接口默认超时时间(ms)'),
('default_retry_count',     '2',                                                               'INT',    '接口默认重试次数'),
('retry_interval',          '1000',                                                            'INT',    '重试间隔(ms)'),
('parallel_threads',        '10',                                                              'INT',    '并行巡检最大线程数'),
('llm_enabled',             'true',                                                            'BOOL',   '是否启用AI智能分析'),
('llm_provider',            'openai_compatible',                                               'STRING', '大模型提供商'),
('llm_api_key',             '',                                                                'STRING', '大模型API密钥'),
('llm_api_url',             'https://api.openai.com/v1/chat/completions',                      'STRING', '大模型API地址'),
('llm_model',               'gpt-4o',                                                          'STRING', '大模型名称'),
('llm_max_tokens',          '4096',                                                            'INT',    '大模型最大输出Token'),
('llm_temperature',         '0.3',                                                             'STRING', '大模型温度参数'),
('alert_enabled',           'false',                                                           'BOOL',   '是否启用异常告警'),
('alert_webhook',           '',                                                                'STRING', '告警Webhook地址'),
('alert_threshold',         '3',                                                               'INT',    '连续失败N次触发告警'),
('log_retention_days',      '90',                                                              'INT',    '巡检日志保留天数'),
('response_body_max_length', '8192',                                                           'INT',    '响应体最大存储长度(字符)');

INSERT INTO `api_inspect_case` (`case_name`, `api_url`, `method`, `request_header`, `request_body`, `timeout`, `retry_count`, `assert_rule`, `group_name`, `priority`, `status`) VALUES
('用户登录接口',    'http://localhost:8080/api/user/login',    'POST', '{"Content-Type":"application/json"}',                        '{"username":"admin","password":"123456"}',          5000, 2, '{"statusCode":200,"bodyContains":"token","responseCodePath":"$.code","responseCode":0}',              '用户模块', 3, 1),
('用户信息查询',    'http://localhost:8080/api/user/info',     'GET',  '{"Authorization":"Bearer ${token}"}',                       NULL,                                               3000, 1, '{"statusCode":200,"notNullPath":"$.data.id"}',                                                    '用户模块', 2, 1),
('订单列表查询',    'http://localhost:8080/api/order/list',    'GET',  '{"Authorization":"Bearer ${token}"}',                       NULL,                                               3000, 1, '{"statusCode":200,"responseCodePath":"$.code","responseCode":0}',                                  '订单模块', 2, 1),
('订单创建接口',    'http://localhost:8080/api/order/create',  'POST', '{"Content-Type":"application/json","Authorization":"Bearer ${token}"}', '{"productId":1,"quantity":1}',           5000, 2, '{"statusCode":200,"responseCode":0}',                                                                 '订单模块', 3, 1),
('系统健康检查',    'http://localhost:8080/api/system/health', 'GET',  NULL,                                                          NULL,                                               2000, 0, '{"statusCode":200,"maxResponseTime":1000}',                                                          '系统模块', 1, 1);
