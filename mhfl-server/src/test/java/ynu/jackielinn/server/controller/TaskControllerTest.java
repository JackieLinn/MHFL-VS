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
import ynu.jackielinn.server.dto.request.CreateTaskRO;
import ynu.jackielinn.server.dto.request.ListTaskRO;
import ynu.jackielinn.server.dto.response.ClientVO;
import ynu.jackielinn.server.dto.response.CreateTaskResultVO;
import ynu.jackielinn.server.dto.response.RoundVO;
import ynu.jackielinn.server.dto.response.TaskVO;
import ynu.jackielinn.server.service.TaskService;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TaskController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTaskShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(post("/api/task")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateTaskRO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void createTaskShouldReturnSuccess() throws Exception {
        when(taskService.createTask(any(CreateTaskRO.class), eq(1L)))
                .thenReturn(CreateTaskResultVO.builder().taskId(10L).copied(false).recommendedSameConfig(false).build());

        mockMvc.perform(post("/api/task")
                        .requestAttr("id", 1L)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateTaskRO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value(10));
    }

    @Test
    void createTaskShouldReturnBadRequestWhenServiceThrows() throws Exception {
        when(taskService.createTask(any(CreateTaskRO.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("same recommended config"));

        mockMvc.perform(post("/api/task")
                        .requestAttr("id", 1L)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateTaskRO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void listTasksShouldReturnSuccess() throws Exception {
        Page<TaskVO> page = new Page<>(1, 10);
        page.setRecords(List.of(new TaskVO()));
        when(taskService.listTasks(any(ListTaskRO.class), eq(1L), eq(false))).thenReturn(page);

        mockMvc.perform(get("/api/task/list").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void listTasksShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/task/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void listTasksShouldUseAdminFlagFromSecurityContext() throws Exception {
        setAdminAuthentication();
        Page<TaskVO> page = new Page<>(1, 10);
        page.setRecords(List.of(new TaskVO()));
        when(taskService.listTasks(any(ListTaskRO.class), eq(1L), eq(true))).thenReturn(page);

        mockMvc.perform(get("/api/task/list").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(taskService).listTasks(any(ListTaskRO.class), eq(1L), eq(true));
    }

    @Test
    void getTaskDetailShouldReturnNotFoundWhenServiceReturnsNull() throws Exception {
        when(taskService.getTaskDetail(1L, 1L, false)).thenReturn(null);

        mockMvc.perform(get("/api/task/1").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getTaskDetailShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/task/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getTaskDetailShouldReturnSuccess() throws Exception {
        when(taskService.getTaskDetail(1L, 1L, false)).thenReturn(new TaskVO());

        mockMvc.perform(get("/api/task/1").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteTaskShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        when(taskService.deleteTask(1L, 1L, false)).thenReturn("cannot delete");

        mockMvc.perform(delete("/api/task/1").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("cannot delete"));
    }

    @Test
    void deleteTaskShouldReturnSuccessWhenServiceReturnsNull() throws Exception {
        when(taskService.deleteTask(1L, 1L, false)).thenReturn(null);

        mockMvc.perform(delete("/api/task/1").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void setRecommendShouldReturnForbiddenWhenNotAdmin() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of(new SimpleGrantedAuthority("ROLE_user"))));

        mockMvc.perform(put("/api/task/admin/1/recommend").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void setRecommendShouldReturnSuccessForAdmin() throws Exception {
        setAdminAuthentication();
        when(taskService.setRecommend(1L)).thenReturn(null);

        mockMvc.perform(put("/api/task/admin/1/recommend").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void startTaskShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        when(taskService.startTask(1L, 1L)).thenReturn("already trained");

        mockMvc.perform(post("/api/task/1/start").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void startTaskShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(post("/api/task/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void startTaskShouldReturnSuccessWhenServiceReturnsNull() throws Exception {
        when(taskService.startTask(1L, 1L)).thenReturn(null);

        mockMvc.perform(post("/api/task/1/start").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void stopTaskShouldReturnSuccessWhenServiceReturnsNull() throws Exception {
        when(taskService.stopTask(1L, 1L)).thenReturn(null);

        mockMvc.perform(post("/api/task/1/stop").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void stopTaskShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(post("/api/task/1/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void stopTaskShouldReturnBadRequestWhenServiceReturnsMessage() throws Exception {
        when(taskService.stopTask(1L, 1L)).thenReturn("not running");

        mockMvc.perform(post("/api/task/1/stop").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getTaskRoundsShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/task/1/rounds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getTaskRoundsShouldReturnNotFoundWhenServiceReturnsNull() throws Exception {
        when(taskService.getTaskRounds(1L, 1L, false)).thenReturn(null);

        mockMvc.perform(get("/api/task/1/rounds").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getTaskRoundsShouldReturnSuccess() throws Exception {
        when(taskService.getTaskRounds(1L, 1L, false)).thenReturn(List.of(new RoundVO()));

        mockMvc.perform(get("/api/task/1/rounds").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getTaskClientsLatestShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/task/1/clients/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getTaskClientsLatestShouldReturnNotFoundWhenServiceReturnsNull() throws Exception {
        when(taskService.getTaskClientsLatest(1L, 1L, false)).thenReturn(null);

        mockMvc.perform(get("/api/task/1/clients/latest").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getTaskClientsLatestShouldReturnSuccess() throws Exception {
        when(taskService.getTaskClientsLatest(1L, 1L, false)).thenReturn(List.of(new ClientVO()));

        mockMvc.perform(get("/api/task/1/clients/latest").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getTaskClientDetailShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/task/1/clients/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getTaskClientDetailShouldReturnNotFoundWhenServiceReturnsNull() throws Exception {
        when(taskService.getTaskClientDetail(1L, 0, 1L, false)).thenReturn(null);

        mockMvc.perform(get("/api/task/1/clients/0").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getTaskClientDetailShouldReturnSuccess() throws Exception {
        when(taskService.getTaskClientDetail(1L, 0, 1L, false)).thenReturn(List.of(new ClientVO()));

        mockMvc.perform(get("/api/task/1/clients/0").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    private CreateTaskRO validCreateTaskRO() {
        return CreateTaskRO.builder()
                .did(1L)
                .aid(2L)
                .numNodes(100)
                .fraction(0.1)
                .classesPerNode(5)
                .lowProb(0.2)
                .numSteps(10)
                .epochs(1)
                .build();
    }

    private void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
    }
}
