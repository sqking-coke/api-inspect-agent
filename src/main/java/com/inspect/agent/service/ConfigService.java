package com.inspect.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspect.agent.entity.ApiInspectConfig;
import com.inspect.agent.mapper.ApiInspectConfigMapper;
import org.springframework.stereotype.Service;

/**
 * 系统配置服务，以 key-value 形式读写运行时配置项。
 * <p>优先读取数据库配置，数据库无记录时由调用方使用默认值。
 */
@Service
public class ConfigService extends ServiceImpl<ApiInspectConfigMapper, ApiInspectConfig> {

    /**
     * 根据 key 获取配置值。
     *
     * @return 配置值，不存在时返回 null
     */
    public String getValue(String key) {
        ApiInspectConfig config = getOne(new LambdaQueryWrapper<ApiInspectConfig>()
                .eq(ApiInspectConfig::getConfigKey, key));
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 设置配置值，key 已存在则更新，否则新增。
     *
     * @param key   配置键
     * @param value 配置值
     */
    public void setValue(String key, String value) {
        ApiInspectConfig config = getOne(new LambdaQueryWrapper<ApiInspectConfig>()
                .eq(ApiInspectConfig::getConfigKey, key));
        if (config != null) {
            config.setConfigValue(value);
            updateById(config);
        } else {
            config = new ApiInspectConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType("STRING");
            save(config);
        }
    }
}