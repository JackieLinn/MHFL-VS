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
import ynu.jackielinn.server.dto.request.ConfirmResetRO;
import ynu.jackielinn.server.dto.request.EmailRegisterRO;
import ynu.jackielinn.server.dto.request.EmailResetRO;
import ynu.jackielinn.server.service.AccountService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthorizeController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class AuthorizeControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @Test
    void askVerifyCodeShouldReturnSuccess() throws Exception {
        when(accountService.registerEmailVerifyCode("register", "user@example.com", "127.0.0.1")).thenReturn(null);

        mockMvc.perform(get("/auth/ask-code")
                        .param("email", "user@example.com")
                        .param("type", "register")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(accountService).registerEmailVerifyCode("register", "user@example.com", "127.0.0.1");
    }

    @Test
    void askVerifyCodeShouldReturnBusinessBadRequestWhenTypeInvalid() throws Exception {
        mockMvc.perform(get("/auth/ask-code")
                        .param("email", "user@example.com")
                        .param("type", "other"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void askVerifyCodeShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        when(accountService.registerEmailVerifyCode("register", "user@example.com", "127.0.0.1"))
                .thenReturn("too frequent");

        mockMvc.perform(get("/auth/ask-code")
                        .param("email", "user@example.com")
                        .param("type", "register")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("too frequent"));
    }

    @Test
    void registerShouldReturnSuccess() throws Exception {
        EmailRegisterRO ro = EmailRegisterRO.builder()
                .telephone("13800138000")
                .email("user@example.com")
                .code("123456")
                .username("user1")
                .password("123456")
                .build();
        when(accountService.registerEmailAccount(any(EmailRegisterRO.class))).thenReturn(null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void registerShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        EmailRegisterRO ro = EmailRegisterRO.builder()
                .telephone("13800138000")
                .email("user@example.com")
                .code("123456")
                .username("user1")
                .password("123456")
                .build();
        when(accountService.registerEmailAccount(any(EmailRegisterRO.class))).thenReturn("username exists");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("username exists"));
    }

    @Test
    void registerShouldReturnBadRequestWhenBodyInvalid() throws Exception {
        EmailRegisterRO ro = EmailRegisterRO.builder()
                .telephone("1")
                .email("bad-email")
                .code("1")
                .username("")
                .password("1")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetConfirmShouldReturnSuccess() throws Exception {
        ConfirmResetRO ro = ConfirmResetRO.builder()
                .email("user@example.com")
                .code("123456")
                .build();
        when(accountService.resetConfirm(any(ConfirmResetRO.class))).thenReturn(null);

        mockMvc.perform(post("/auth/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void resetConfirmShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        ConfirmResetRO ro = ConfirmResetRO.builder()
                .email("user@example.com")
                .code("123456")
                .build();
        when(accountService.resetConfirm(any(ConfirmResetRO.class))).thenReturn("invalid code");

        mockMvc.perform(post("/auth/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid code"));
    }

    @Test
    void resetConfirmShouldReturnBadRequestWhenBodyInvalid() throws Exception {
        ConfirmResetRO ro = ConfirmResetRO.builder()
                .email("bad-email")
                .code("1")
                .build();

        mockMvc.perform(post("/auth/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordShouldReturnSuccess() throws Exception {
        EmailResetRO ro = EmailResetRO.builder()
                .email("user@example.com")
                .code("123456")
                .password("123456")
                .build();
        when(accountService.resetEmailAccountPassword(any(EmailResetRO.class))).thenReturn(null);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void resetPasswordShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        EmailResetRO ro = EmailResetRO.builder()
                .email("user@example.com")
                .code("123456")
                .password("123456")
                .build();
        when(accountService.resetEmailAccountPassword(any(EmailResetRO.class))).thenReturn("reset failed");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("reset failed"));
    }

    @Test
    void resetPasswordShouldReturnBadRequestWhenBodyInvalid() throws Exception {
        EmailResetRO ro = EmailResetRO.builder()
                .email("bad-email")
                .code("1")
                .password("123")
                .build();

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isBadRequest());
    }
}
