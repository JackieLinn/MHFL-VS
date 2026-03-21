package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ynu.jackielinn.server.common.BaseController;
import ynu.jackielinn.server.common.RestResponse;
import ynu.jackielinn.server.dto.request.ChatRequestRO;
import ynu.jackielinn.server.dto.request.FeedbackRO;
import ynu.jackielinn.server.dto.request.UpdateTitleRO;
import ynu.jackielinn.server.dto.response.*;
import ynu.jackielinn.server.service.AssistantService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 智能助手 Controller。提供会话 CRUD 及非流式聊天代理接口。
 * 所有接口需 JWT 认证，仅操作当前用户自己的会话。
 */
@RestController
@RequestMapping("/api/assistant")
@Tag(name = "智能助手", description = "会话管理及 AI 聊天相关接口")
public class AssistantController extends BaseController {

    @Resource
    private AssistantService assistantService;

    /**
     * 创建或复用空会话。若当前用户已有空会话（message_count=0），直接返回该会话 id，不新建。
     *
     * @param request 用于获取当前用户 id
     * @return 会话 id
     */
    @Operation(summary = "创建会话接口", description = "创建新会话或复用空会话；空会话指 message_count=0 的会话")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期")
    })
    @PostMapping("/conversation")
    public RestResponse<CreateConversationVO> createConversation(HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("id");
        if (uid == null) {
            return RestResponse.failure(401, "未登录或登录已过期");
        }
        CreateConversationVO vo = assistantService.createConversation(uid);
        return RestResponse.success(vo);
    }

    /**
     * 会话列表。仅返回 message_count > 0 的会话，按 update_time 降序。
     *
     * @param request 用于获取当前用户 id
     * @return 会话列表
     */
    @Operation(summary = "会话列表接口", description = "查询当前用户的会话列表，仅包含有消息的会话")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期")
    })
    @GetMapping("/conversation/list")
    public RestResponse<List<ConversationVO>> listConversations(
            @Parameter(description = "按标题模糊搜索，为空时返回全部") @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("id");
        if (uid == null) {
            return RestResponse.failure(401, "未登录或登录已过期");
        }
        List<ConversationVO> list = assistantService.listByUserId(uid, keyword);
        return RestResponse.success(list);
    }

    /**
     * 会话详情。含消息列表，按 sequence_num 升序。
     *
     * @param id      会话 id
     * @param request 用于获取当前用户 id
     * @return 会话详情，无权限或不存在时 404
     */
    @Operation(summary = "会话详情接口", description = "根据会话 id 查询详情，含消息列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "404", description = "会话不存在或无权限查看")
    })
    @GetMapping("/conversation/{id}")
    public RestResponse<ConversationDetailVO> getConversation(
            @Parameter(description = "会话 id") @PathVariable Long id,
            HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("id");
        if (uid == null) {
            return RestResponse.failure(401, "未登录或登录已过期");
        }
        ConversationDetailVO vo = assistantService.getDetail(id, uid);
        if (vo == null) {
            return RestResponse.failure(404, "会话不存在或无权限查看");
        }
        return RestResponse.success(vo);
    }

    /**
     * 逻辑删除会话。
     *
     * @param id      会话 id
     * @param request 用于获取当前用户 id
     * @return 操作结果
     */
    @Operation(summary = "删除会话接口", description = "逻辑删除会话，仅本人可删")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "会话不存在或无权限"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期")
    })
    @DeleteMapping("/conversation/{id}")
    public RestResponse<Void> deleteConversation(
            @Parameter(description = "会话 id") @PathVariable Long id,
            HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("id");
        if (uid == null) {
            return RestResponse.failure(401, "未登录或登录已过期");
        }
        return messageHandle(() -> assistantService.deleteConversation(id, uid));
    }

    /**
     * 更新会话标题。
     *
     * @param id      会话 id
     * @param ro      新标题
     * @param request 用于获取当前用户 id
     * @return 操作结果
     */
    @Operation(summary = "更新会话标题接口", description = "修改会话标题，仅本人可操作")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "会话不存在或无权限"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期")
    })
    @PutMapping("/conversation/{id}/title")
    public RestResponse<Void> updateTitle(
            @Parameter(description = "会话 id") @PathVariable Long id,
            @RequestBody @Valid UpdateTitleRO ro,
            HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("id");
        if (uid == null) {
            return RestResponse.failure(401, "未登录或登录已过期");
        }
        return messageHandle(() -> assistantService.updateTitle(id, uid, ro.getTitle()));
    }

    /**
     * 非流式聊天。cid 为空时自动创建或复用空会话；首条消息时用用户消息前 18 字作为标题。
     *
     * @param ro      聊天请求（cid、message）
     * @param request 用于获取当前用户 id
     * @return 助手回复（content、sources）
     */
    @Operation(summary = "聊天接口", description = "非流式聊天，代理到 Python；cid 为空时复用空会话或新建")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "参数错误或会话无权限"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "500", description = "智能助手服务异常")
    })
    @PostMapping("/chat")
    public RestResponse<ChatResponseVO> chat(
            @RequestBody @Valid ChatRequestRO ro,
            HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("id");
        if (uid == null) {
            return RestResponse.failure(401, "未登录或登录已过期");
        }
        try {
            ChatResponseVO vo = assistantService.chat(uid, ro);
            return RestResponse.success(vo);
        } catch (IllegalArgumentException e) {
            return RestResponse.failure(400, e.getMessage());
        } catch (RuntimeException e) {
            return RestResponse.failure(500, e.getMessage());
        }
    }

    /**
     * 流式聊天。代理到 Python /api/assistant/chat/stream，逐 chunk 推送 SSE。
     *
     * @param ro      聊天请求（cid、message）
     * @param request 用于获取当前用户 id
     * @return SseEmitter，推送 data: {type, content}
     */
    @Operation(summary = "流式聊天接口", description = "SSE 流式输出，data 格式：{type:delta|done|error, content}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 流"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期")
    })
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestBody @Valid ChatRequestRO ro,
            HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("id");
        if (uid == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
        return assistantService.chatStream(uid, ro);
    }

    /**
     * 更新消息反馈（点赞/点踩）。仅能对 assistant 消息操作。
     *
     * @param id      消息 id
     * @param ro      反馈类型（0 取消 1 点赞 -1 点踩）
     * @param request 用于获取当前用户 id
     * @return 操作结果
     */
    @Operation(summary = "消息反馈接口", description = "对助手消息进行点赞或点踩；0 取消反馈 1 点赞 -1 点踩")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "消息不存在、无权限或只能对助手消息反馈"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期")
    })
    @PutMapping("/message/{id}/feedback")
    public RestResponse<Void> updateMessageFeedback(
            @Parameter(description = "消息 id") @PathVariable Long id,
            @RequestBody @Valid FeedbackRO ro,
            HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("id");
        if (uid == null) {
            return RestResponse.failure(401, "未登录或登录已过期");
        }
        return messageHandle(() -> assistantService.updateMessageFeedback(id, uid, ro));
    }
}
