package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ynu.jackielinn.server.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "仪表盘 Dashboard 接口", description = "仪表盘 Dashboard 操作相关接口")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;
}
