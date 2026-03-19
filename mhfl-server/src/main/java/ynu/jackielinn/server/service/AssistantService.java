package ynu.jackielinn.server.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ynu.jackielinn.server.dto.request.ChatRequestRO;
import ynu.jackielinn.server.dto.request.FeedbackRO;
import ynu.jackielinn.server.dto.request.SaveMessageRO;
import ynu.jackielinn.server.dto.request.UpdateConversationRO;
import ynu.jackielinn.server.dto.response.*;
import ynu.jackielinn.server.entity.Conversation;
import ynu.jackielinn.server.entity.Message;

import java.util.List;

public interface AssistantService {

    /**
     * 创建或复用空会话。若当前用户已有空会话（message_count=0），直接返回该会话 id，不新建。
     *
     * @param uid 当前用户 id
     * @return 会话 id
     */
    CreateConversationVO createConversation(Long uid);

    /**
     * 查询当前用户的会话列表。仅返回 message_count > 0 的会话，按 update_time 降序。
     *
     * @param uid 当前用户 id
     * @return 会话列表
     */
    List<ConversationVO> listByUserId(Long uid);

    /**
     * 获取会话详情。校验归属后返回，含消息列表。
     *
     * @param id  会话 id
     * @param uid 当前用户 id
     * @return 会话详情，无权限或不存在时返回 null
     */
    ConversationDetailVO getDetail(Long id, Long uid);

    /**
     * 逻辑删除会话。校验归属后执行。
     *
     * @param id  会话 id
     * @param uid 当前用户 id
     * @return null 表示成功，否则为错误信息
     */
    String deleteConversation(Long id, Long uid);

    /**
     * 更新会话标题。校验归属后执行。
     *
     * @param id    会话 id
     * @param uid   当前用户 id
     * @param title 新标题
     * @return null 表示成功，否则为错误信息
     */
    String updateTitle(Long id, Long uid, String title);

    /**
     * 非流式聊天。存 user 消息 -> 调 Python -> 存 assistant 消息 -> 更新 conversation。
     * cid 为空时自动创建或复用空会话；首条消息时用用户消息前 18 字作为标题。
     *
     * @param uid 当前用户 id
     * @param ro  聊天请求（cid、message）
     * @return 助手回复
     */
    ChatResponseVO chat(Long uid, ChatRequestRO ro);

    /**
     * 流式聊天。存 user 消息 -> WebClient 调 Python 流式接口 -> 转发 SSE -> 流结束后存 assistant 消息、更新 conversation。
     *
     * @param uid 当前用户 id
     * @param ro  聊天请求（cid、message）
     * @return SseEmitter，用于向前端推送 SSE
     */
    SseEmitter chatStream(Long uid, ChatRequestRO ro);

    /**
     * 更新消息反馈（点赞/点踩）。仅能对 assistant 消息操作，校验会话归属。
     *
     * @param messageId 消息 id
     * @param uid       当前用户 id
     * @param ro        反馈类型（0 取消 1 点赞 -1 点踩）
     * @return null 表示成功，否则为错误信息
     */
    String updateMessageFeedback(Long messageId, Long uid, FeedbackRO ro);

    /**
     * 获取或创建会话。cid 有效则校验并返回；cid 为 null 则复用空会话或新建。
     *
     * @param cid 会话 id，可为 null
     * @param uid 当前用户 id
     * @return 会话实体（内部使用）
     * @throws IllegalArgumentException 会话不存在或无权限时
     */
    Conversation getOrCreateConversation(Long cid, Long uid);

    /**
     * 获取会话内下一条消息的顺序号。
     *
     * @param cid 会话 id
     * @return 下一个 sequence_num（从 1 开始，无消息时返回 1）
     */
    Integer getNextSequenceNum(Long cid);

    /**
     * 保存消息。使用 RO 封装，不直接传入实体。
     *
     * @param ro 保存消息请求对象
     */
    void saveMessage(SaveMessageRO ro);

    /**
     * 更新会话。使用 RO 封装，仅更新非 null 字段。
     *
     * @param ro 更新会话请求对象
     */
    void updateConversation(UpdateConversationRO ro);

    /**
     * 根据 id 查询消息。供 updateMessageFeedback 等内部逻辑使用。
     *
     * @param id 消息 id
     * @return 消息实体，不存在时返回 null
     */
    Message getMessageById(Long id);

    /**
     * 根据 id 查询会话。供 updateMessageFeedback 等内部逻辑使用。
     *
     * @param id 会话 id
     * @return 会话实体，不存在时返回 null
     */
    Conversation getConversationById(Long id);
}
