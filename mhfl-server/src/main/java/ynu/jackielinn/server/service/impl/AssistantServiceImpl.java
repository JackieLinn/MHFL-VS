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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ynu.jackielinn.server.common.Feedback;
import ynu.jackielinn.server.dto.request.ChatRequestRO;
import ynu.jackielinn.server.dto.request.FeedbackRO;
import ynu.jackielinn.server.dto.request.SaveMessageRO;
import ynu.jackielinn.server.dto.request.UpdateConversationRO;
import ynu.jackielinn.server.dto.response.*;
import ynu.jackielinn.server.entity.Conversation;
import ynu.jackielinn.server.entity.Message;
import ynu.jackielinn.server.mapper.ConversationMapper;
import ynu.jackielinn.server.mapper.MessageMapper;
import ynu.jackielinn.server.service.AssistantService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class AssistantServiceImpl implements AssistantService {

    private static final String DEFAULT_TITLE = "新建对话";
    private static final int TITLE_MAX_LEN = 18;

    @Value("${python.fastapi.url:http://localhost:8000}")
    private String pythonFastApiUrl;

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
    public List<ConversationVO> listByUserId(Long uid) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUid, uid)
                .gt(Conversation::getMessageCount, 0)
                .orderByDesc(Conversation::getUpdateTime);
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

        // 2. 调 Python
        ChatResponseVO resp = callPythonChat(ro.getMessage());

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
        SseEmitter emitter = new SseEmitter(300_000L);
        SecurityContext ctx = SecurityContextHolder.getContext();
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
                final AtomicBoolean completed = new AtomicBoolean(false);

                Map<String, Object> body = new HashMap<>();
                body.put("message", ro.getMessage());
                body.put("context_data", null);
                assistantWebClient.post()
                        .uri("/api/assistant/chat/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                        })
                        .subscribe(
                                event -> {
                                    SecurityContextHolder.setContext(ctx);
                                    String data = event.data();
                                    if (data != null && !data.isBlank()) {
                                        handleSseEvent(data, emitter, cidFinal, convFinal, seqFinal, ro.getMessage(), completed);
                                    }
                                },
                                err -> {
                                    SecurityContextHolder.setContext(ctx);
                                    log.error("Assistant chat stream error", err);
                                    if (!completed.get()) {
                                        sendTerminalEvent(emitter, "error", "Assistant stream proxy error");
                                        completed.set(true);
                                        emitter.complete();
                                    }
                                },
                                () -> {
                                    SecurityContextHolder.setContext(ctx);
                                    try {
                                        if (!completed.get()) {
                                            sendTerminalEvent(emitter, "error", "Assistant stream ended before done event");
                                            completed.set(true);
                                            emitter.complete();
                                        }
                                    } catch (Exception e) {
                                        log.debug("Emitter completion handling failed: {}", e.getMessage());
                                    }
                                }
                        );
            } catch (Exception e) {
                log.error("Assistant chatStream failed", e);
                sendTerminalEvent(emitter, "error", "Assistant chat stream failed");
                emitter.complete();
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

    private void handleSseEvent(String json,
                                SseEmitter emitter,
                                Long cid,
                                Conversation conv,
                                int seq,
                                String userMessage,
                                AtomicBoolean completed) {
        try {
            JSONObject obj = JSON.parseObject(json);
            String type = obj.getString("type");
            if ("delta".equals(type)) {
                emitter.send(SseEmitter.event().data(json));
                return;
            }
            if ("done".equals(type)) {
                persistAssistantReply(cid, conv, seq, userMessage, obj);
                emitter.send(SseEmitter.event().data(json));
                completed.set(true);
                emitter.complete();
                return;
            }
            if ("error".equals(type)) {
                completed.set(true);
                String message = obj.getString("content");
                sendTerminalEvent(emitter, "error", message != null ? message : "Assistant stream error");
                emitter.complete();
            }
        } catch (Exception e) {
            log.warn("Parse SSE event failed: {}", e.getMessage());
        }
    }

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
     * 调用 Python FastAPI /api/assistant/chat 获取非流式回复。
     *
     * @param message 用户消息内容
     * @return 助手回复（content、sources）
     * @throws RuntimeException 调用失败或响应格式异常时
     */
    private ChatResponseVO callPythonChat(String message) {
        String url = pythonFastApiUrl + "/api/assistant/chat";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject();
        body.put("message", message);
        body.put("context_data", null);
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
}
