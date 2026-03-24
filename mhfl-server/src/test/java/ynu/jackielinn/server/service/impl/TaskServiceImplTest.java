package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.dto.request.CreateTaskRO;
import ynu.jackielinn.server.dto.request.ListTaskRO;
import ynu.jackielinn.server.dto.response.ClientVO;
import ynu.jackielinn.server.dto.response.CreateTaskResultVO;
import ynu.jackielinn.server.dto.response.RoundVO;
import ynu.jackielinn.server.dto.response.TaskVO;
import ynu.jackielinn.server.entity.Account;
import ynu.jackielinn.server.entity.Algorithm;
import ynu.jackielinn.server.entity.Client;
import ynu.jackielinn.server.entity.Dataset;
import ynu.jackielinn.server.entity.Round;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.mapper.TaskMapper;
import ynu.jackielinn.server.service.AccountService;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.ClientService;
import ynu.jackielinn.server.service.DatasetService;
import ynu.jackielinn.server.service.RedisSubscriptionService;
import ynu.jackielinn.server.service.RoundService;
import ynu.jackielinn.server.websocket.WebSocketSessionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Spy
    @InjectMocks
    private TaskServiceImpl service;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private DatasetService datasetService;

    @Mock
    private AlgorithmService algorithmService;

    @Mock
    private AccountService accountService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private RoundService roundService;

    @Mock
    private ClientService clientService;

    @Mock
    private RedisSubscriptionService redisSubscriptionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", taskMapper);
        ReflectionTestUtils.setField(service, "pythonFastApiUrl", "http://localhost:8000");
    }

    @Test
    void createTaskShouldThrowWhenDatasetMissing() {
        CreateTaskRO ro = buildCreateTaskRO();
        when(datasetService.getById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.createTask(ro, 7L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createTaskShouldThrowWhenAlgorithmMissing() {
        CreateTaskRO ro = buildCreateTaskRO();
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());
        when(algorithmService.getById(2L)).thenReturn(null);

        assertThatThrownBy(() -> service.createTask(ro, 7L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createTaskShouldThrowWhenRecommendedConfigExists() {
        CreateTaskRO ro = buildCreateTaskRO();
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).build());
        LambdaQueryChainWrapper<Task> chain1 = mock(LambdaQueryChainWrapper.class);
        doReturn(chain1).when(service).lambdaQuery();
        when(chain1.eq(anyTaskFn(), any())).thenReturn(chain1);
        when(chain1.last(anyString())).thenReturn(chain1);
        when(chain1.one()).thenReturn(Task.builder().id(9L).uid(100L).status(Status.RECOMMENDED).build());

        assertThatThrownBy(() -> service.createTask(ro, 7L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createTaskShouldCopyFromLatestSuccess() {
        CreateTaskRO ro = buildCreateTaskRO();
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).build());

        LambdaQueryChainWrapper<Task> recommendedQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<Task> sourceQuery = mock(LambdaQueryChainWrapper.class);
        doReturn(recommendedQuery, sourceQuery).when(service).lambdaQuery();

        when(recommendedQuery.eq(anyTaskFn(), any())).thenReturn(recommendedQuery);
        when(recommendedQuery.last(anyString())).thenReturn(recommendedQuery);
        when(recommendedQuery.one()).thenReturn(null);

        Task source = Task.builder()
                .id(50L)
                .uid(99L)
                .did(1L)
                .aid(2L)
                .numNodes(3)
                .fraction(0.1)
                .classesPerNode(5)
                .lowProb(0.2)
                .numSteps(10)
                .epochs(2)
                .status(Status.SUCCESS)
                .accuracy(0.8)
                .loss(1.2)
                .precision(0.7)
                .recall(0.6)
                .f1Score(0.65)
                .build();
        when(sourceQuery.eq(anyTaskFn(), any())).thenReturn(sourceQuery);
        doReturn(sourceQuery).when(sourceQuery).orderByDesc(org.mockito.ArgumentMatchers.<SFunction<Task, ?>>any());
        when(sourceQuery.last(anyString())).thenReturn(sourceQuery);
        when(sourceQuery.one()).thenReturn(source);

        doAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(100L);
            return true;
        }).when(service).save(any(Task.class));

        List<Round> sourceRounds = List.of(
                Round.builder().id(1L).tid(50L).roundNum(1).accuracy(0.61).build(),
                Round.builder().id(2L).tid(50L).roundNum(2).accuracy(0.72).build()
        );
        when(roundService.listByTidOrderByRoundNum(50L)).thenReturn(sourceRounds);

        AtomicLong roundId = new AtomicLong(1000L);
        doAnswer(invocation -> {
            Round r = invocation.getArgument(0);
            r.setId(roundId.getAndIncrement());
            return null;
        }).when(roundService).saveRound(any(Round.class));

        when(clientService.listByRidIn(List.of(1L, 2L))).thenReturn(List.of(
                Client.builder().rid(1L).clientIndex(0).accuracy(0.5).timestamp(LocalDateTime.now()).build(),
                Client.builder().rid(null).clientIndex(2).accuracy(0.3).timestamp(LocalDateTime.now()).build(),
                Client.builder().rid(999L).clientIndex(1).accuracy(0.4).timestamp(LocalDateTime.now()).build()
        ));

        CreateTaskResultVO result = service.createTask(ro, 7L);

        assertThat(result.getCopied()).isTrue();
        assertThat(result.getTaskId()).isEqualTo(100L);
        verify(clientService, times(1)).saveClient(any(Client.class));
    }

    @Test
    void createTaskShouldThrowWhenRecommendedConfigExistsAndUidNull() {
        CreateTaskRO ro = buildCreateTaskRO();
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).build());
        LambdaQueryChainWrapper<Task> chain = mock(LambdaQueryChainWrapper.class);
        doReturn(chain).when(service).lambdaQuery();
        when(chain.eq(anyTaskFn(), any())).thenReturn(chain);
        when(chain.last(anyString())).thenReturn(chain);
        when(chain.one()).thenReturn(Task.builder().id(9L).uid(null).status(Status.RECOMMENDED).build());

        assertThatThrownBy(() -> service.createTask(ro, 7L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createTaskShouldCreateNewTaskWhenNoRecommendedAndNoSuccess() {
        CreateTaskRO ro = buildCreateTaskRO();
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).build());

        LambdaQueryChainWrapper<Task> recommendedQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<Task> sourceQuery = mock(LambdaQueryChainWrapper.class);
        doReturn(recommendedQuery, sourceQuery).when(service).lambdaQuery();
        when(recommendedQuery.eq(anyTaskFn(), any())).thenReturn(recommendedQuery);
        when(recommendedQuery.last(anyString())).thenReturn(recommendedQuery);
        when(recommendedQuery.one()).thenReturn(null);
        when(sourceQuery.eq(anyTaskFn(), any())).thenReturn(sourceQuery);
        doReturn(sourceQuery).when(sourceQuery).orderByDesc(org.mockito.ArgumentMatchers.<SFunction<Task, ?>>any());
        when(sourceQuery.last(anyString())).thenReturn(sourceQuery);
        when(sourceQuery.one()).thenReturn(null);

        doAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(101L);
            return true;
        }).when(service).save(any(Task.class));

        CreateTaskResultVO result = service.createTask(ro, 8L);

        assertThat(result.getCopied()).isFalse();
        assertThat(result.getTaskId()).isEqualTo(101L);
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(service).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Status.NOT_STARTED);
    }

    @Test
    void deleteTaskShouldCoverMainBranches() {
        doReturn(null).when(service).getById(1L);
        assertThat(service.deleteTask(1L, 2L, false)).isNotNull();

        doReturn(Task.builder().id(1L).uid(2L).status(Status.IN_PROGRESS).build()).when(service).getById(2L);
        assertThat(service.deleteTask(2L, 2L, false)).isNotNull();

        doReturn(Task.builder().id(1L).uid(2L).status(Status.RECOMMENDED).build()).when(service).getById(3L);
        assertThat(service.deleteTask(3L, 2L, false)).isNotNull();

        doReturn(Task.builder().id(1L).uid(9L).status(Status.SUCCESS).build()).when(service).getById(4L);
        assertThat(service.deleteTask(4L, 2L, false)).isNotNull();

        doReturn(Task.builder().id(1L).uid(2L).status(Status.SUCCESS).build()).when(service).getById(5L);
        doReturn(false, true).when(service).update(any(LambdaUpdateWrapper.class));
        assertThat(service.deleteTask(5L, 2L, false)).isNotNull();
        assertThat(service.deleteTask(5L, 2L, false)).isNull();
    }

    @Test
    void deleteTaskShouldAllowAdminDeleteOthers() {
        doReturn(Task.builder().id(6L).uid(99L).status(Status.SUCCESS).build()).when(service).getById(6L);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));

        String result = service.deleteTask(6L, 2L, true);

        assertThat(result).isNull();
    }

    @Test
    void createTaskShouldThrowWhenRecommendedTaskBelongsToCurrentUser() {
        CreateTaskRO ro = buildCreateTaskRO();
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).build());
        LambdaQueryChainWrapper<Task> chain = mock(LambdaQueryChainWrapper.class);
        doReturn(chain).when(service).lambdaQuery();
        when(chain.eq(anyTaskFn(), any())).thenReturn(chain);
        when(chain.last(anyString())).thenReturn(chain);
        when(chain.one()).thenReturn(Task.builder().id(10L).uid(7L).status(Status.RECOMMENDED).build());

        assertThatThrownBy(() -> service.createTask(ro, 7L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createTaskShouldThrowWhenSuccessTaskBelongsToCurrentUser() {
        CreateTaskRO ro = buildCreateTaskRO();
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).build());
        LambdaQueryChainWrapper<Task> recommendedQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<Task> sourceQuery = mock(LambdaQueryChainWrapper.class);
        doReturn(recommendedQuery, sourceQuery).when(service).lambdaQuery();
        when(recommendedQuery.eq(anyTaskFn(), any())).thenReturn(recommendedQuery);
        when(recommendedQuery.last(anyString())).thenReturn(recommendedQuery);
        when(recommendedQuery.one()).thenReturn(null);
        when(sourceQuery.eq(anyTaskFn(), any())).thenReturn(sourceQuery);
        doReturn(sourceQuery).when(sourceQuery).orderByDesc(org.mockito.ArgumentMatchers.<SFunction<Task, ?>>any());
        when(sourceQuery.last(anyString())).thenReturn(sourceQuery);
        when(sourceQuery.one()).thenReturn(Task.builder().id(50L).uid(7L).status(Status.SUCCESS).build());

        assertThatThrownBy(() -> service.createTask(ro, 7L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createTaskShouldCopyWhenSuccessTaskUidIsNull() {
        CreateTaskRO ro = buildCreateTaskRO();
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).build());
        LambdaQueryChainWrapper<Task> recommendedQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<Task> sourceQuery = mock(LambdaQueryChainWrapper.class);
        doReturn(recommendedQuery, sourceQuery).when(service).lambdaQuery();
        when(recommendedQuery.eq(anyTaskFn(), any())).thenReturn(recommendedQuery);
        when(recommendedQuery.last(anyString())).thenReturn(recommendedQuery);
        when(recommendedQuery.one()).thenReturn(null);
        when(sourceQuery.eq(anyTaskFn(), any())).thenReturn(sourceQuery);
        doReturn(sourceQuery).when(sourceQuery).orderByDesc(org.mockito.ArgumentMatchers.<SFunction<Task, ?>>any());
        when(sourceQuery.last(anyString())).thenReturn(sourceQuery);
        when(sourceQuery.one()).thenReturn(Task.builder()
                .id(60L).uid(null).did(1L).aid(2L)
                .numNodes(10).fraction(0.1).classesPerNode(5).lowProb(0.2).numSteps(10).epochs(2)
                .status(Status.SUCCESS).build());
        doAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(160L);
            return true;
        }).when(service).save(any(Task.class));
        when(roundService.listByTidOrderByRoundNum(60L)).thenReturn(List.of());
        when(clientService.listByRidIn(List.of())).thenReturn(List.of());

        CreateTaskResultVO result = service.createTask(ro, 7L);

        assertThat(result.getCopied()).isTrue();
        assertThat(result.getTaskId()).isEqualTo(160L);
    }

    @Test
    void setRecommendShouldCoverMainBranches() {
        doReturn(null).when(service).getById(1L);
        assertThat(service.setRecommend(1L)).isNotNull();

        doReturn(Task.builder().id(2L).status(Status.NOT_STARTED).build()).when(service).getById(2L);
        assertThat(service.setRecommend(2L)).isNotNull();

        doReturn(Task.builder().id(3L).did(1L).aid(2L).numNodes(10).fraction(0.1).classesPerNode(5).lowProb(0.2).numSteps(20).epochs(2).status(Status.SUCCESS).build())
                .when(service).getById(3L);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));
        assertThat(service.setRecommend(3L)).isNull();

        doReturn(Task.builder().id(4L).did(1L).aid(2L).numNodes(10).fraction(0.1).classesPerNode(5).lowProb(0.2).numSteps(20).epochs(2).status(Status.RECOMMENDED).build())
                .when(service).getById(4L);
        doReturn(false).when(service).update(any(LambdaUpdateWrapper.class));
        assertThat(service.setRecommend(4L)).isNotNull();
    }

    @Test
    void listTasksShouldReturnMappedPage() {
        Task task = Task.builder().id(10L).uid(7L).did(1L).aid(2L).status(Status.SUCCESS).build();
        IPage<Task> pageResult = new Page<>(1, 10, 1);
        pageResult.setRecords(List.of(task));
        doReturn(pageResult).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));
        when(datasetService.listByIds(any())).thenReturn(List.of(Dataset.builder().id(1L).dataName("CIFAR-100").build()));
        when(algorithmService.listByIds(any())).thenReturn(List.of(Algorithm.builder().id(2L).algorithmName("FedAvg").build()));
        when(accountService.listByIds(any())).thenReturn(List.of(Account.builder().id(7L).username("alice").build()));

        IPage<TaskVO> result = service.listTasks(ListTaskRO.builder().current(1L).size(10L).build(), 7L, false);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getDataName()).isEqualTo("CIFAR-100");
        assertThat(result.getRecords().get(0).getAlgorithmName()).isEqualTo("FedAvg");
        assertThat(result.getRecords().get(0).getUsername()).isEqualTo("alice");
    }

    @Test
    void listTasksShouldHandleKeywordAndTimeRangeBranches() {
        IPage<Task> pageResult = new Page<>(2, 5, 0);
        pageResult.setRecords(List.of());
        doReturn(pageResult).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));
        when(datasetService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(algorithmService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(accountService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        IPage<TaskVO> result = service.listTasks(
                ListTaskRO.builder()
                        .keyword("none")
                        .current(2L)
                        .size(5L)
                        .startTime(LocalDate.now().minusDays(1))
                        .endTime(LocalDate.now())
                        .createTimeSort("asc")
                        .build(),
                1L,
                true
        );

        assertThat(result.getCurrent()).isEqualTo(2L);
        assertThat(result.getSize()).isEqualTo(5L);
        assertThat(result.getTotal()).isEqualTo(0L);
    }

    @Test
    void listTasksShouldCoverKeywordOrBranchesForAdminAndNonAdmin() {
        IPage<Task> pageResult = new Page<>(1, 10, 0);
        pageResult.setRecords(List.of());
        doReturn(pageResult).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        when(datasetService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(Dataset.builder().id(1L).build()));
        when(algorithmService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(Algorithm.builder().id(2L).build()));
        when(accountService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(Account.builder().id(3L).build()));

        IPage<TaskVO> adminResult = service.listTasks(ListTaskRO.builder().keyword("k").build(), 1L, true);
        IPage<TaskVO> userResult = service.listTasks(ListTaskRO.builder().keyword("k").build(), 1L, false);

        assertThat(adminResult.getTotal()).isEqualTo(0L);
        assertThat(userResult.getTotal()).isEqualTo(0L);
    }

    @Test
    void listTasksShouldCoverDefaultPageAndSingleTimeBoundaryBranches() {
        IPage<Task> pageResult = new Page<>(1, 10, 0);
        pageResult.setRecords(null);
        doReturn(pageResult).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        ListTaskRO onlyStart = new ListTaskRO();
        onlyStart.setStartTime(LocalDate.now().minusDays(2));
        onlyStart.setKeyword("   ");
        onlyStart.setUpdateTimeSort("DESC");
        IPage<TaskVO> r1 = service.listTasks(onlyStart, 1L, false);

        ListTaskRO onlyEnd = new ListTaskRO();
        onlyEnd.setEndTime(LocalDate.now());
        IPage<TaskVO> r2 = service.listTasks(onlyEnd, 1L, false);

        assertThat(r1.getCurrent()).isEqualTo(1L);
        assertThat(r1.getSize()).isEqualTo(10L);
        assertThat(r2.getTotal()).isEqualTo(0L);
    }

    @Test
    void getTaskDetailShouldCoverPermissionBranches() {
        doReturn(null).when(service).getById(1L);
        assertThat(service.getTaskDetail(1L, 7L, false)).isNull();

        doReturn(Task.builder().id(2L).uid(8L).status(Status.SUCCESS).build()).when(service).getById(2L);
        assertThat(service.getTaskDetail(2L, 7L, false)).isNull();

        Task allowed = Task.builder().id(3L).uid(7L).did(1L).aid(2L).status(Status.SUCCESS).build();
        doReturn(allowed).when(service).getById(3L);
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("Tiny-ImageNet").build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).algorithmName("FedProto").build());
        when(accountService.getById(7L)).thenReturn(Account.builder().id(7L).username("bob").build());

        TaskVO result = service.getTaskDetail(3L, 7L, false);

        assertThat(result).isNotNull();
        assertThat(result.getDataName()).isEqualTo("Tiny-ImageNet");
        assertThat(result.getAlgorithmName()).isEqualTo("FedProto");
        assertThat(result.getUsername()).isEqualTo("bob");
    }

    @Test
    void getTaskDetailShouldAllowRecommendedAndAdminAndHandleNullNames() {
        Task recommended = Task.builder().id(13L).uid(99L).did(1L).aid(2L).status(Status.RECOMMENDED).build();
        doReturn(recommended).when(service).getById(13L);
        when(datasetService.getById(1L)).thenReturn(null);
        when(algorithmService.getById(2L)).thenReturn(null);
        when(accountService.getById(99L)).thenReturn(null);

        TaskVO r1 = service.getTaskDetail(13L, 7L, false);
        TaskVO r2 = service.getTaskDetail(13L, 7L, true);

        assertThat(r1).isNotNull();
        assertThat(r1.getDataName()).isNull();
        assertThat(r2).isNotNull();
    }

    @Test
    void startTaskShouldCoverValidationAndCallBranches() {
        doReturn(null).when(service).getById(1L);
        assertThat(service.startTask(1L, 7L)).isNotNull();

        doReturn(Task.builder().id(2L).uid(8L).status(Status.NOT_STARTED).build()).when(service).getById(2L);
        assertThat(service.startTask(2L, 7L)).isNotNull();

        doReturn(Task.builder().id(3L).uid(7L).status(Status.IN_PROGRESS).build()).when(service).getById(3L);
        assertThat(service.startTask(3L, 7L)).isNotNull();

        doReturn(Task.builder().id(4L).uid(7L).status(Status.SUCCESS).build()).when(service).getById(4L);
        assertThat(service.startTask(4L, 7L)).isNotNull();

        Task t5 = Task.builder().id(5L).uid(7L).did(1L).aid(2L).status(Status.NOT_STARTED).build();
        doReturn(t5).when(service).getById(5L);
        when(datasetService.getById(1L)).thenReturn(null);
        assertThat(service.startTask(5L, 7L)).isNotNull();

        Task t6 = Task.builder().id(6L).uid(7L).did(1L).aid(2L).status(Status.NOT_STARTED).build();
        doReturn(t6).when(service).getById(6L);
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());
        when(algorithmService.getById(2L)).thenReturn(null);
        assertThat(service.startTask(6L, 7L)).isNotNull();
    }

    @Test
    void startTaskShouldReturnErrorWhenFastApiResponseIsNotOk() {
        Task task = Task.builder()
                .id(7L)
                .uid(7L)
                .did(1L)
                .aid(2L)
                .numNodes(10)
                .fraction(0.1)
                .classesPerNode(5)
                .lowProb(0.2)
                .numSteps(30)
                .epochs(2)
                .status(Status.NOT_STARTED)
                .build();
        doReturn(task).when(service).getById(7L);
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).algorithmName("FedAvg").build());
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/train/start"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":500,\"message\":\"err\"}", HttpStatus.OK));

        String result = service.startTask(7L, 7L);

        assertThat(result).isEqualTo("err");
        verify(service, never()).updateById(any(Task.class));
    }

    @Test
    void startTaskShouldReturnGenericMessageWhenCodeNot200AndMessageNull() {
        Task task = Task.builder()
                .id(70L)
                .uid(7L)
                .did(1L)
                .aid(2L)
                .numNodes(10)
                .fraction(0.1)
                .classesPerNode(5)
                .lowProb(0.2)
                .numSteps(30)
                .epochs(2)
                .status(Status.NOT_STARTED)
                .build();
        doReturn(task).when(service).getById(70L);
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).algorithmName("FedAvg").build());
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/train/start"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":500}", HttpStatus.OK));

        String result = service.startTask(70L, 7L);

        assertThat(result).isEqualTo("启动训练失败");
    }

    @Test
    void startTaskShouldHandleNon2xxAndNullBody() {
        Task task = Task.builder()
                .id(71L)
                .uid(7L)
                .did(1L)
                .aid(2L)
                .numNodes(10)
                .fraction(0.1)
                .classesPerNode(5)
                .lowProb(0.2)
                .numSteps(30)
                .epochs(2)
                .status(Status.NOT_STARTED)
                .build();
        doReturn(task).when(service).getById(71L);
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).algorithmName("FedAvg").build());
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/train/start"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>((String) null, HttpStatus.BAD_REQUEST),
                        new ResponseEntity<>((String) null, HttpStatus.OK));

        String r1 = service.startTask(71L, 7L);
        String r2 = service.startTask(71L, 7L);

        assertThat(r1).contains("启动训练失败");
        assertThat(r2).contains("启动训练失败");
    }

    @Test
    void startTaskShouldSucceedAndSubscribe() {
        Task task = Task.builder()
                .id(8L)
                .uid(7L)
                .did(1L)
                .aid(2L)
                .numNodes(10)
                .fraction(0.1)
                .classesPerNode(5)
                .lowProb(0.2)
                .numSteps(30)
                .epochs(2)
                .status(Status.NOT_STARTED)
                .build();
        doReturn(task).when(service).getById(8L);
        doReturn(true).when(service).updateById(any(Task.class));
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).algorithmName("FedAvg").build());
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/train/start"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":200,\"data\":{\"started\":true}}", HttpStatus.OK));
        when(applicationContext.getBean(RedisSubscriptionService.class)).thenReturn(redisSubscriptionService);

        String result = service.startTask(8L, 7L);

        assertThat(result).isNull();
        assertThat(task.getStatus()).isEqualTo(Status.IN_PROGRESS);
        verify(redisSubscriptionService).subscribeTask(8L);
    }

    @Test
    void stopTaskShouldCoverValidationAndCallBranches() {
        doReturn(null).when(service).getById(1L);
        assertThat(service.stopTask(1L, 7L)).isNotNull();

        doReturn(Task.builder().id(2L).uid(8L).status(Status.IN_PROGRESS).build()).when(service).getById(2L);
        assertThat(service.stopTask(2L, 7L)).isNotNull();

        doReturn(Task.builder().id(3L).uid(7L).status(Status.SUCCESS).build()).when(service).getById(3L);
        assertThat(service.stopTask(3L, 7L)).isNotNull();
    }

    @Test
    void stopTaskShouldHandleFastApiErrorsAndSuccess() {
        Task running = Task.builder().id(9L).uid(7L).status(Status.IN_PROGRESS).build();
        doReturn(running).when(service).getById(9L);
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/train/stop/9"), eq(null), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":500,\"message\":\"stop-failed\"}", HttpStatus.OK),
                        new ResponseEntity<>("{\"code\":200,\"data\":{\"stopped\":true}}", HttpStatus.OK));
        doReturn(true).when(service).updateById(any(Task.class));

        String fail = service.stopTask(9L, 7L);
        String ok = service.stopTask(9L, 7L);

        assertThat(fail).isEqualTo("stop-failed");
        assertThat(ok).isNull();
        assertThat(running.getStatus()).isEqualTo(Status.CANCELLED);
        verify(sessionManager).sendToTask(eq(9L), any());
        verify(sessionManager).closeAllSessionsForTask(9L);
    }

    @Test
    void stopTaskShouldHandleNon2xxAndNullBodyAndFallbackMessage() {
        Task running = Task.builder().id(90L).uid(7L).status(Status.IN_PROGRESS).build();
        doReturn(running).when(service).getById(90L);
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/train/stop/90"), eq(null), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":200}", HttpStatus.BAD_REQUEST),
                        new ResponseEntity<>((String) null, HttpStatus.OK),
                        new ResponseEntity<>("{\"code\":500}", HttpStatus.OK));

        String r1 = service.stopTask(90L, 7L);
        String r2 = service.stopTask(90L, 7L);
        String r3 = service.stopTask(90L, 7L);

        assertThat(r1).contains("停止训练失败");
        assertThat(r2).contains("停止训练失败");
        assertThat(r3).isEqualTo("任务未在运行或停止失败");
    }

    @Test
    void getTaskRoundsShouldCoverPermissionAndMapping() {
        doReturn(null).when(service).getById(1L);
        assertThat(service.getTaskRounds(1L, 7L, false)).isNull();

        doReturn(Task.builder().id(2L).uid(8L).status(Status.SUCCESS).build()).when(service).getById(2L);
        assertThat(service.getTaskRounds(2L, 7L, false)).isNull();

        doReturn(Task.builder().id(3L).uid(7L).status(Status.SUCCESS).build()).when(service).getById(3L);
        when(roundService.listByTidOrderByRoundNum(3L)).thenReturn(List.of(
                Round.builder().id(1L).tid(3L).roundNum(1).accuracy(0.5).build(),
                Round.builder().id(2L).tid(3L).roundNum(2).accuracy(0.6).build()
        ));

        List<RoundVO> result = service.getTaskRounds(3L, 7L, false);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRoundNum()).isEqualTo(1);
    }

    @Test
    void getTaskRoundsShouldAllowAdminAndRecommended() {
        doReturn(Task.builder().id(30L).uid(8L).status(Status.RECOMMENDED).build()).when(service).getById(30L);
        when(roundService.listByTidOrderByRoundNum(30L)).thenReturn(Collections.emptyList());
        assertThat(service.getTaskRounds(30L, 7L, false)).isNotNull();
        assertThat(service.getTaskRounds(30L, 7L, true)).isNotNull();
    }

    @Test
    void getTaskClientsLatestShouldCoverPermissionAndPlaceholderBranches() {
        doReturn(null).when(service).getById(1L);
        assertThat(service.getTaskClientsLatest(1L, 7L, false)).isNull();

        doReturn(Task.builder().id(2L).uid(8L).status(Status.SUCCESS).build()).when(service).getById(2L);
        assertThat(service.getTaskClientsLatest(2L, 7L, false)).isNull();

        doReturn(Task.builder().id(3L).uid(7L).numNodes(3).status(Status.SUCCESS).build()).when(service).getById(3L);
        when(roundService.listByTidOrderByRoundNum(3L)).thenReturn(List.of(
                Round.builder().id(11L).roundNum(1).build(),
                Round.builder().id(12L).roundNum(2).build()
        ));
        when(clientService.getLatestByRidsAndClientIndex(List.of(11L, 12L), 0)).thenReturn(null);
        when(clientService.getLatestByRidsAndClientIndex(List.of(11L, 12L), 1))
                .thenReturn(Client.builder().id(99L).rid(12L).clientIndex(1).accuracy(0.88).build());
        when(clientService.getLatestByRidsAndClientIndex(List.of(11L, 12L), 2)).thenReturn(null);

        List<ClientVO> result = service.getTaskClientsLatest(3L, 7L, false);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getRoundNum()).isEqualTo(-1);
        assertThat(result.get(1).getRoundNum()).isEqualTo(2);
        assertThat(result.get(2).getRoundNum()).isEqualTo(-1);
    }

    @Test
    void getTaskClientsLatestShouldCoverNullNumNodesAndNullRid() {
        doReturn(Task.builder().id(31L).uid(7L).numNodes(null).status(Status.SUCCESS).build()).when(service).getById(31L);
        when(roundService.listByTidOrderByRoundNum(31L)).thenReturn(List.of(
                Round.builder().id(21L).roundNum(1).build()
        ));
        List<ClientVO> empty = service.getTaskClientsLatest(31L, 7L, false);
        assertThat(empty).isEmpty();

        doReturn(Task.builder().id(32L).uid(7L).numNodes(1).status(Status.SUCCESS).build()).when(service).getById(32L);
        when(roundService.listByTidOrderByRoundNum(32L)).thenReturn(List.of(
                Round.builder().id(22L).roundNum(2).build()
        ));
        when(clientService.getLatestByRidsAndClientIndex(List.of(22L), 0))
                .thenReturn(Client.builder().id(100L).rid(null).clientIndex(0).build());
        List<ClientVO> one = service.getTaskClientsLatest(32L, 7L, false);
        assertThat(one).hasSize(1);
        assertThat(one.get(0).getRoundNum()).isEqualTo(-1);
    }

    @Test
    void getTaskClientDetailShouldCoverPermissionAndSorting() {
        doReturn(null).when(service).getById(1L);
        assertThat(service.getTaskClientDetail(1L, 1, 7L, false)).isNull();

        doReturn(Task.builder().id(2L).uid(8L).status(Status.SUCCESS).build()).when(service).getById(2L);
        assertThat(service.getTaskClientDetail(2L, 1, 7L, false)).isNull();

        doReturn(Task.builder().id(3L).uid(7L).status(Status.SUCCESS).build()).when(service).getById(3L);
        when(roundService.listByTidOrderByRoundNum(3L)).thenReturn(List.of(
                Round.builder().id(11L).roundNum(1).build(),
                Round.builder().id(12L).roundNum(2).build()
        ));
        when(clientService.listByRidsAndClientIndex(List.of(11L, 12L), 1)).thenReturn(List.of(
                Client.builder().id(1L).rid(12L).clientIndex(1).build(),
                Client.builder().id(2L).rid(999L).clientIndex(1).build(),
                Client.builder().id(3L).rid(11L).clientIndex(1).build()
        ));

        List<ClientVO> result = service.getTaskClientDetail(3L, 1, 7L, false);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ClientVO::getRoundNum).containsExactly(-1, 1, 2);
    }

    @Test
    void getTaskClientDetailShouldCoverComparatorNullRoundNumBranches() {
        doReturn(Task.builder().id(33L).uid(7L).status(Status.SUCCESS).build()).when(service).getById(33L);
        when(roundService.listByTidOrderByRoundNum(33L)).thenReturn(List.of(
                Round.builder().id(31L).roundNum(1).build()
        ));

        Client c1 = mock(Client.class);
        when(c1.getRid()).thenReturn(31L);
        when(c1.asViewObject(eq(ClientVO.class), any(Consumer.class)))
                .thenReturn(ClientVO.builder().clientIndex(1).roundNum(null).build());

        Client c2 = mock(Client.class);
        when(c2.getRid()).thenReturn(31L);
        when(c2.asViewObject(eq(ClientVO.class), any(Consumer.class)))
                .thenReturn(ClientVO.builder().clientIndex(2).roundNum(1).build());

        when(clientService.listByRidsAndClientIndex(List.of(31L), 1)).thenReturn(List.of(c1, c2));

        List<ClientVO> result = service.getTaskClientDetail(33L, 1, 7L, false);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRoundNum()).isNull();
    }

    @Test
    void listTasksShouldCoverSortFallbacks() {
        IPage<Task> pageResult = new Page<>(1, 10, 0);
        pageResult.setRecords(List.of());
        doReturn(pageResult).when(service).page(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<TaskVO> r1 = service.listTasks(ListTaskRO.builder().createTimeSort("DESC").build(), 1L, false);
        IPage<TaskVO> r2 = service.listTasks(ListTaskRO.builder().createTimeSort("DEFAULT").updateTimeSort("ASC").build(), 1L, false);
        IPage<TaskVO> r3 = service.listTasks(ListTaskRO.builder().createTimeSort("invalid").updateTimeSort("invalid").build(), 1L, false);

        assertThat(r1.getTotal()).isEqualTo(0L);
        assertThat(r2.getTotal()).isEqualTo(0L);
        assertThat(r3.getTotal()).isEqualTo(0L);
    }

    @Test
    void startTaskShouldReturnExceptionMessageWhenRestThrows() {
        Task task = Task.builder()
                .id(11L)
                .uid(7L)
                .did(1L)
                .aid(2L)
                .numNodes(10)
                .fraction(0.1)
                .classesPerNode(5)
                .lowProb(0.2)
                .numSteps(30)
                .epochs(2)
                .status(Status.NOT_STARTED)
                .build();
        doReturn(task).when(service).getById(11L);
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());
        when(algorithmService.getById(2L)).thenReturn(Algorithm.builder().id(2L).algorithmName("FedAvg").build());
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/train/start"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("boom"));

        String result = service.startTask(11L, 7L);

        assertThat(result).contains("boom");
    }

    @Test
    void stopTaskShouldReturnExceptionMessageWhenRestThrows() {
        Task running = Task.builder().id(12L).uid(7L).status(Status.IN_PROGRESS).build();
        doReturn(running).when(service).getById(12L);
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/train/stop/12"), eq(null), eq(String.class)))
                .thenThrow(new RuntimeException("stop-ex"));

        String result = service.stopTask(12L, 7L);

        assertThat(result).contains("stop-ex");
    }

    private static CreateTaskRO buildCreateTaskRO() {
        return CreateTaskRO.builder()
                .did(1L)
                .aid(2L)
                .numNodes(100)
                .fraction(0.1)
                .classesPerNode(5)
                .lowProb(0.2)
                .numSteps(10)
                .epochs(3)
                .build();
    }

    private static <R> SFunction<Task, R> anyTaskFn() {
        return (SFunction<Task, R>) any(SFunction.class);
    }
}
