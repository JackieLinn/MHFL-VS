package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.dto.response.RecommendClientDetailVO;
import ynu.jackielinn.server.dto.response.RecommendClientMetricsVO;
import ynu.jackielinn.server.dto.response.RecommendExperimentSettingsVO;
import ynu.jackielinn.server.dto.response.RecommendMetricsCompareVO;
import ynu.jackielinn.server.dto.response.RecommendTestCurvesVO;
import ynu.jackielinn.server.entity.Algorithm;
import ynu.jackielinn.server.entity.Client;
import ynu.jackielinn.server.entity.Round;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.ClientService;
import ynu.jackielinn.server.service.RoundService;
import ynu.jackielinn.server.service.TaskService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendServiceImplTest {

    @InjectMocks
    private RecommendServiceImpl service;

    @Mock
    private TaskService taskService;

    @Mock
    private AlgorithmService algorithmService;

    @Mock
    private RoundService roundService;

    @Mock
    private ClientService clientService;

    @Test
    void getExperimentSettingsShouldReturnBasicDataWhenNoSourceTask() {
        when(algorithmService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                Algorithm.builder().id(1L).algorithmName("A").build(),
                Algorithm.builder().id(2L).algorithmName("B").build()
        ));

        RecommendExperimentSettingsVO vo = service.getExperimentSettings(1L, null);

        assertThat(vo.getDatasetId()).isEqualTo(1L);
        assertThat(vo.getAlgorithmNames()).containsExactly("A", "B");
        assertThat(vo.getSourceTaskId()).isNull();
    }

    @Test
    void getExperimentSettingsShouldPickFirstMatchedTaskByCandidateOrder() {
        Task task1 = Task.builder().id(1L).did(1L).status(Status.RECOMMENDED).numNodes(100).epochs(10).build();
        Task task2 = Task.builder().id(2L).did(1L).status(Status.RECOMMENDED).numNodes(200).epochs(20).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task1, task2));
        when(algorithmService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        RecommendExperimentSettingsVO vo = service.getExperimentSettings(1L, List.of(3L, 2L, 2L, 1L));

        assertThat(vo.getSourceTaskId()).isEqualTo(2L);
        assertThat(vo.getNumNodes()).isEqualTo(200);
        assertThat(vo.getEpochs()).isEqualTo(20);
    }

    @Test
    void getExperimentSettingsShouldIgnoreNullCandidateIds() {
        Task task = Task.builder().id(1L).did(1L).status(Status.RECOMMENDED).numNodes(50).epochs(5).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        RecommendExperimentSettingsVO vo = service.getExperimentSettings(1L, Arrays.asList(3L, null, 1L));

        assertThat(vo.getSourceTaskId()).isEqualTo(1L);
        assertThat(vo.getNumNodes()).isEqualTo(50);
        assertThat(vo.getEpochs()).isEqualTo(5);
    }

    @Test
    void getMetricsCompareShouldKeepPlaceholderForMissingTasks() {
        Task task = Task.builder()
                .id(1L).aid(11L).did(1L).status(Status.RECOMMENDED)
                .loss(1.0).accuracy(0.8).precision(0.7).recall(0.6).f1Score(0.65)
                .build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.listByIds(any())).thenReturn(List.of(Algorithm.builder().id(11L).algorithmName("FedAvg").build()));

        RecommendMetricsCompareVO vo = service.getMetricsCompare(1L, List.of(1L, 2L));

        assertThat(vo.getItems()).hasSize(2);
        assertThat(vo.getItems().get(0).getAlgorithmName()).isEqualTo("FedAvg");
        assertThat(vo.getItems().get(1).getTaskId()).isNull();
        assertThat(vo.getItems().get(1).getAccuracy()).isNull();
    }

    @Test
    void getMetricsCompareShouldReturnEmptyWhenCandidateIdsNull() {
        RecommendMetricsCompareVO vo = service.getMetricsCompare(1L, null);

        assertThat(vo.getDatasetId()).isEqualTo(1L);
        assertThat(vo.getItems()).isEmpty();
        verify(taskService, never()).list(any(LambdaQueryWrapper.class));
        verify(algorithmService, never()).listByIds(any());
    }

    @Test
    void getTestCurvesShouldReturnPlaceholderWhenNoMatchedTask() {
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        RecommendTestCurvesVO vo = service.getTestCurves(1L, List.of(1L), null);

        assertThat(vo.getDatasetId()).isEqualTo(1L);
        assertThat(vo.getRounds()).isEmpty();
        assertThat(vo.getAlgorithms()).hasSize(1);
        assertThat(vo.getAlgorithms().get(0).getAccuracyRaw()).isEmpty();
    }

    @Test
    void getTestCurvesShouldUseRawWhenSigmaLessOrEqualZero() {
        Task task = Task.builder().id(1L).aid(11L).did(1L).status(Status.RECOMMENDED).numSteps(3).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.listByIds(any())).thenReturn(List.of(Algorithm.builder().id(11L).algorithmName("FedAvg").build()));
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of(
                Round.builder().tid(1L).roundNum(0).accuracy(0.2).precision(0.3).recall(0.4).f1Score(0.5).build(),
                Round.builder().tid(1L).roundNum(2).accuracy(0.6).precision(0.7).recall(0.8).f1Score(0.9).build()
        ));

        RecommendTestCurvesVO vo = service.getTestCurves(1L, List.of(1L), 0.0);

        assertThat(vo.getRounds()).containsExactly(1, 2, 3);
        assertThat(vo.getAlgorithms()).hasSize(1);
        assertThat(vo.getAlgorithms().get(0).getAccuracyRaw())
                .containsExactly(0.2, null, 0.6);
        assertThat(vo.getAlgorithms().get(0).getAccuracySmooth())
                .containsExactly(0.2, null, 0.6);
    }

    @Test
    void getTestCurvesShouldHandleNaNSigmaAndAllNullSeries() {
        Task task = Task.builder().id(1L).aid(null).did(1L).status(Status.RECOMMENDED).numSteps(2).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of(
                Round.builder().tid(1L).roundNum(0).accuracy(null).precision(null).recall(null).f1Score(null).build(),
                Round.builder().tid(1L).roundNum(1).accuracy(null).precision(null).recall(null).f1Score(null).build()
        ));

        RecommendTestCurvesVO vo = service.getTestCurves(1L, List.of(1L), Double.NaN);

        assertThat(vo.getRounds()).containsExactly(1, 2);
        assertThat(vo.getAlgorithms().get(0).getAlgorithmName()).isNull();
        assertThat(vo.getAlgorithms().get(0).getAccuracySmooth()).containsExactly(null, null);
        verify(algorithmService, never()).listByIds(any());
    }

    @Test
    void getTestCurvesShouldClampNegativeSigmaToZero() {
        Task task = Task.builder().id(1L).aid(11L).did(1L).status(Status.RECOMMENDED).numSteps(2).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.listByIds(any())).thenReturn(List.of(Algorithm.builder().id(11L).algorithmName("FedAvg").build()));
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of(
                Round.builder().tid(1L).roundNum(0).accuracy(0.1).precision(0.2).recall(0.3).f1Score(0.4).build(),
                Round.builder().tid(1L).roundNum(1).accuracy(0.2).precision(0.3).recall(0.4).f1Score(0.5).build()
        ));

        RecommendTestCurvesVO vo = service.getTestCurves(1L, List.of(1L), -1.0);

        assertThat(vo.getAlgorithms().get(0).getAccuracySmooth()).containsExactly(0.1, 0.2);
    }

    @Test
    void getTestCurvesShouldReturnEmptyWhenOnlyNullCandidates() {
        RecommendTestCurvesVO vo = service.getTestCurves(1L, Arrays.asList(null, null), 2.5);

        assertThat(vo.getRounds()).isEmpty();
        assertThat(vo.getAlgorithms()).isEmpty();
        verify(taskService, never()).list(any(LambdaQueryWrapper.class));
    }

    @Test
    void getTestCurvesShouldFallbackRoundsCountFromRoundsWhenNumStepsMissing() {
        Task task = Task.builder().id(1L).aid(11L).did(1L).status(Status.RECOMMENDED).numSteps(null).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.listByIds(any())).thenReturn(List.of(Algorithm.builder().id(11L).algorithmName("FedAvg").build()));
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of(
                Round.builder().tid(1L).roundNum(0).accuracy(0.2).precision(0.3).recall(0.4).f1Score(0.5).build(),
                Round.builder().tid(1L).roundNum(1).accuracy(0.3).precision(0.4).recall(0.5).f1Score(0.6).build()
        ));

        RecommendTestCurvesVO vo = service.getTestCurves(1L, List.of(1L), 10.0);

        assertThat(vo.getRounds()).containsExactly(1, 2);
        assertThat(vo.getAlgorithms().get(0).getAccuracySmooth()).hasSize(2);
    }

    @Test
    void getClientMetricsShouldThrowWhenMetricInvalid() {
        assertThatThrownBy(() -> service.getClientMetrics(1L, List.of(1L), "bad"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getClientMetrics(1L, List.of(1L), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getClientMetricsShouldReturnDefaultClientCountWhenNoValidTaskIds() {
        RecommendClientMetricsVO vo = service.getClientMetrics(1L, Arrays.asList(null, null), "accuracy");

        assertThat(vo.getClients()).hasSize(100);
        assertThat(vo.getAlgorithmNames()).isEmpty();
        verify(taskService, never()).list(any(LambdaQueryWrapper.class));
    }

    @Test
    void getClientMetricsShouldAcceptTrimmedPrecisionMetric() {
        Task task = Task.builder().id(1L).aid(null).did(1L).status(Status.RECOMMENDED).numNodes(1).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of(
                Round.builder().id(10L).roundNum(0).build()
        ));
        when(clientService.listByRidIn(eq(List.of(10L)))).thenReturn(List.of(
                Client.builder().rid(10L).clientIndex(0).precision(0.7).build()
        ));

        RecommendClientMetricsVO vo = service.getClientMetrics(1L, List.of(1L), "  Precision ");

        assertThat(vo.getMetric()).isEqualTo("precision");
        assertThat(vo.getAlgorithmNames()).containsExactly((String) null);
        assertThat(vo.getClients().get(0).getValues()).containsExactly(0.7);
        verify(algorithmService, never()).getById(any());
    }

    @Test
    void getClientMetricsShouldHandleRoundsWithoutRidAndClientListNull() {
        Task task = Task.builder().id(1L).aid(7L).did(1L).status(Status.RECOMMENDED).numNodes(2).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.getById(7L)).thenReturn(Algorithm.builder().id(7L).algorithmName("FedAvg").build());
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of(
                Round.builder().id(null).roundNum(0).build()
        ));

        RecommendClientMetricsVO vo = service.getClientMetrics(1L, List.of(1L), "accuracy");

        assertThat(vo.getClients()).hasSize(2);
        assertThat(vo.getClients().get(0).getValues()).containsExactly((Double) null);
        assertThat(vo.getClients().get(1).getValues()).containsExactly((Double) null);
        verify(clientService, never()).listByRidIn(any());
    }

    @Test
    void getClientMetricsShouldKeepLatestWhenCandidateOlderOrTimestampNull() {
        Task task = Task.builder().id(1L).aid(7L).did(1L).status(Status.RECOMMENDED).numNodes(1).build();
        LocalDateTime now = LocalDateTime.now();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.getById(7L)).thenReturn(Algorithm.builder().id(7L).algorithmName("FedAvg").build());
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of(
                Round.builder().id(10L).roundNum(1).build()
        ));
        when(clientService.listByRidIn(eq(List.of(10L)))).thenReturn(List.of(
                Client.builder().rid(10L).clientIndex(0).accuracy(0.8).timestamp(now).build(),
                Client.builder().rid(10L).clientIndex(0).accuracy(0.2).timestamp(null).build(),
                Client.builder().rid(10L).clientIndex(0).accuracy(0.3).timestamp(now.minusSeconds(10)).build(),
                Client.builder().rid(10L).clientIndex(null).accuracy(0.4).timestamp(now.plusSeconds(1)).build()
        ));

        RecommendClientMetricsVO vo = service.getClientMetrics(1L, List.of(1L), "accuracy");

        assertThat(vo.getClients().get(0).getValues()).containsExactly(0.8);
    }

    @Test
    void getClientMetricsShouldReturnLatestMetricsByClient() {
        Task task = Task.builder().id(1L).aid(7L).did(1L).status(Status.RECOMMENDED).numNodes(3).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.getById(7L)).thenReturn(Algorithm.builder().id(7L).algorithmName("FedAvg").build());
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of(
                Round.builder().id(10L).roundNum(0).build(),
                Round.builder().id(11L).roundNum(1).build()
        ));
        when(clientService.listByRidIn(eq(List.of(10L, 11L)))).thenReturn(List.of(
                Client.builder().rid(10L).clientIndex(0).accuracy(0.3).timestamp(LocalDateTime.now().minusMinutes(2)).build(),
                Client.builder().rid(11L).clientIndex(0).accuracy(0.6).timestamp(LocalDateTime.now().minusMinutes(1)).build(),
                Client.builder().rid(11L).clientIndex(1).accuracy(0.4).timestamp(null).build(),
                Client.builder().rid(11L).clientIndex(1).accuracy(0.5).timestamp(LocalDateTime.now()).build(),
                Client.builder().rid(11L).clientIndex(1).accuracy(0.45).timestamp(null).build()
        ));

        RecommendClientMetricsVO vo = service.getClientMetrics(1L, List.of(1L, 2L), "accuracy");

        assertThat(vo.getMetric()).isEqualTo("accuracy");
        assertThat(vo.getAlgorithmNames()).containsExactly("FedAvg", null);
        assertThat(vo.getClients()).hasSize(3);
        assertThat(vo.getClients().get(0).getValues()).containsExactly(0.6, null);
        assertThat(vo.getClients().get(1).getValues()).containsExactly(0.5, null);
        assertThat(vo.getClients().get(2).getValues()).containsExactly(null, null);
    }

    @Test
    void getClientMetricsShouldReturnEmptyWhenTaskRoundsMissing() {
        Task task = Task.builder().id(1L).aid(7L).did(1L).status(Status.RECOMMENDED).numNodes(2).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.getById(7L)).thenReturn(Algorithm.builder().id(7L).algorithmName("FedAvg").build());
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of());

        RecommendClientMetricsVO vo = service.getClientMetrics(1L, List.of(1L), "accuracy");

        assertThat(vo.getClients()).hasSize(2);
        assertThat(vo.getClients().get(0).getValues()).containsExactly((Double) null);
        assertThat(vo.getClients().get(1).getValues()).containsExactly((Double) null);
    }

    @Test
    void getClientDetailShouldThrowWhenMetricInvalid() {
        assertThatThrownBy(() -> service.getClientDetail(1L, List.of(1L), 0, "bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getClientDetailShouldSupportRecallMetric() {
        Task task = Task.builder().id(1L).aid(7L).did(1L).status(Status.RECOMMENDED).numSteps(2).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.getById(7L)).thenReturn(Algorithm.builder().id(7L).algorithmName("FedAvg").build());
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of(
                Round.builder().id(10L).roundNum(0).build(),
                Round.builder().id(11L).roundNum(1).build()
        ));
        when(clientService.listByRidsAndClientIndex(eq(List.of(10L, 11L)), eq(0))).thenReturn(List.of(
                Client.builder().rid(10L).clientIndex(0).recall(0.5).build(),
                Client.builder().rid(11L).clientIndex(0).recall(0.6).build()
        ));

        RecommendClientDetailVO vo = service.getClientDetail(1L, List.of(1L), 0, "recall");

        assertThat(vo.getMetric()).isEqualTo("recall");
        assertThat(vo.getAlgorithms().get(0).getValues()).containsExactly(0.5, 0.6);
    }

    @Test
    void getClientDetailShouldIgnoreInvalidRidAndRoundRangeAndNullMetricValue() {
        Task task = Task.builder().id(1L).aid(null).did(1L).status(Status.RECOMMENDED).numSteps(2).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of(
                Round.builder().id(10L).roundNum(0).build(),
                Round.builder().id(11L).roundNum(1).build(),
                Round.builder().id(12L).roundNum(-1).build(),
                Round.builder().id(13L).roundNum(5).build(),
                Round.builder().id(14L).roundNum(null).build()
        ));
        when(clientService.listByRidsAndClientIndex(eq(List.of(10L, 11L, 12L, 13L)), eq(0))).thenReturn(List.of(
                Client.builder().rid(null).clientIndex(0).f1Score(0.2).build(),
                Client.builder().rid(999L).clientIndex(0).f1Score(0.2).build(),
                Client.builder().rid(12L).clientIndex(0).f1Score(0.3).build(),
                Client.builder().rid(13L).clientIndex(0).f1Score(0.4).build(),
                Client.builder().rid(10L).clientIndex(0).f1Score(0.6).build(),
                Client.builder().rid(11L).clientIndex(0).f1Score(null).build()
        ));

        RecommendClientDetailVO vo = service.getClientDetail(1L, List.of(1L), 0, "f1");

        assertThat(vo.getAlgorithms()).hasSize(1);
        assertThat(vo.getAlgorithms().get(0).getAlgorithmName()).isNull();
        assertThat(vo.getAlgorithms().get(0).getValues()).containsExactly(0.6, 0.6);
    }

    @Test
    void getClientDetailShouldReturnEmptyForNullCandidates() {
        RecommendClientDetailVO vo = service.getClientDetail(1L, null, 0, "accuracy");

        assertThat(vo.getRounds()).isEmpty();
        assertThat(vo.getAlgorithms()).isEmpty();
        assertThat(vo.getMetric()).isEqualTo("accuracy");
        verify(taskService, never()).list(any(LambdaQueryWrapper.class));
    }

    @Test
    void getClientDetailShouldReturnCarryForwardSeriesAndZeroForMissingTask() {
        Task task = Task.builder().id(1L).aid(7L).did(1L).status(Status.RECOMMENDED).numSteps(5).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.getById(7L)).thenReturn(Algorithm.builder().id(7L).algorithmName("FedAvg").build());
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of(
                Round.builder().id(100L).roundNum(1).build(),
                Round.builder().id(101L).roundNum(3).build()
        ));
        when(clientService.listByRidsAndClientIndex(eq(List.of(100L, 101L)), eq(0))).thenReturn(List.of(
                Client.builder().rid(100L).clientIndex(0).accuracy(0.2).build(),
                Client.builder().rid(101L).clientIndex(0).accuracy(0.6).build()
        ));

        RecommendClientDetailVO vo = service.getClientDetail(1L, List.of(1L, 2L), 0, "accuracy");

        assertThat(vo.getRounds()).containsExactly(1, 2, 3, 4, 5);
        assertThat(vo.getAlgorithms()).hasSize(2);
        assertThat(vo.getAlgorithms().get(0).getValues())
                .containsExactly(0.0, 0.2, 0.2, 0.6, 0.6);
        assertThat(vo.getAlgorithms().get(1).getValues())
                .containsExactly(0.0, 0.0, 0.0, 0.0, 0.0);
    }

    @Test
    void getClientDetailShouldFallbackRoundsCountWhenNumStepsMissing() {
        Task task = Task.builder().id(1L).aid(7L).did(1L).status(Status.RECOMMENDED).numSteps(null).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.getById(7L)).thenReturn(Algorithm.builder().id(7L).algorithmName("FedAvg").build());
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(List.of(
                Round.builder().id(100L).roundNum(0).build(),
                Round.builder().id(101L).roundNum(2).build()
        ));
        when(clientService.listByRidsAndClientIndex(eq(List.of(100L, 101L)), eq(0))).thenReturn(List.of());

        RecommendClientDetailVO vo = service.getClientDetail(1L, List.of(1L), 0, "accuracy");

        assertThat(vo.getRounds()).containsExactly(1, 2, 3);
        assertThat(vo.getAlgorithms().get(0).getValues()).containsExactly(0.0, 0.0, 0.0);
    }

    @Test
    void getClientDetailShouldHandleRoundsWithNullListOnFallback() {
        Task task = Task.builder().id(1L).aid(7L).did(1L).status(Status.RECOMMENDED).numSteps(null).build();
        when(taskService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(algorithmService.getById(7L)).thenReturn(Algorithm.builder().id(7L).algorithmName("FedAvg").build());
        when(roundService.listByTidOrderByRoundNum(1L)).thenReturn(null, List.of());
        when(clientService.listByRidsAndClientIndex(eq(List.of()), eq(0))).thenReturn(List.of());

        RecommendClientDetailVO vo = service.getClientDetail(1L, List.of(1L), 0, "accuracy");

        assertThat(vo.getRounds()).isEmpty();
        assertThat(vo.getAlgorithms().get(0).getValues()).isEmpty();
    }
}
