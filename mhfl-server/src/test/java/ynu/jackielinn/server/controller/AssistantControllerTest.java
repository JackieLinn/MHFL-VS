package ynu.jackielinn.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ynu.jackielinn.server.dto.request.ChatRequestRO;
import ynu.jackielinn.server.dto.request.FeedbackRO;
import ynu.jackielinn.server.dto.request.UpdateTitleRO;
import ynu.jackielinn.server.dto.response.ChatResponseVO;
import ynu.jackielinn.server.dto.response.ConversationDetailVO;
import ynu.jackielinn.server.dto.response.ConversationVO;
import ynu.jackielinn.server.dto.response.CreateConversationVO;
import ynu.jackielinn.server.service.AssistantService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AssistantController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class AssistantControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssistantService assistantService;

    @Test
    void createConversationShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(post("/api/assistant/conversation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void createConversationShouldReturnSuccess() throws Exception {
        when(assistantService.createConversation(1L)).thenReturn(CreateConversationVO.builder().id(10L).build());

        mockMvc.perform(post("/api/assistant/conversation").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void listConversationsShouldReturnSuccess() throws Exception {
        when(assistantService.listByUserId(1L, "test")).thenReturn(List.of(new ConversationVO()));

        mockMvc.perform(get("/api/assistant/conversation/list")
                        .requestAttr("id", 1L)
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void listConversationsShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/assistant/conversation/list").param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getConversationShouldReturnNotFoundWhenServiceReturnsNull() throws Exception {
        when(assistantService.getDetail(1L, 1L)).thenReturn(null);

        mockMvc.perform(get("/api/assistant/conversation/1").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getConversationShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/assistant/conversation/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void deleteConversationShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        when(assistantService.deleteConversation(1L, 1L)).thenReturn("no permission");

        mockMvc.perform(delete("/api/assistant/conversation/1").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void deleteConversationShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(delete("/api/assistant/conversation/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void updateTitleShouldReturnSuccess() throws Exception {
        UpdateTitleRO ro = UpdateTitleRO.builder().title("new title").build();
        when(assistantService.updateTitle(1L, 1L, "new title")).thenReturn(null);

        mockMvc.perform(put("/api/assistant/conversation/1/title")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateTitleShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        UpdateTitleRO ro = UpdateTitleRO.builder().title("new title").build();

        mockMvc.perform(put("/api/assistant/conversation/1/title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void chatShouldReturnBadRequestWhenServiceThrowsIllegalArgument() throws Exception {
        ChatRequestRO ro = ChatRequestRO.builder().message("hello").build();
        when(assistantService.chat(eq(1L), any(ChatRequestRO.class)))
                .thenThrow(new IllegalArgumentException("bad request"));

        mockMvc.perform(post("/api/assistant/chat")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("bad request"));
    }

    @Test
    void chatShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        ChatRequestRO ro = ChatRequestRO.builder().message("hello").build();

        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void chatShouldReturnServerErrorWhenServiceThrowsRuntimeException() throws Exception {
        ChatRequestRO ro = ChatRequestRO.builder().message("hello").build();
        when(assistantService.chat(eq(1L), any(ChatRequestRO.class)))
                .thenThrow(new RuntimeException("upstream down"));

        mockMvc.perform(post("/api/assistant/chat")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("upstream down"));
    }

    @Test
    void chatShouldReturnSuccess() throws Exception {
        ChatRequestRO ro = ChatRequestRO.builder().message("hello").build();
        when(assistantService.chat(eq(1L), any(ChatRequestRO.class)))
                .thenReturn(ChatResponseVO.builder().content("hi").sources(List.of("doc")).build());

        mockMvc.perform(post("/api/assistant/chat")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("hi"));
    }

    @Test
    void chatStreamShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        ChatRequestRO ro = ChatRequestRO.builder().message("hello").build();

        mockMvc.perform(post("/api/assistant/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chatStreamShouldReturnSseEmitter() throws Exception {
        ChatRequestRO ro = ChatRequestRO.builder().message("hello").build();
        when(assistantService.chatStream(eq(1L), any(ChatRequestRO.class))).thenReturn(new SseEmitter());

        mockMvc.perform(post("/api/assistant/chat/stream")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void updateMessageFeedbackShouldReturnSuccess() throws Exception {
        FeedbackRO ro = FeedbackRO.builder().feedback(1).build();
        when(assistantService.updateMessageFeedback(eq(1L), eq(1L), any(FeedbackRO.class))).thenReturn(null);

        mockMvc.perform(put("/api/assistant/message/1/feedback")
                        .requestAttr("id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(assistantService).updateMessageFeedback(eq(1L), eq(1L), any(FeedbackRO.class));
    }

    @Test
    void updateMessageFeedbackShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        FeedbackRO ro = FeedbackRO.builder().feedback(1).build();

        mockMvc.perform(put("/api/assistant/message/1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
