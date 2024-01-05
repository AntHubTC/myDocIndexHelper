package com.minibyte.controller;

import com.minibyte.common.MBResponse;
import com.minibyte.service.ConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@Controller
@RequestMapping("config")
public class ConfigController {

    @Resource
    private ConfigService configService;

    @GetMapping
    @ResponseBody
    public MBResponse<Map<String, String>> search() {
        Map<String, String> config = configService.listConfigs();
        return MBResponse.ofSuccess(config);
    }

    @PutMapping
    @ResponseBody
    public MBResponse<String> saveConfig(@RequestBody Map<String, String> config) {
        configService.saveConfig(config);
        return MBResponse.ofSuccess("ok");
    }
}
