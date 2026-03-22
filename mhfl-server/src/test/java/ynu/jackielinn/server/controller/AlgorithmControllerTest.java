package ynu.jackielinn.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import ynu.jackielinn.server.dto.request.ListAlgorithmRO;
import ynu.jackielinn.server.dto.response.AlgorithmVO;
import ynu.jackielinn.server.service.AlgorithmService;

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

@WebMvcTest(value = AlgorithmController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class AlgorithmControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlgorithmService algorithmService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAlgorithmShouldReturnSuccessForAdmin() throws Exception {
        setAdminAuthentication();
        when(algorithmService.createAlgorithm("FedAvg")).thenReturn(null);

        mockMvc.perform(post("/api/algorithm/admin/create")
                        .requestAttr("id", 1L)
                        .param("algorithmName", "FedAvg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(algorithmService).createAlgorithm("FedAvg");
    }

    @Test
    void createAlgorithmShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(post("/api/algorithm/admin/create")
                        .param("algorithmName", "FedAvg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void createAlgorithmShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        setAdminAuthentication();
        when(algorithmService.createAlgorithm("FedAvg")).thenReturn("duplicate");

        mockMvc.perform(post("/api/algorithm/admin/create")
                        .requestAttr("id", 1L)
                        .param("algorithmName", "FedAvg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("duplicate"));
    }

    @Test
    void createAlgorithmShouldReturnForbiddenWhenNotAdmin() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of(new SimpleGrantedAuthority("ROLE_user"))));

        mockMvc.perform(post("/api/algorithm/admin/create")
                        .requestAttr("id", 1L)
                        .param("algorithmName", "FedAvg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void createAlgorithmShouldReturnBadRequestWhenNameBlank() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(post("/api/algorithm/admin/create")
                        .requestAttr("id", 1L)
                        .param("algorithmName", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAlgorithmShouldReturnSuccessForAdmin() throws Exception {
        setAdminAuthentication();
        when(algorithmService.deleteAlgorithm(2L)).thenReturn(null);

        mockMvc.perform(delete("/api/algorithm/admin/2").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(algorithmService).deleteAlgorithm(2L);
    }

    @Test
    void deleteAlgorithmShouldReturnForbiddenWhenNotAdmin() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of(new SimpleGrantedAuthority("ROLE_user"))));

        mockMvc.perform(delete("/api/algorithm/admin/2")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void deleteAlgorithmShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        setAdminAuthentication();
        when(algorithmService.deleteAlgorithm(2L)).thenReturn("not exists");

        mockMvc.perform(delete("/api/algorithm/admin/2").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("not exists"));
    }

    @Test
    void updateAlgorithmShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        setAdminAuthentication();
        when(algorithmService.updateAlgorithm(2L, "FedProto")).thenReturn("duplicate");

        mockMvc.perform(put("/api/algorithm/admin/2")
                        .requestAttr("id", 1L)
                        .param("algorithmName", "FedProto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("duplicate"));
    }

    @Test
    void updateAlgorithmShouldReturnSuccessWhenServiceReturnsNull() throws Exception {
        setAdminAuthentication();
        when(algorithmService.updateAlgorithm(2L, "FedProto")).thenReturn(null);

        mockMvc.perform(put("/api/algorithm/admin/2")
                        .requestAttr("id", 1L)
                        .param("algorithmName", "FedProto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateAlgorithmShouldReturnForbiddenWhenNotAdmin() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of(new SimpleGrantedAuthority("ROLE_user"))));

        mockMvc.perform(put("/api/algorithm/admin/2")
                        .requestAttr("id", 1L)
                        .param("algorithmName", "FedProto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void listAlgorithmsShouldReturnPageData() throws Exception {
        Page<AlgorithmVO> page = new Page<>(1, 10);
        page.setRecords(List.of(new AlgorithmVO()));
        when(algorithmService.listAlgorithms(any(ListAlgorithmRO.class))).thenReturn(page);

        mockMvc.perform(get("/api/algorithm/list").param("current", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());

        verify(algorithmService).listAlgorithms(any(ListAlgorithmRO.class));
    }

    @Test
    void listAlgorithmsShouldReturnBadRequestWhenSizeInvalid() throws Exception {
        mockMvc.perform(get("/api/algorithm/list").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listAlgorithmsShouldReturnBadRequestWhenCurrentInvalid() throws Exception {
        mockMvc.perform(get("/api/algorithm/list").param("current", "0"))
                .andExpect(status().isBadRequest());
    }

    private void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
    }
}
