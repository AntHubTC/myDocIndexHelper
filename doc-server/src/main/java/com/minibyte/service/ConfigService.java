package com.minibyte.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minibyte.bo.dto.system.KvInfo;
import com.minibyte.mapper.system.KvInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件索引检索服务
 *
 * @author:
 * @date:
 * @description:
 */
@Service
@Slf4j
public class ConfigService {
    @Resource
    private KvInfoMapper kvInfoMapper;

    public String getConfigValue(String key) {
        Map<String, String> config = this.listConfigs();
        return config.get(key);
    }

    public Map<String, String> listConfigs() {
        List<KvInfo> kvInfos = kvInfoMapper.selectList(new QueryWrapper<>());
        return Optional.ofNullable(kvInfos).orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(KvInfo::getKey, KvInfo::getValue, (a,b) -> a));
    }

    public void saveConfig(Map<String, String> config) {
        for (String key : config.keySet()) {
            KvInfo kvInfo = kvInfoMapper.selectById(key);
            if (Objects.isNull(kvInfo)) {
                kvInfo = new KvInfo();
                kvInfo.setKey(key);
                kvInfo.setValue(config.get(key));
                kvInfoMapper.insert(kvInfo);
            } else {
                kvInfoMapper.updateById(kvInfo);
            }
        }
    }
}
