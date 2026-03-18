package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ynu.jackielinn.server.common.BaseController;

/**
 * AI聊天助手 Controller
 * 提供AI聊天助手操作接口
 */
@RestController
@RequestMapping("/api/assistant")
@Tag(name = "AI聊天助手", description = "AI聊天助手相关接口")
public class AssistantController extends BaseController {
}
