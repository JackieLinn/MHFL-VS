package ynu.jackielinn.server.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 仪表盘模块接口集成测试。
 * 覆盖 Controller -> Service -> Mapper -> H2 链路。
 */
@Sql(
        scripts = {
                "classpath:integration/dashboard/schema.sql",
                "classpath:integration/dashboard/data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class DashboardIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpRedisMock() {
        RedisConnectionFactory redisConnectionFactory = org.mockito.Mockito.mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = org.mockito.Mockito.mock(RedisConnection.class);
        when(stringRedisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 成功场景：平台概览统计返回正确数据。
     */
    @Test
    void getPlatformStatsShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin/platform-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalUsers").value(2))
                .andExpect(jsonPath("$.data.totalTasks").value(5))
                .andExpect(jsonPath("$.data.totalDatasets").value(2))
                .andExpect(jsonPath("$.data.totalAlgorithms").value(2));
    }

    /**
     * 失败场景：数据为空时平台概览统计退化为 0。
     */
    @Test
    void getPlatformStatsShouldReturnZeroWhenNoData() throws Exception {
        jdbcTemplate.execute("DELETE FROM task");
        jdbcTemplate.execute("DELETE FROM algorithm");
        jdbcTemplate.execute("DELETE FROM dataset");
        jdbcTemplate.execute("DELETE FROM account");

        mockMvc.perform(get("/api/dashboard/admin/platform-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalUsers").value(0))
                .andExpect(jsonPath("$.data.totalTasks").value(0))
                .andExpect(jsonPath("$.data.totalDatasets").value(0))
                .andExpect(jsonPath("$.data.totalAlgorithms").value(0));
    }

    /**
     * 成功场景：按算法统计任务数成功。
     */
    @Test
    void getTasksByAlgorithmShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin/tasks-by-algorithm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.FedAvg").value(2))
                .andExpect(jsonPath("$.data.FedProto").value(3));
    }

    /**
     * 失败场景：无算法数据时返回空映射。
     */
    @Test
    void getTasksByAlgorithmShouldReturnEmptyWhenNoAlgorithm() throws Exception {
        jdbcTemplate.execute("DELETE FROM task");
        jdbcTemplate.execute("DELETE FROM algorithm");

        mockMvc.perform(get("/api/dashboard/admin/tasks-by-algorithm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.FedAvg").doesNotExist());
    }

    /**
     * 成功场景：任务状态统计成功。
     */
    @Test
    void getTaskStatusStatsShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/task-status-stats")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.notStarted").value(1))
                .andExpect(jsonPath("$.data.inProgress").value(1))
                .andExpect(jsonPath("$.data.completed").value(1))
                .andExpect(jsonPath("$.data.failed").value(1));
    }

    /**
     * 失败场景：未登录时返回 401。
     */
    @Test
    void getTaskStatusStatsShouldReturnFailureWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/dashboard/task-status-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 成功场景：任务趋势统计成功。
     */
    @Test
    void getTaskTrend7DaysShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/task-trend-7days")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dates").isArray())
                .andExpect(jsonPath("$.data.counts").isArray());
    }

    /**
     * 失败场景：未登录时返回 401。
     */
    @Test
    void getTaskTrend7DaysShouldReturnFailureWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/dashboard/task-trend-7days"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 成功场景：统计卡片数据成功。
     */
    @Test
    void getStatCardsShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/stat-cards")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.running").value(1))
                .andExpect(jsonPath("$.data.success").value(1));
    }

    /**
     * 失败场景：未登录时返回 401。
     */
    @Test
    void getStatCardsShouldReturnFailureWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/dashboard/stat-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 成功场景：最近任务查询成功。
     */
    @Test
    void getRecentTasksShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/recent-tasks")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * 失败场景：未登录时返回 401。
     */
    @Test
    void getRecentTasksShouldReturnFailureWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/dashboard/recent-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 成功场景：系统健康检查中 FastAPI 为 healthy。
     */
    @Test
    void getSystemHealthShouldReturnSuccess() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"status\":\"healthy\"}}"));

        mockMvc.perform(get("/api/dashboard/system-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fastapi").value(true));
    }

    /**
     * 失败场景：系统健康检查中 FastAPI 异常，状态为 false。
     */
    @Test
    void getSystemHealthShouldReturnFastApiFalseWhenUnhealthy() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":500,\"data\":{\"status\":\"unhealthy\"}}"));

        mockMvc.perform(get("/api/dashboard/system-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fastapi").value(false));
    }

    /**
     * 成功场景：管理员身份分支可正常查询状态统计。
     */
    @Test
    void getTaskStatusStatsShouldReturnSuccessForAdmin() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));

        mockMvc.perform(get("/api/dashboard/task-status-stats")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 失败场景：非管理员分支不应统计到管理员任务。
     */
    @Test
    void getTaskStatusStatsShouldReturnUserScopeWhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/dashboard/task-status-stats")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.completed").value(1));
    }
}
