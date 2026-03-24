package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import ynu.jackielinn.server.dto.response.DashboardPlatformStatsVO;
import ynu.jackielinn.server.dto.response.DashboardStatCardsVO;
import ynu.jackielinn.server.dto.response.DashboardSystemHealthVO;
import ynu.jackielinn.server.dto.response.DashboardTaskStatusStatsVO;
import ynu.jackielinn.server.dto.response.DashboardTaskTrendVO;
import ynu.jackielinn.server.dto.response.TaskVO;
import ynu.jackielinn.server.entity.Account;
import ynu.jackielinn.server.entity.Algorithm;
import ynu.jackielinn.server.entity.Dataset;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.AccountService;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.DatasetService;
import ynu.jackielinn.server.service.TaskService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @InjectMocks
    private DashboardServiceImpl service;

    @Mock
    private AccountService accountService;

    @Mock
    private TaskService taskService;

    @Mock
    private DatasetService datasetService;

    @Mock
    private AlgorithmService algorithmService;

    @Mock
    private DataSource dataSource;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ConnectionFactory rabbitConnectionFactory;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pythonFastApiUrl", "http://localhost:8000");
    }

    @Test
    void getPlatformStatsShouldMapCounts() {
        when(accountService.count()).thenReturn(3L);
        when(taskService.count()).thenReturn(10L);
        when(datasetService.count()).thenReturn(2L);
        when(algorithmService.count()).thenReturn(4L);

        DashboardPlatformStatsVO result = service.getPlatformStats();

        assertThat(result.getTotalUsers()).isEqualTo(3L);
        assertThat(result.getTotalTasks()).isEqualTo(10L);
        assertThat(result.getTotalDatasets()).isEqualTo(2L);
        assertThat(result.getTotalAlgorithms()).isEqualTo(4L);
    }

    @Test
    void getTasksByAlgorithmShouldReturnOrderedMap() {
        when(algorithmService.list()).thenReturn(List.of(
                Algorithm.builder().id(1L).algorithmName("FedAvg").build(),
                Algorithm.builder().id(2L).algorithmName("FedProto").build()
        ));
        when(taskService.count(any(LambdaQueryWrapper.class))).thenReturn(7L, 5L);

        Map<String, Long> result = service.getTasksByAlgorithm();

        assertThat(result).containsEntry("FedAvg", 7L).containsEntry("FedProto", 5L);
        assertThat(result.keySet()).containsExactly("FedAvg", "FedProto");
    }

    @Test
    void getTaskStatusStatsShouldReturnCountsForUserAndAdmin() {
        when(taskService.count(any(LambdaQueryWrapper.class))).thenReturn(1L, 2L, 3L, 4L, 9L, 8L, 7L, 6L);

        DashboardTaskStatusStatsVO user = service.getTaskStatusStats(1L, false);
        DashboardTaskStatusStatsVO admin = service.getTaskStatusStats(1L, true);

        assertThat(user.getNotStarted()).isEqualTo(1L);
        assertThat(user.getInProgress()).isEqualTo(2L);
        assertThat(user.getCompleted()).isEqualTo(3L);
        assertThat(user.getFailed()).isEqualTo(4L);
        assertThat(admin.getNotStarted()).isEqualTo(9L);
    }

    @Test
    void getTaskTrend7DaysShouldReturn7Points() {
        when(taskService.count(any(LambdaQueryWrapper.class))).thenReturn(1L, 2L, 3L, 4L, 5L, 6L, 7L);

        DashboardTaskTrendVO result = service.getTaskTrend7Days(1L, false);

        assertThat(result.getDates()).hasSize(7);
        assertThat(result.getCounts()).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L);
    }

    @Test
    void getStatCardsShouldReturnCounts() {
        when(taskService.count(any(LambdaQueryWrapper.class))).thenReturn(20L, 3L, 8L, 2L);

        DashboardStatCardsVO result = service.getStatCards(1L, false);

        assertThat(result.getTotal()).isEqualTo(20L);
        assertThat(result.getRunning()).isEqualTo(3L);
        assertThat(result.getSuccess()).isEqualTo(8L);
        assertThat(result.getToday()).isEqualTo(2L);
    }

    @Test
    void getRecentTasksShouldReturnEmptyWhenNoRecords() {
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<TaskVO> result = service.getRecentTasks(1L, false);

        assertThat(result).isEmpty();
        verify(datasetService, never()).listByIds(any());
    }

    @Test
    void getRecentTasksShouldReturnEmptyWhenRecordsNull() {
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(null);

        List<TaskVO> result = service.getRecentTasks(1L, false);

        assertThat(result).isEmpty();
        verify(datasetService, never()).listByIds(any());
    }

    @Test
    void getRecentTasksShouldMapNames() {
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                Task.builder()
                        .id(1L).uid(10L).did(100L).aid(200L)
                        .createTime(LocalDateTime.now())
                        .build()
        ));
        when(datasetService.listByIds(any())).thenReturn(List.of(Dataset.builder().id(100L).dataName("CIFAR-100").build()));
        when(algorithmService.listByIds(any())).thenReturn(List.of(Algorithm.builder().id(200L).algorithmName("FedAvg").build()));
        when(accountService.listByIds(any())).thenReturn(List.of(Account.builder().id(10L).username("alice").build()));

        List<TaskVO> result = service.getRecentTasks(1L, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDataName()).isEqualTo("CIFAR-100");
        assertThat(result.get(0).getAlgorithmName()).isEqualTo("FedAvg");
        assertThat(result.get(0).getUsername()).isEqualTo("alice");
    }

    @Test
    void getSystemHealthShouldReturnAllTrueWhenDependenciesHealthy() throws Exception {
        Connection conn = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.isValid(3)).thenReturn(true);

        RedisConnectionFactory redisFactory = mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = mock(RedisConnection.class);
        when(stringRedisTemplate.getConnectionFactory()).thenReturn(redisFactory);
        when(redisFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        org.springframework.amqp.rabbit.connection.Connection rabbitConn = mock(org.springframework.amqp.rabbit.connection.Connection.class);
        when(rabbitConnectionFactory.createConnection()).thenReturn(rabbitConn);

        when(restTemplate.exchange(eq("http://localhost:8000/api/health"), eq(org.springframework.http.HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":200,\"data\":{\"status\":\"healthy\"}}", HttpStatus.OK));

        DashboardSystemHealthVO result = service.getSystemHealth();

        assertThat(result.isMysql()).isTrue();
        assertThat(result.isRedis()).isTrue();
        assertThat(result.isRabbitmq()).isTrue();
        assertThat(result.isFastapi()).isTrue();
    }

    @Test
    void getSystemHealthShouldReturnAllFalseWhenDependenciesFail() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("db down"));
        when(stringRedisTemplate.getConnectionFactory()).thenThrow(new RuntimeException("redis down"));
        when(rabbitConnectionFactory.createConnection()).thenThrow(new RuntimeException("mq down"));
        when(restTemplate.exchange(eq("http://localhost:8000/api/health"), eq(org.springframework.http.HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RuntimeException("fastapi down"));

        DashboardSystemHealthVO result = service.getSystemHealth();

        assertThat(result.isMysql()).isFalse();
        assertThat(result.isRedis()).isFalse();
        assertThat(result.isRabbitmq()).isFalse();
        assertThat(result.isFastapi()).isFalse();
    }

    @Test
    void privateCheckFastapiShouldCoverResponseBranches() {
        when(restTemplate.exchange(eq("http://localhost:8000/api/health"), eq(org.springframework.http.HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":200}", HttpStatus.BAD_REQUEST),
                        new ResponseEntity<>("{\"message\":\"x\"}", HttpStatus.OK),
                        new ResponseEntity<>("{\"code\":500}", HttpStatus.OK),
                        new ResponseEntity<>((String) null, HttpStatus.OK),
                        new ResponseEntity<>("{\"code\":200,\"data\":null}", HttpStatus.OK),
                        new ResponseEntity<>("{\"code\":200,\"data\":{\"status\":\"down\"}}", HttpStatus.OK),
                        new ResponseEntity<>("{\"code\":200,\"data\":{\"status\":\"healthy\"}}", HttpStatus.OK));

        Boolean r1 = ReflectionTestUtils.invokeMethod(service, "checkFastapi");
        Boolean r2 = ReflectionTestUtils.invokeMethod(service, "checkFastapi");
        Boolean r3 = ReflectionTestUtils.invokeMethod(service, "checkFastapi");
        Boolean r4 = ReflectionTestUtils.invokeMethod(service, "checkFastapi");
        Boolean r5 = ReflectionTestUtils.invokeMethod(service, "checkFastapi");
        Boolean r6 = ReflectionTestUtils.invokeMethod(service, "checkFastapi");
        Boolean r7 = ReflectionTestUtils.invokeMethod(service, "checkFastapi");

        assertThat(r1).isFalse();
        assertThat(r2).isFalse();
        assertThat(r3).isFalse();
        assertThat(r4).isFalse();
        assertThat(r5).isFalse();
        assertThat(r6).isFalse();
        assertThat(r7).isTrue();
    }

    @Test
    void privateCheckRedisShouldThrowAssertionWhenConnectionFactoryNull() {
        when(stringRedisTemplate.getConnectionFactory()).thenReturn(null);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> ReflectionTestUtils.invokeMethod(service, "checkRedis")
        ).isInstanceOf(AssertionError.class);
    }
}
