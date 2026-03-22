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
import ynu.jackielinn.server.dto.request.ListDatasetRO;
import ynu.jackielinn.server.dto.response.DatasetVO;
import ynu.jackielinn.server.service.DatasetService;

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

@WebMvcTest(value = DatasetController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class DatasetControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DatasetService datasetService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDatasetShouldReturnSuccessForAdmin() throws Exception {
        setAdminAuthentication();
        when(datasetService.createDataset("CIFAR-100")).thenReturn(null);

        mockMvc.perform(post("/api/dataset/admin/create")
                        .requestAttr("id", 1L)
                        .param("dataName", "CIFAR-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(datasetService).createDataset("CIFAR-100");
    }

    @Test
    void createDatasetShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(post("/api/dataset/admin/create")
                        .param("dataName", "CIFAR-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void createDatasetShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        setAdminAuthentication();
        when(datasetService.createDataset("CIFAR-100")).thenReturn("duplicate");

        mockMvc.perform(post("/api/dataset/admin/create")
                        .requestAttr("id", 1L)
                        .param("dataName", "CIFAR-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("duplicate"));
    }

    @Test
    void createDatasetShouldReturnForbiddenWhenNotAdmin() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of(new SimpleGrantedAuthority("ROLE_user"))));

        mockMvc.perform(post("/api/dataset/admin/create")
                        .requestAttr("id", 1L)
                        .param("dataName", "CIFAR-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void createDatasetShouldReturnBadRequestWhenNameBlank() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(post("/api/dataset/admin/create")
                        .requestAttr("id", 1L)
                        .param("dataName", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteDatasetShouldReturnSuccessForAdmin() throws Exception {
        setAdminAuthentication();
        when(datasetService.deleteDataset(1L)).thenReturn(null);

        mockMvc.perform(delete("/api/dataset/admin/1").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(datasetService).deleteDataset(1L);
    }

    @Test
    void deleteDatasetShouldReturnForbiddenWhenNotAdmin() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of(new SimpleGrantedAuthority("ROLE_user"))));

        mockMvc.perform(delete("/api/dataset/admin/1").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void deleteDatasetShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        setAdminAuthentication();
        when(datasetService.deleteDataset(1L)).thenReturn("not exists");

        mockMvc.perform(delete("/api/dataset/admin/1").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("not exists"));
    }

    @Test
    void updateDatasetShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        setAdminAuthentication();
        when(datasetService.updateDataset(1L, "Tiny-ImageNet")).thenReturn("name exists");

        mockMvc.perform(put("/api/dataset/admin/1")
                        .requestAttr("id", 1L)
                        .param("dataName", "Tiny-ImageNet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("name exists"));
    }

    @Test
    void updateDatasetShouldReturnSuccessWhenServiceReturnsNull() throws Exception {
        setAdminAuthentication();
        when(datasetService.updateDataset(1L, "Tiny-ImageNet")).thenReturn(null);

        mockMvc.perform(put("/api/dataset/admin/1")
                        .requestAttr("id", 1L)
                        .param("dataName", "Tiny-ImageNet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateDatasetShouldReturnForbiddenWhenNotAdmin() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of(new SimpleGrantedAuthority("ROLE_user"))));

        mockMvc.perform(put("/api/dataset/admin/1")
                        .requestAttr("id", 1L)
                        .param("dataName", "Tiny-ImageNet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void listDatasetsShouldReturnPageData() throws Exception {
        Page<DatasetVO> page = new Page<>(1, 10);
        page.setRecords(List.of(new DatasetVO()));
        when(datasetService.listDatasets(any(ListDatasetRO.class))).thenReturn(page);

        mockMvc.perform(get("/api/dataset/list").param("current", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());

        verify(datasetService).listDatasets(any(ListDatasetRO.class));
    }

    @Test
    void listDatasetsShouldReturnBadRequestWhenCurrentInvalid() throws Exception {
        mockMvc.perform(get("/api/dataset/list").param("current", "0"))
                .andExpect(status().isBadRequest());
    }

    private void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
    }
}
