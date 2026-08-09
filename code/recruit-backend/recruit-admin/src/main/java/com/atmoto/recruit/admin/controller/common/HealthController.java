package com.atmoto.recruit.admin.controller.common;

import com.atmoto.recruit.common.core.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查 —— 验证应用已启动
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public AjaxResult health() {
        return AjaxResult.success("遨天科技-校园招聘管理系统 运行中", null);
    }
}
