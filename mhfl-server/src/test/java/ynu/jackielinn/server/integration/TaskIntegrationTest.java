package ynu.jackielinn.server.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.RedisSubscriptionService;
import ynu.jackielinn.server.service.TaskService;
import ynu.jackielinn.server.websocket.WebSocketSessionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 任务模块接口集成测试。
 * 覆盖 Controller -> Service -> Mapper -> H2 链路；中间件相关依赖均 mock。
 */
@Sql(
        scripts = {
                "classpath:integration/task/schema.sql",
                "classpath:integration/task/data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class TaskIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskService taskService;

    @MockitoBean
    private RedisSubscriptionService redisSubscriptionService;

    @MockitoBean
    private WebSocketSessionManager webSocketSessionManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTaskShouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/task")
                        .contentType("application/json")
                        .requestAttr("id", 2L)
                        .content("""
                                {
                                  "did": 1,
                                  "aid": 1,
                                  "numNodes": 5,
                                  "fraction": 0.2,
                                  "classesPerNode": 2,
                                  "lowProb": 0.1,
                                  "numSteps": 99,
                                  "epochs": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.copied").value(false));
    }

    @Test
    void createTaskShouldReturnFailureWhenDatasetNotExists() throws Exception {
        mockMvc.perform(post("/api/task")
                        .contentType("application/json")
                        .requestAttr("id", 2L)
                        .content("""
                                {
                                  "did": 999,
                                  "aid": 1,
                                  "numNodes": 5,
                                  "fraction": 0.2,
                                  "classesPerNode": 2,
                                  "lowProb": 0.1,
                                  "numSteps": 99,
                                  "epochs": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void listTasksShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/task/list")
                        .requestAttr("id", 2L)
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(4));
    }

    @Test
    void listTasksShouldReturnFailureWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/task/list")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getTaskDetailShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/task/3")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(3));
    }

    @Test
    void getTaskDetailShouldReturnFailureWhenNotExists() throws Exception {
        mockMvc.perform(get("/api/task/999")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void deleteTaskShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/task/1")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Task deleted = taskService.getById(1L);
        assertThat(deleted).isNull();
    }

    @Test
    void deleteTaskShouldReturnFailureWhenUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/task/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void setRecommendShouldReturnSuccess() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(put("/api/task/admin/3/recommend")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Task task = taskService.getById(3L);
        assertThat(task).isNotNull();
        assertThat(task.getStatus()).isEqualTo(Status.RECOMMENDED);
    }

    @Test
    void setRecommendShouldReturnFailureWhenNotAdmin() throws Exception {
        setUserAuthentication();

        mockMvc.perform(put("/api/task/admin/3/recommend")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void startTaskShouldReturnSuccess() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"message\":\"ok\"}"));

        mockMvc.perform(post("/api/task/1/start")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Task task = taskService.getById(1L);
        assertThat(task).isNotNull();
        assertThat(task.getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    void startTaskShouldReturnFailureWhenStatusNotAllowed() throws Exception {
        mockMvc.perform(post("/api/task/2/start")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void stopTaskShouldReturnSuccess() throws Exception {
        when(restTemplate.postForEntity(anyString(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"message\":\"ok\"}"));

        mockMvc.perform(post("/api/task/2/stop")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Task task = taskService.getById(2L);
        assertThat(task).isNotNull();
        assertThat(task.getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Test
    void stopTaskShouldReturnFailureWhenTaskNotInProgress() throws Exception {
        mockMvc.perform(post("/api/task/1/stop")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getTaskRoundsShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/task/3/rounds")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].roundNum").value(0));
    }

    @Test
    void getTaskRoundsShouldReturnFailureWhenNotExists() throws Exception {
        mockMvc.perform(get("/api/task/999/rounds")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getTaskClientsLatestShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/task/3/clients/latest")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].clientIndex").value(0));
    }

    @Test
    void getTaskClientsLatestShouldReturnFailureWhenNotExists() throws Exception {
        mockMvc.perform(get("/api/task/999/clients/latest")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getTaskClientDetailShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/task/3/clients/0")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].clientIndex").value(0));
    }

    @Test
    void getTaskClientDetailShouldReturnFailureWhenNotExists() throws Exception {
        mockMvc.perform(get("/api/task/999/clients/0")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    private void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_admin"))
                )
        );
    }

    private void setUserAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_user"))
                )
        );
    }
}
