package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ynu.jackielinn.server.service.RecommendService;

@RestController
@RequestMapping("/api/recommended")
@Tag(name = "推荐页面操作接口", description = "推荐页面操作等相关接口")
public class RecommendController {

    @Resource
    private RecommendService recommendService;
}
