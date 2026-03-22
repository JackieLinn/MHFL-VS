package ynu.jackielinn.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ynu.jackielinn.server.dto.request.CreateAccountRO;
import ynu.jackielinn.server.dto.request.ListAccountRO;
import ynu.jackielinn.server.dto.request.UpdateAccountRO;
import ynu.jackielinn.server.dto.response.AccountVO;
import ynu.jackielinn.server.service.AccountService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AccountController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAccountShouldReturnSuccess() throws Exception {
        CreateAccountRO ro = CreateAccountRO.builder()
                .username("user1")
                .email("user@example.com")
                .telephone("13800138000")
                .build();
        when(accountService.createAccount(any(CreateAccountRO.class))).thenReturn(null);

        mockMvc.perform(post("/api/account/admin/create")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteAccountShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(delete("/api/account/admin/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void deleteAccountShouldReturnSuccessForAdmin() throws Exception {
        setAdminAuthentication();
        when(accountService.deleteAccount(2L, 1L)).thenReturn(null);

        mockMvc.perform(delete("/api/account/admin/2").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(accountService).deleteAccount(2L, 1L);
    }

    @Test
    void updateAccountShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        UpdateAccountRO ro = new UpdateAccountRO();

        mockMvc.perform(put("/api/account/update")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void updateAccountShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        UpdateAccountRO ro = UpdateAccountRO.builder().username("newname").build();
        when(accountService.updateAccount(any(UpdateAccountRO.class), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn("duplicate username");

        mockMvc.perform(put("/api/account/update")
                        .requestAttr("id", 1L)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("duplicate username"));
    }

    @Test
    void listAccountsShouldReturnPageData() throws Exception {
        Page<AccountVO> page = new Page<>(1, 10);
        page.setRecords(List.of(new AccountVO()));
        when(accountService.listAccounts(any(ListAccountRO.class))).thenReturn(page);

        mockMvc.perform(get("/api/account/admin/list").param("current", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void getAccountInfoShouldReturnNotFoundWhenServiceReturnsNull() throws Exception {
        when(accountService.getAccountInfo(1L)).thenReturn(null);

        mockMvc.perform(get("/api/account/info").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getAccountInfoShouldReturnSuccess() throws Exception {
        when(accountService.getAccountInfo(1L)).thenReturn(new AccountVO());

        mockMvc.perform(get("/api/account/info").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
    }
}
