package ynu.jackielinn.server.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import ynu.jackielinn.server.common.Feedback;
import ynu.jackielinn.server.dto.request.ChatRequestRO;
import ynu.jackielinn.server.dto.request.FeedbackRO;
import ynu.jackielinn.server.dto.request.ListAlgorithmRO;
import ynu.jackielinn.server.dto.request.ListDatasetRO;
import ynu.jackielinn.server.dto.request.SaveMessageRO;
import ynu.jackielinn.server.dto.request.UpdateConversationRO;
import ynu.jackielinn.server.dto.response.*;
import ynu.jackielinn.server.entity.Conversation;
import ynu.jackielinn.server.entity.Message;
import ynu.jackielinn.server.mapper.ConversationMapper;
import ynu.jackielinn.server.mapper.MessageMapper;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.AssistantService;
import ynu.jackielinn.server.service.DashboardService;
import ynu.jackielinn.server.service.DatasetService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class AssistantServiceImpl implements AssistantService {

    private static final String DEFAULT_TITLE = "新建对话";

    private static final int TITLE_MAX_LEN = 18;

    @Value("${python.fastapi.url:http://localhost:8000}")
    private String pythonFastApiUrl;

    @Value("${assistant.stream.emitter-timeout-ms:900000}")
    private Long assistantStreamEmitterTimeoutMs;

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private RestTemplate restTemplate;

    @Resource(name = "assistantWebClient")
    private WebClient assistantWebClient;

    @Resource(name = "assistantChatStreamExecutor")
    private Executor assistantChatStreamExecutor;

    @Resource
    private DashboardService dashboardService;

    @Resource
    private AlgorithmService algorithmService;

    @Resource
    private DatasetService datasetService;

    /**
     * 创建或复用空会话。先查当前用户是否有 message_count=0 的会话，有则返回其 id，否则新建。
     *
     * @param uid 当前用户 id
     * @return 会话 id
     */
    @Override
    public CreateConversationVO createConversation(Long uid) {
        Conversation empty = findEmptyConversation(uid);
        if (empty != null) {
            return CreateConversationVO.builder().id(empty.getId()).build();
        }
        Conversation conv = Conversation.builder()
                .uid(uid)
                .title(DEFAULT_TITLE)
                .summary(null)
                .messageCount(0)
                .build();
        conversationMapper.insert(conv);
        return CreateConversationVO.builder().id(conv.getId()).build();
    }

    /**
     * 查询会话列表。仅返回 message_count > 0 的会话，按 update_time 降序，每条带最后消息预览。
     *
     * @param uid 当前用户 id
     * @return 会话列表
     */
    @Override
    public List<ConversationVO> listByUserId(Long uid, String keyword) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUid, uid)
                .gt(Conversation::getMessageCount, 0)
                .orderByDesc(Conversation::getUpdateTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Conversation::getTitle, keyword.trim());
        }
        List<Conversation> list = conversationMapper.selectList(wrapper);
        List<ConversationVO> result = new ArrayList<>(list.size());
        for (Conversation c : list) {
            String preview = getLastMessagePreview(c.getId());
            result.add(ConversationVO.builder()
                    .id(c.getId())
                    .title(c.getTitle())
                    .preview(preview)
                    .updateTime(c.getUpdateTime())
                    .messageCount(c.getMessageCount())
                    .build());
        }
        return result;
    }

    /**
     * 获取会话详情。校验 uid 归属后返回，含按 sequence_num 升序的消息列表。
     *
     * @param id  会话 id
     * @param uid 当前用户 id
     * @return 会话详情，无权限或不存在时返回 null
     */
    @Override
    public ConversationDetailVO getDetail(Long id, Long uid) {
        Conversation conv = conversationMapper.selectById(id);
        if (conv == null || !conv.getUid().equals(uid)) {
            return null;
        }
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getCid, id)
                        .orderByAsc(Message::getSequenceNum));
        List<MessageVO> messageVOs = new ArrayList<>(messages.size());
        for (Message m : messages) {
            messageVOs.add(m.asViewObject(MessageVO.class, vo -> {
                vo.setSources(parseSourcesJson(m.getSourcesJson()));
                vo.setFeedback(m.getFeedback() != null ? m.getFeedback() : Feedback.NONE);
            }));
        }
        return ConversationDetailVO.builder()
                .id(conv.getId())
                .title(conv.getTitle())
                .messageCount(conv.getMessageCount())
                .createTime(conv.getCreateTime())
                .updateTime(conv.getUpdateTime())
                .messages(messageVOs)
                .build();
    }

    /**
     * 逻辑删除会话。校验归属后显式设置 is_deleted、delete_time。
     *
     * @param id  会话 id
     * @param uid 当前用户 id
     * @return null 表示成功，否则为错误信息
     */
    @Override
    public String deleteConversation(Long id, Long uid) {
        Conversation conv = conversationMapper.selectById(id);
        if (conv == null) {
            return "会话不存在";
        }
        if (!conv.getUid().equals(uid)) {
            return "无权限删除该会话";
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Conversation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Conversation::getId, id)
                .set(Conversation::getDeleted, 1)
                .set(Conversation::getDeleteTime, now);
        if (conversationMapper.update(null, wrapper) > 0) {
            return null;
        }
        return "删除失败，请联系管理员";
    }

    /**
     * 更新会话标题。校验归属后仅更新 title 字段。
     *
     * @param id    会话 id
     * @param uid   当前用户 id
     * @param title 新标题
     * @return null 表示成功，否则为错误信息
     */
    @Override
    public String updateTitle(Long id, Long uid, String title) {
        Conversation conv = conversationMapper.selectById(id);
        if (conv == null) {
            return "会话不存在";
        }
        if (!conv.getUid().equals(uid)) {
            return "无权限修改该会话";
        }
        Conversation update = new Conversation();
        update.setId(id);
        update.setTitle(title);
        if (conversationMapper.updateById(update) > 0) {
            return null;
        }
        return "更新失败，请联系管理员";
    }

    /**
     * 非流式聊天。getOrCreateConversation -> 存 user 消息 -> 调 Python -> 存 assistant 消息 -> 更新 conversation。
     * 首条消息时用用户消息前 18 字作为标题。
     *
     * @param uid 当前用户 id
     * @param ro  聊天请求（cid、message）
     * @return 助手回复
     */
    @Override
    public ChatResponseVO chat(Long uid, ChatRequestRO ro) {
        Conversation conv = getOrCreateConversation(ro.getCid(), uid);
        Long cid = conv.getId();
        Integer seq = getNextSequenceNum(cid);

        // 1. 存 user 消息
        saveMessage(SaveMessageRO.builder()
                .cid(cid)
                .role("user")
                .content(ro.getMessage())
                .sequenceNum(seq)
                .feedback(Feedback.NONE)
                .build());

        int currentCount = (conv.getMessageCount() == null ? 0 : conv.getMessageCount()) + 1;
        String memoryContext = buildMemoryContextAndMaybeUpdateSummary(cid, conv, currentCount);

        // 2. 意图分类 -> 预取业务数据 -> 调 Python（步骤 14/15）
        ClassifyResult classifyResult = callPythonClassify(ro.getMessage());
        Map<String, Object> contextData = buildContextData(uid, classifyResult);
        ChatResponseVO resp = callPythonChat(ro.getMessage(), contextData, classifyResult.needsKb(), memoryContext);

        // 3. 存 assistant 消息
        String sourcesJson = resp.getSources() != null ? JSON.toJSONString(resp.getSources()) : "[]";
        saveMessage(SaveMessageRO.builder()
                .cid(cid)
                .role("assistant")
                .content(resp.getContent())
                .sequenceNum((seq != null ? seq : 1) + 1)
                .sourcesJson(sourcesJson)
                .feedback(Feedback.NONE)
                .build());

        // 4. 更新 conversation
        int newCount = (conv.getMessageCount() == null ? 0 : conv.getMessageCount()) + 2;
        String newTitle = conv.getTitle();
        if (DEFAULT_TITLE.equals(conv.getTitle())) {
            String msg = ro.getMessage();
            newTitle = msg.length() > TITLE_MAX_LEN ? msg.substring(0, TITLE_MAX_LEN) + "..." : msg;
        }
        updateConversation(UpdateConversationRO.builder()
                .id(cid)
                .messageCount(newCount)
                .title(newTitle)
                .build());

        return resp;
    }

    /**
     * 流式聊天。存 user 消息 -> WebClient 调 Python 流式接口 -> 解析 SSE 转发 -> 流结束后存 assistant 消息、更新 conversation。
     *
     * @param uid 当前用户 id
     * @param ro  聊天请求（cid、message）
     * @return SseEmitter，用于向前端推送 SSE
     */
    @Override
    public SseEmitter chatStream(Long uid, ChatRequestRO ro) {
        SseEmitter emitter = new SseEmitter(assistantStreamEmitterTimeoutMs);
        SecurityContext ctx = SecurityContextHolder.getContext();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Disposable> disposableRef = new AtomicReference<>();

        emitter.onCompletion(() -> {
            Disposable d = disposableRef.getAndSet(null);
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        });
        emitter.onTimeout(() -> {
            Disposable d = disposableRef.getAndSet(null);
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
            if (!completed.getAndSet(true)) {
                sendTerminalEvent(emitter, "error", "Assistant stream timeout");
            }
            emitter.complete();
        });
        emitter.onError(ex -> {
            Disposable d = disposableRef.getAndSet(null);
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        });

        assistantChatStreamExecutor.execute(() -> {
            try {
                SecurityContextHolder.setContext(ctx);
                Conversation conv = getOrCreateConversation(ro.getCid(), uid);
                Long cid = conv.getId();
                Integer seq = getNextSequenceNum(cid);

                saveMessage(SaveMessageRO.builder()
                        .cid(cid)
                        .role("user")
                        .content(ro.getMessage())
                        .sequenceNum(seq)
                        .feedback(Feedback.NONE)
                        .build());

                final Long cidFinal = cid;
                final Conversation convFinal = conv;
                final int seqFinal = seq != null ? seq : 1;

                int currentCount = (conv.getMessageCount() == null ? 0 : conv.getMessageCount()) + 1;
                String memoryContext = buildMemoryContextAndMaybeUpdateSummary(cid, conv, currentCount);

                ClassifyResult classifyResult = callPythonClassify(ro.getMessage());
                Map<String, Object> contextData = buildContextData(uid, classifyResult);

                Map<String, Object> body = new HashMap<>();
                body.put("message", ro.getMessage());
                body.put("context_data", contextData);
                body.put("needs_kb", classifyResult.needsKb());
                body.put("memory_context", memoryContext != null ? memoryContext : "");
                Disposable disposable = assistantWebClient.post()
                        .uri("/api/assistant/chat/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                        })
                        .subscribe(
                                event -> {
                                    try {
                                        SecurityContextHolder.setContext(ctx);
                                        String data = event.data();
                                        if (!completed.get() && data != null && !data.isBlank()) {
                                            handleSseEvent(data, emitter, cidFinal, convFinal, seqFinal, ro.getMessage(), completed);
                                        }
                                    } finally {
                                        SecurityContextHolder.clearContext();
                                    }
                                },
                                err -> {
                                    try {
                                        SecurityContextHolder.setContext(ctx);
                                        log.error("Assistant chat stream error", err);
                                        if (!completed.getAndSet(true)) {
                                            sendTerminalEvent(emitter, "error", "Assistant stream proxy error");
                                            emitter.complete();
                                        }
                                        Disposable d = disposableRef.getAndSet(null);
                                        if (d != null && !d.isDisposed()) {
                                            d.dispose();
                                        }
                                    } finally {
                                        SecurityContextHolder.clearContext();
                                    }
                                },
                                () -> {
                                    try {
                                        SecurityContextHolder.setContext(ctx);
                                        try {
                                            if (!completed.getAndSet(true)) {
                                                sendTerminalEvent(emitter, "error", "Assistant stream ended before done event");
                                                emitter.complete();
                                            }
                                        } catch (Exception e) {
                                            log.debug("Emitter completion handling failed: {}", e.getMessage());
                                        }
                                        Disposable d = disposableRef.getAndSet(null);
                                        if (d != null && !d.isDisposed()) {
                                            d.dispose();
                                        }
                                    } finally {
                                        SecurityContextHolder.clearContext();
                                    }
                                }
                        );
                disposableRef.set(disposable);
            } catch (Exception e) {
                log.error("Assistant chatStream failed", e);
                if (!completed.getAndSet(true)) {
                    sendTerminalEvent(emitter, "error", "Assistant chat stream failed");
                    emitter.complete();
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
        return emitter;
    }

    /**
     * 更新消息反馈。通过 getMessageById、getConversationById 校验归属，仅 assistant 消息可反馈。
     *
     * @param messageId 消息 id
     * @param uid       当前用户 id
     * @param ro        反馈类型（0 取消 1 点赞 -1 点踩）
     * @return null 表示成功，否则为错误信息
     */
    @Override
    public String updateMessageFeedback(Long messageId, Long uid, FeedbackRO ro) {
        Message msg = getMessageById(messageId);
        if (msg == null) {
            return "消息不存在";
        }
        Conversation conv = getConversationById(msg.getCid());
        if (conv == null) {
            return "会话不存在";
        }
        if (!conv.getUid().equals(uid)) {
            return "无权限操作该消息";
        }
        if (!"assistant".equals(msg.getRole())) {
            return "只能对助手消息进行点赞或点踩";
        }
        Feedback feedback = ro.toFeedback();
        Message update = new Message();
        update.setId(messageId);
        update.setFeedback(feedback);
        if (messageMapper.updateById(update) > 0) {
            return null;
        }
        return "更新失败，请联系管理员";
    }

    /**
     * 获取或创建会话。cid 有效则校验并返回；cid 为 null 则复用空会话或新建。
     *
     * @param cid 会话 id，可为 null
     * @param uid 当前用户 id
     * @return 会话实体（内部使用）
     * @throws IllegalArgumentException 会话不存在或无权限时
     */
    @Override
    public Conversation getOrCreateConversation(Long cid, Long uid) {
        if (cid != null) {
            Conversation conv = conversationMapper.selectById(cid);
            if (conv == null) {
                throw new IllegalArgumentException("会话不存在");
            }
            if (!conv.getUid().equals(uid)) {
                throw new IllegalArgumentException("无权限操作该会话");
            }
            return conv;
        }
        Conversation empty = findEmptyConversation(uid);
        if (empty != null) {
            return empty;
        }
        Conversation conv = Conversation.builder()
                .uid(uid)
                .title(DEFAULT_TITLE)
                .summary(null)
                .messageCount(0)
                .build();
        conversationMapper.insert(conv);
        return conv;
    }

    /**
     * 获取会话内下一条消息的顺序号。查当前最大 sequence_num 加 1，无消息时返回 1。
     *
     * @param cid 会话 id
     * @return 下一个 sequence_num（从 1 开始，无消息时返回 1）
     */
    @Override
    public Integer getNextSequenceNum(Long cid) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getCid, cid).orderByDesc(Message::getSequenceNum).last("LIMIT 1");
        Message last = messageMapper.selectOne(wrapper);
        return last == null ? 1 : 1 + last.getSequenceNum();
    }

    /**
     * 保存消息。将 SaveMessageRO 转为 Message 实体后插入。
     *
     * @param ro 保存消息请求对象
     */
    @Override
    public void saveMessage(SaveMessageRO ro) {
        Message entity = Message.builder()
                .cid(ro.getCid())
                .role(ro.getRole())
                .content(ro.getContent())
                .sequenceNum(ro.getSequenceNum())
                .sourcesJson(ro.getSourcesJson())
                .feedback(ro.getFeedback() != null ? ro.getFeedback() : Feedback.NONE)
                .build();
        messageMapper.insert(entity);
    }

    /**
     * 更新会话。将 UpdateConversationRO 中非 null 字段更新到数据库。
     *
     * @param ro 更新会话请求对象
     */
    @Override
    public void updateConversation(UpdateConversationRO ro) {
        Conversation entity = new Conversation();
        entity.setId(ro.getId());
        if (ro.getMessageCount() != null) {
            entity.setMessageCount(ro.getMessageCount());
        }
        if (ro.getTitle() != null) {
            entity.setTitle(ro.getTitle());
        }
        if (ro.getSummary() != null) {
            entity.setSummary(ro.getSummary());
        }
        conversationMapper.updateById(entity);
    }

    /**
     * 根据 id 查询消息。供 updateMessageFeedback 等内部逻辑使用。
     *
     * @param id 消息 id
     * @return 消息实体，不存在时返回 null
     */
    @Override
    public Message getMessageById(Long id) {
        return messageMapper.selectById(id);
    }

    /**
     * 根据 id 查询会话。供 updateMessageFeedback 等内部逻辑使用。
     *
     * @param id 会话 id
     * @return 会话实体，不存在时返回 null
     */
    @Override
    public Conversation getConversationById(Long id) {
        return conversationMapper.selectById(id);
    }

    /**
     * 处理来自 Python 流式接口的单条 SSE 事件，并转发给前端。
     * 支持事件类型：start / delta / done / error。
     * - done：落库 assistant 消息并更新会话，再结束 emitter
     * - error：发送终态错误事件并结束 emitter
     * 若事件不是标准 JSON，则按 delta 文本兜底转发，避免中断流式展示。
     *
     * @param json        SSE 事件原始数据（可能带 data: 前缀）
     * @param emitter     当前 SSE 发射器
     * @param cid         会话 id
     * @param conv        会话实体
     * @param seq         user 消息 sequence_num
     * @param userMessage 用户原始提问
     * @param completed   流是否已完成标记，避免重复 complete
     */
    private void handleSseEvent(String json,
                                SseEmitter emitter,
                                Long cid,
                                Conversation conv,
                                int seq,
                                String userMessage,
                                AtomicBoolean completed) {
        String payload = json == null ? "" : json.trim();
        if (payload.isBlank()) {
            return;
        }
        if (payload.startsWith("data:")) {
            payload = payload.substring(5).trim();
        }

        JSONObject obj = null;
        try {
            obj = JSON.parseObject(payload);
        } catch (Exception ignored) {
            // fallback below
        }
        if (obj == null || obj.getString("type") == null) {
            try {
                JSONObject fallback = new JSONObject();
                fallback.put("type", "delta");
                fallback.put("content", payload);
                emitter.send(SseEmitter.event().data(fallback.toJSONString()));
            } catch (Exception ex) {
                log.debug("Forward fallback delta failed: {}", ex.getMessage());
            }
            return;
        }

        try {
            String type = obj.getString("type");
            String normalized = obj.toJSONString();
            if ("delta".equals(type)) {
                emitter.send(SseEmitter.event().data(normalized));
                return;
            }
            if ("start".equals(type)) {
                emitter.send(SseEmitter.event().data(normalized));
                return;
            }
            if ("done".equals(type)) {
                persistAssistantReply(cid, conv, seq, userMessage, obj);
                emitter.send(SseEmitter.event().data(normalized));
                completed.set(true);
                emitter.complete();
                return;
            }
            if ("error".equals(type)) {
                if (!completed.getAndSet(true)) {
                    String message = obj.getString("content");
                    sendTerminalEvent(emitter, "error", message != null ? message : "Assistant stream error");
                    emitter.complete();
                }
            }
        } catch (Exception e) {
            log.warn("Handle SSE event failed: {}", e.getMessage());
            if (!completed.getAndSet(true)) {
                sendTerminalEvent(emitter, "error", "Assistant stream event handling failed");
                emitter.complete();
            }
        }
    }

    /**
     * 在流式对话收到 done 事件后，落库 assistant 消息并更新会话统计信息。
     * 这里会写入消息内容、sources_json，并将 message_count +2（user + assistant）。
     * 若当前会话标题仍为默认值，则用首条用户问题截断后更新标题。
     *
     * @param cid         会话 id
     * @param conv        当前会话实体
     * @param seq         user 消息的 sequence_num
     * @param userMessage 用户问题文本（用于首条标题生成）
     * @param donePayload Python done 事件负载，包含 content/sources
     */
    private void persistAssistantReply(Long cid,
                                       Conversation conv,
                                       int seq,
                                       String userMessage,
                                       JSONObject donePayload) {
        String content = donePayload.getString("content");
        String sourcesJson = "[]";
        JSONArray arr = donePayload.getJSONArray("sources");
        if (arr != null) {
            sourcesJson = arr.toJSONString();
        }
        saveMessage(SaveMessageRO.builder()
                .cid(cid)
                .role("assistant")
                .content(content != null ? content : "")
                .sequenceNum(seq + 1)
                .sourcesJson(sourcesJson)
                .feedback(Feedback.NONE)
                .build());
        int newCount = (conv.getMessageCount() == null ? 0 : conv.getMessageCount()) + 2;
        String newTitle = conv.getTitle();
        if (DEFAULT_TITLE.equals(conv.getTitle())) {
            newTitle = userMessage.length() > TITLE_MAX_LEN ? userMessage.substring(0, TITLE_MAX_LEN) + "..." : userMessage;
        }
        updateConversation(UpdateConversationRO.builder()
                .id(cid)
                .messageCount(newCount)
                .title(newTitle)
                .build());
    }

    /**
     * 向前端发送终态 SSE 事件（如 error），用于显式结束流式状态。
     * 事件体格式：{"type":"...","content":"..."}。
     *
     * @param emitter 当前 SSE 发射器
     * @param type    事件类型（error / done 等）
     * @param message 终态提示信息
     */
    private void sendTerminalEvent(SseEmitter emitter, String type, String message) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("type", type);
            payload.put("content", message);
            emitter.send(SseEmitter.event().data(payload.toJSONString()));
        } catch (Exception ex) {
            log.debug("Send terminal SSE event failed: {}", ex.getMessage());
        }
    }

    /**
     * 查找当前用户的空会话（message_count=0），用于复用。
     *
     * @param uid 当前用户 id
     * @return 空会话，不存在时返回 null
     */
    private Conversation findEmptyConversation(Long uid) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUid, uid).eq(Conversation::getMessageCount, 0).last("LIMIT 1");
        return conversationMapper.selectOne(wrapper);
    }

    /**
     * 获取会话最后一条消息的预览（截取前 80 字）。
     *
     * @param cid 会话 id
     * @return 最后一条消息内容预览，无消息时返回空字符串
     */
    private String getLastMessagePreview(Long cid) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getCid, cid).orderByDesc(Message::getSequenceNum).last("LIMIT 1");
        Message last = messageMapper.selectOne(wrapper);
        if (last == null || last.getContent() == null) {
            return "";
        }
        String content = last.getContent();
        return content.length() > 80 ? content.substring(0, 80) + "..." : content;
    }

    /**
     * 调用 Python FastAPI /api/assistant/classify 进行意图分类。
     *
     * @param message 用户消息内容
     * @return 分类结果（needs_tasks、needs_algorithms、needs_datasets、needs_kb），异常时业务数据全 false、needs_kb 默认 true
     */
    private ClassifyResult callPythonClassify(String message) {
        String url = pythonFastApiUrl + "/api/assistant/classify";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject();
        body.put("message", message);
        HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return new ClassifyResult(false, false, false, true);
            }
            JSONObject json = JSON.parseObject(response.getBody());
            Integer code = json.getInteger("code");
            if (code == null || code != 200) {
                return new ClassifyResult(false, false, false, true);
            }
            JSONObject data = json.getJSONObject("data");
            if (data == null) {
                return new ClassifyResult(false, false, false, true);
            }
            boolean needsTasks = Boolean.TRUE.equals(data.getBoolean("needs_tasks"));
            boolean needsAlgorithms = Boolean.TRUE.equals(data.getBoolean("needs_algorithms"));
            boolean needsDatasets = Boolean.TRUE.equals(data.getBoolean("needs_datasets"));
            boolean needsKb = data.get("needs_kb") == null ? true : Boolean.TRUE.equals(data.getBoolean("needs_kb"));
            return new ClassifyResult(needsTasks, needsAlgorithms, needsDatasets, needsKb);
        } catch (Exception e) {
            log.warn("Assistant classify failed, using empty context: {}", e.getMessage());
            return new ClassifyResult(false, false, false, true);
        }
    }

    /**
     * 根据意图分类结果预取业务数据，仅返回 VO，不返回实体。
     *
     * @param uid            当前用户 id
     * @param classifyResult 意图分类结果
     * @return context_data Map，供 Python 拼入 prompt
     */
    private Map<String, Object> buildContextData(Long uid, ClassifyResult classifyResult) {
        Map<String, Object> ctx = new HashMap<>();
        boolean isAdmin = isAdmin();

        if (classifyResult.needsTasks()) {
            List<TaskVO> recentTasks = dashboardService.getRecentTasks(uid, isAdmin);
            ctx.put("recentTasks", recentTasks.size() > 5 ? recentTasks.subList(0, 5) : recentTasks);
            DashboardStatCardsVO taskStats = dashboardService.getStatCards(uid, isAdmin);
            ctx.put("taskStats", taskStats);
        }
        if (classifyResult.needsAlgorithms()) {
            List<AlgorithmVO> algorithms = algorithmService.listAlgorithms(
                    ListAlgorithmRO.builder().all(true).build()).getRecords();
            ctx.put("algorithms", algorithms);
        }
        if (classifyResult.needsDatasets()) {
            List<DatasetVO> datasets = datasetService.listDatasets(
                    ListDatasetRO.builder().all(true).build()).getRecords();
            ctx.put("datasets", datasets);
        }
        return ctx;
    }

    /**
     * 构建 memory_context 并在满足条件时更新摘要（步骤 15）。
     * 规则：每次请求传 summary（可为空）+ 最近 4 轮完整对话；第 5 轮（9 条消息）摘要仍为空，第 6 轮（11 条）起开始压缩。
     * 触发：currentCount > 9（即第 11、13、15… 条消息时，有需要压缩的历史）。
     * olderCount = currentCount - 9（排除当前 user 后，超出「最近 8 条」的旧消息数）。
     * 摘要：已有摘要则 prev_summary + 新 2 条 -> 新摘要覆盖；否则仅新消息生成。限制 300 字。
     *
     * @param cid          会话 id
     * @param conv         会话实体（会原地更新 summary 并写库）
     * @param currentCount 当前消息总数（已含刚存的 user 消息）
     * @return memory_context 字符串，供 Python 拼入 prompt
     */
    private String buildMemoryContextAndMaybeUpdateSummary(Long cid, Conversation conv, int currentCount) {
        List<Message> allMessages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>().eq(Message::getCid, cid).orderByAsc(Message::getSequenceNum));
        if (allMessages.isEmpty()) {
            return "";
        }

        if (currentCount > 9) {
            int olderCount = currentCount - 9;
            if (olderCount > 0 && olderCount <= allMessages.size()) {
                String prevSummary = conv.getSummary();
                List<Map<String, Object>> toSummarize;
                if (prevSummary != null && !prevSummary.isBlank()) {
                    int from = Math.max(0, olderCount - 2);
                    toSummarize = toMessageMaps(allMessages.subList(from, olderCount));
                } else {
                    toSummarize = toMessageMaps(allMessages.subList(0, olderCount));
                }
                String newSummary = callPythonSummarize(prevSummary, toSummarize);
                if (newSummary != null && !newSummary.isBlank()) {
                    conv.setSummary(newSummary);
                    updateConversation(UpdateConversationRO.builder().id(cid).summary(newSummary).build());
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        if (conv.getSummary() != null && !conv.getSummary().isBlank()) {
            sb.append("[历史摘要]\n").append(conv.getSummary()).append("\n\n");
        }
        sb.append("[最近对话]\n");
        int excludeLast = 1;
        int recentSize = Math.min(8, allMessages.size() - excludeLast);
        if (recentSize <= 0) {
            return sb.toString();
        }
        int recentStart = allMessages.size() - excludeLast - recentSize;
        if (recentStart < 0) {
            recentStart = 0;
        }
        List<Message> recent = allMessages.subList(recentStart, allMessages.size() - excludeLast);
        for (Message m : recent) {
            String role = "user".equals(m.getRole()) ? "user" : "assistant";
            String content = m.getContent() != null ? m.getContent() : "";
            sb.append(role).append(": ").append(content);
            if ("assistant".equals(m.getRole())) {
                Feedback fb = m.getFeedback() != null ? m.getFeedback() : Feedback.NONE;
                sb.append(" [用户反馈：").append(fb.getDescription()).append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private List<Map<String, Object>> toMessageMaps(List<Message> messages) {
        List<Map<String, Object>> list = new ArrayList<>(messages.size());
        for (Message m : messages) {
            Map<String, Object> map = new HashMap<>();
            map.put("role", m.getRole());
            map.put("content", m.getContent());
            if ("assistant".equals(m.getRole())) {
                Feedback fb = m.getFeedback() != null ? m.getFeedback() : Feedback.NONE;
                map.put("feedback", fb.getDescription());
            }
            list.add(map);
        }
        return list;
    }

    /**
     * 调用 Python FastAPI /api/assistant/summarize 生成增量摘要。
     */
    private String callPythonSummarize(String prevSummary, List<Map<String, Object>> messages) {
        String url = pythonFastApiUrl + "/api/assistant/summarize";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject();
        body.put("prev_summary", prevSummary != null ? prevSummary : "");
        body.put("messages", messages);
        HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return prevSummary;
            }
            JSONObject json = JSON.parseObject(response.getBody());
            Integer code = json.getInteger("code");
            if (code == null || code != 200) {
                return prevSummary;
            }
            JSONObject data = json.getJSONObject("data");
            if (data == null) {
                return prevSummary;
            }
            String summary = data.getString("summary");
            return summary != null ? summary : prevSummary;
        } catch (Exception e) {
            log.warn("Assistant summarize failed: {}", e.getMessage());
            return prevSummary;
        }
    }

    /**
     * 判断当前用户是否为管理员。
     */
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_admin"::equals);
    }

    /**
     * 调用 Python FastAPI /api/assistant/chat 获取非流式回复。
     *
     * @param message     用户消息内容
     * @param contextData 预取的业务数据（可为空）
     * @param needsKb     是否需要知识库检索（步骤 14 编排）
     * @return 助手回复（content、sources）
     * @throws RuntimeException 调用失败或响应格式异常时
     */
    private ChatResponseVO callPythonChat(String message, Map<String, Object> contextData, boolean needsKb, String memoryContext) {
        String url = pythonFastApiUrl + "/api/assistant/chat";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject();
        body.put("message", message);
        body.put("context_data", contextData != null ? contextData : Collections.emptyMap());
        body.put("needs_kb", needsKb);
        body.put("memory_context", memoryContext != null ? memoryContext : "");
        HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("智能助手服务调用失败");
        }

        JSONObject json = JSON.parseObject(response.getBody());
        Integer code = json.getInteger("code");
        if (code == null || code != 200) {
            String msg = json.getString("message");
            throw new RuntimeException(msg != null ? msg : "智能助手服务返回错误");
        }

        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            throw new RuntimeException("智能助手服务返回数据格式异常");
        }
        String content = data.getString("content");
        List<String> sources = new ArrayList<>();
        JSONArray arr = data.getJSONArray("sources");
        if (arr != null) {
            for (Object o : arr) {
                if (o != null) {
                    sources.add(o.toString());
                }
            }
        }
        return ChatResponseVO.builder()
                .content(content != null ? content : "")
                .sources(sources)
                .build();
    }

    /**
     * 解析 sources_json 为字符串列表。
     *
     * @param sourcesJson JSON 数组字符串
     * @return 解析后的列表，解析失败返回空列表
     */
    private List<String> parseSourcesJson(String sourcesJson) {
        if (sourcesJson == null || sourcesJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            JSONArray arr = JSON.parseArray(sourcesJson);
            if (arr == null) {
                return Collections.emptyList();
            }
            List<String> sources = new ArrayList<>(arr.size());
            for (Object o : arr) {
                if (o != null) {
                    sources.add(o.toString());
                }
            }
            return sources;
        } catch (Exception e) {
            log.warn("Parse sources_json failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 意图分类结果，用于决定预取哪些业务数据及是否做知识库检索（步骤 14）。
     */
    private record ClassifyResult(boolean needsTasks, boolean needsAlgorithms, boolean needsDatasets, boolean needsKb) {
    }
}
