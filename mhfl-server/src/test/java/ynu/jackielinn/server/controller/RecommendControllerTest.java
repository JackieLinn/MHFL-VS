package ynu.jackielinn.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ynu.jackielinn.server.dto.response.RecommendClientDetailVO;
import ynu.jackielinn.server.dto.response.RecommendClientMetricsVO;
import ynu.jackielinn.server.dto.response.RecommendExperimentSettingsVO;
import ynu.jackielinn.server.dto.response.RecommendMetricsCompareVO;
import ynu.jackielinn.server.dto.response.RecommendTestCurvesVO;
import ynu.jackielinn.server.entity.Dataset;
import ynu.jackielinn.server.service.DatasetService;
import ynu.jackielinn.server.service.RecommendService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(value = RecommendController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class RecommendControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecommendController recommendController;

    @MockitoBean
    private RecommendService recommendService;

    @MockitoBean
    private DatasetService datasetService;

    @Test
    void getExperimentSettingsShouldReturnBadRequestWhenDatasetMissing() throws Exception {
        when(datasetService.getById(1L)).thenReturn(null);

        mockMvc.perform(get("/api/recommended/experiment-settings").param("datasetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getExperimentSettingsShouldUseTinyTaskIds() throws Exception {
        when(datasetService.getById(2L)).thenReturn(Dataset.builder().id(2L).dataName("tiny-imagenet").build());
        when(recommendService.getExperimentSettings(eq(2L), anyList()))
                .thenReturn(RecommendExperimentSettingsVO.builder().datasetId(2L).algorithmNames(List.of()).build());

        mockMvc.perform(get("/api/recommended/experiment-settings").param("datasetId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.datasetId").value(2));

        verify(recommendService).getExperimentSettings(eq(2L), eq(List.of(6L, 8L, 9L, 10L, 11L, 14L)));
    }

    @Test
    void getMetricsCompareShouldUseCifarTaskIdsWhenNameNull() throws Exception {
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName(null).build());
        when(recommendService.getMetricsCompare(eq(1L), anyList()))
                .thenReturn(RecommendMetricsCompareVO.builder().datasetId(1L).items(List.of()).build());

        mockMvc.perform(get("/api/recommended/metrics-compare").param("datasetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.datasetId").value(1));

        verify(recommendService).getMetricsCompare(eq(1L), eq(List.of(1L, 2L, 3L, 4L, 5L, 13L)));
    }

    @Test
    void getMetricsCompareShouldReturnBadRequestWhenDatasetMissing() throws Exception {
        when(datasetService.getById(1L)).thenReturn(null);

        mockMvc.perform(get("/api/recommended/metrics-compare").param("datasetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getTestCurvesShouldPassDefaultSigma() throws Exception {
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());
        when(recommendService.getTestCurves(eq(1L), anyList(), eq(2.5)))
                .thenReturn(RecommendTestCurvesVO.builder().datasetId(1L).rounds(List.of()).algorithms(List.of()).build());

        mockMvc.perform(get("/api/recommended/test-curves").param("datasetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(recommendService).getTestCurves(eq(1L), eq(List.of(1L, 2L, 3L, 4L, 5L, 13L)), eq(2.5));
    }

    @Test
    void getTestCurvesShouldReturnBadRequestWhenDatasetMissing() throws Exception {
        when(datasetService.getById(1L)).thenReturn(null);

        mockMvc.perform(get("/api/recommended/test-curves").param("datasetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getClientMetricsShouldReturnBadRequestWhenServiceThrows() throws Exception {
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());
        when(recommendService.getClientMetrics(eq(1L), anyList(), eq("bad")))
                .thenThrow(new IllegalArgumentException("metric invalid"));

        mockMvc.perform(get("/api/recommended/clients-metrics")
                        .param("datasetId", "1")
                        .param("metric", "bad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("metric invalid"));
    }

    @Test
    void getClientMetricsShouldReturnBadRequestWhenDatasetMissing() throws Exception {
        when(datasetService.getById(1L)).thenReturn(null);

        mockMvc.perform(get("/api/recommended/clients-metrics")
                        .param("datasetId", "1")
                        .param("metric", "accuracy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getClientMetricsShouldReturnSuccess() throws Exception {
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());
        when(recommendService.getClientMetrics(eq(1L), anyList(), eq("accuracy")))
                .thenReturn(RecommendClientMetricsVO.builder()
                        .datasetId(1L)
                        .metric("accuracy")
                        .algorithmNames(List.of("FedAvg"))
                        .clients(List.of())
                        .build());

        mockMvc.perform(get("/api/recommended/clients-metrics")
                        .param("datasetId", "1")
                        .param("metric", "accuracy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.metric").value("accuracy"));
    }

    @Test
    void getClientDetailShouldReturnBadRequestWhenClientIndexInvalid() throws Exception {
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());

        mockMvc.perform(get("/api/recommended/client-detail")
                        .param("datasetId", "1")
                        .param("clientIndex", "-1")
                        .param("metric", "accuracy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getClientDetailShouldReturnBadRequestWhenDatasetMissing() throws Exception {
        when(datasetService.getById(1L)).thenReturn(null);

        mockMvc.perform(get("/api/recommended/client-detail")
                        .param("datasetId", "1")
                        .param("clientIndex", "0")
                        .param("metric", "accuracy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getClientDetailShouldReturnBadRequestWhenServiceThrows() throws Exception {
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());
        when(recommendService.getClientDetail(eq(1L), anyList(), eq(0), eq("bad")))
                .thenThrow(new IllegalArgumentException("metric invalid"));

        mockMvc.perform(get("/api/recommended/client-detail")
                        .param("datasetId", "1")
                        .param("clientIndex", "0")
                        .param("metric", "bad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("metric invalid"));
    }

    @Test
    void getClientDetailShouldReturnSuccess() throws Exception {
        when(datasetService.getById(2L)).thenReturn(Dataset.builder().id(2L).dataName("tiny-imagenet").build());
        when(recommendService.getClientDetail(eq(2L), anyList(), eq(1), eq("accuracy")))
                .thenReturn(RecommendClientDetailVO.builder()
                        .datasetId(2L)
                        .clientIndex(1)
                        .metric("accuracy")
                        .rounds(List.of(1, 2))
                        .algorithms(List.of())
                        .build());

        mockMvc.perform(get("/api/recommended/client-detail")
                        .param("datasetId", "2")
                        .param("clientIndex", "1")
                        .param("metric", "accuracy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.clientIndex").value(1));

        verify(recommendService).getClientDetail(eq(2L), eq(List.of(6L, 8L, 9L, 10L, 11L, 14L)), eq(1), eq("accuracy"));
    }

    @Test
    void getClientDetailShouldReturnBadRequestWhenClientIndexNullDirectInvoke() {
        when(datasetService.getById(1L)).thenReturn(Dataset.builder().id(1L).dataName("CIFAR-100").build());

        var response = recommendController.getClientDetail(1L, null, "accuracy");

        assertThat(response.code()).isEqualTo(400);
    }
}
