package ynu.jackielinn.server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 助手模块接口集成测试。
 * 覆盖 Controller -> Service -> Mapper -> H2 链路。
 */
@Sql(
        scripts = {
                "classpath:integration/assistant/schema.sql",
                "classpath:integration/assistant/data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class AssistantIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "assistantChatStreamExecutor")
    private Executor assistantChatStreamExecutor;

    /**
     * 成功场景：创建会话成功。
     */
    @Test
    void createConversationShouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/assistant/conversation")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    /**
     * 失败场景：未登录创建会话。
     */
    @Test
    void createConversationShouldReturnFailureWhenNotLogin() throws Exception {
        mockMvc.perform(post("/api/assistant/conversation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 成功场景：查询会话列表成功。
     */
    @Test
    void listConversationsShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/assistant/conversation/list")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    /**
     * 失败场景：未登录查询会话列表。
     */
    @Test
    void listConversationsShouldReturnFailureWhenNotLogin() throws Exception {
        mockMvc.perform(get("/api/assistant/conversation/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 成功场景：查询会话详情成功。
     */
    @Test
    void getConversationShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/assistant/conversation/1")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.messages").isArray());
    }

    /**
     * 失败场景：查询不存在会话详情。
     */
    @Test
    void getConversationShouldReturnFailureWhenNotExists() throws Exception {
        mockMvc.perform(get("/api/assistant/conversation/99")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 成功场景：删除会话成功。
     */
    @Test
    void deleteConversationShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/assistant/conversation/1")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 失败场景：删除他人会话失败。
     */
    @Test
    void deleteConversationShouldReturnFailureWhenNoPermission() throws Exception {
        mockMvc.perform(delete("/api/assistant/conversation/2")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：更新会话标题成功。
     */
    @Test
    void updateTitleShouldReturnSuccess() throws Exception {
        String body = """
                {
                  "title": "新的标题"
                }
                """;
        mockMvc.perform(put("/api/assistant/conversation/1/title")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 失败场景：更新他人会话标题失败。
     */
    @Test
    void updateTitleShouldReturnFailureWhenNoPermission() throws Exception {
        String body = """
                {
                  "title": "新的标题"
                }
                """;
        mockMvc.perform(put("/api/assistant/conversation/2/title")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：聊天接口成功返回助手回复。
     */
    @Test
    void chatShouldReturnSuccess() throws Exception {
        ResponseEntity<String> classifyResp = ResponseEntity.ok("""
                {"code":200,"data":{"needs_tasks":false,"needs_algorithms":false,"needs_datasets":false,"needs_kb":false},"message":"ok"}
                """);
        ResponseEntity<String> chatResp = ResponseEntity.ok("""
                {"code":200,"data":{"content":"assistant reply","sources":["doc1"]},"message":"ok"}
                """);

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(classifyResp, chatResp);

        String body = """
                {
                  "message": "你好"
                }
                """;
        mockMvc.perform(post("/api/assistant/chat")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("assistant reply"));
    }

    /**
     * 失败场景：聊天接口上游返回异常。
     */
    @Test
    void chatShouldReturnFailureWhenUpstreamError() throws Exception {
        ResponseEntity<String> classifyResp = ResponseEntity.ok("""
                {"code":200,"data":{"needs_tasks":false,"needs_algorithms":false,"needs_datasets":false,"needs_kb":false},"message":"ok"}
                """);
        ResponseEntity<String> chatResp = new ResponseEntity<>("""
                {"code":500,"message":"upstream error"}
                """, HttpStatusCode.valueOf(200));

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(classifyResp, chatResp);

        String body = """
                {
                  "message": "你好"
                }
                """;
        mockMvc.perform(post("/api/assistant/chat")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    /**
     * 成功场景：流式聊天接口返回 SSE。
     */
    @Test
    void chatStreamShouldReturnSuccess() throws Exception {
        doAnswer(invocation -> null).when(assistantChatStreamExecutor).execute(any(Runnable.class));

        String body = """
                {
                  "message": "你好"
                }
                """;
        mockMvc.perform(post("/api/assistant/chat/stream")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    /**
     * 失败场景：未登录访问流式聊天接口。
     */
    @Test
    void chatStreamShouldReturnFailureWhenNotLogin() throws Exception {
        String body = """
                {
                  "message": "你好"
                }
                """;
        mockMvc.perform(post("/api/assistant/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 成功场景：更新消息反馈成功。
     */
    @Test
    void updateMessageFeedbackShouldReturnSuccess() throws Exception {
        String body = """
                {
                  "feedback": 1
                }
                """;
        mockMvc.perform(put("/api/assistant/message/2/feedback")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 失败场景：对用户消息反馈失败。
     */
    @Test
    void updateMessageFeedbackShouldReturnFailureWhenMessageRoleInvalid() throws Exception {
        String body = """
                {
                  "feedback": -1
                }
                """;
        mockMvc.perform(put("/api/assistant/message/1/feedback")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
