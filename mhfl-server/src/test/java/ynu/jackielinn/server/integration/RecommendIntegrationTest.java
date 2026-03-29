package ynu.jackielinn.server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 推荐页面接口集成测试。
 * 覆盖 Controller -> Service -> Mapper -> H2 链路。
 */
@Sql(
        scripts = {
                "classpath:integration/recommend/schema.sql",
                "classpath:integration/recommend/data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class RecommendIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 成功场景：数据集存在时返回实验设置，且按候选任务顺序选中第一个推荐任务。
     */
    @Test
    void getExperimentSettingsShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/recommended/experiment-settings")
                        .param("datasetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.datasetId").value(1))
                .andExpect(jsonPath("$.data.sourceTaskId").value(2))
                .andExpect(jsonPath("$.data.numNodes").value(100))
                .andExpect(jsonPath("$.data.numSteps").value(500))
                .andExpect(jsonPath("$.data.epochs").value(10))
                .andExpect(jsonPath("$.data.algorithmNames[0]").value("Standalone"))
                .andExpect(jsonPath("$.data.algorithmNames[5]").value("FedJitter"));
    }

    /**
     * 失败场景：数据集不存在时返回 400 业务码。
     */
    @Test
    void getExperimentSettingsShouldReturnFailureWhenDatasetNotExists() throws Exception {
        mockMvc.perform(get("/api/recommended/experiment-settings")
                        .param("datasetId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：算法效果对比返回候选任务顺序的固定列。
     */
    @Test
    void getMetricsCompareShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/recommended/metrics-compare")
                        .param("datasetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.datasetId").value(1))
                .andExpect(jsonPath("$.data.items[1].taskId").value(2))
                .andExpect(jsonPath("$.data.items[1].algorithmName").value("FedAvg"))
                .andExpect(jsonPath("$.data.items[1].accuracy").value(0.8))
                .andExpect(jsonPath("$.data.items[5].taskId").value(13))
                .andExpect(jsonPath("$.data.items[5].algorithmName").value("FedJitter"));
    }

    /**
     * 失败场景：数据集不存在时返回 400 业务码。
     */
    @Test
    void getMetricsCompareShouldReturnFailureWhenDatasetNotExists() throws Exception {
        mockMvc.perform(get("/api/recommended/metrics-compare")
                        .param("datasetId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：测试集曲线返回原始值与平滑值。
     */
    @Test
    void getTestCurvesShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/recommended/test-curves")
                        .param("datasetId", "1")
                        .param("sigma", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.datasetId").value(1))
                .andExpect(jsonPath("$.data.rounds[0]").value(1))
                .andExpect(jsonPath("$.data.rounds[1]").value(2))
                .andExpect(jsonPath("$.data.algorithms[1].taskId").value(2))
                .andExpect(jsonPath("$.data.algorithms[1].algorithmName").value("FedAvg"))
                .andExpect(jsonPath("$.data.algorithms[1].accuracyRaw[0]").value(0.7))
                .andExpect(jsonPath("$.data.algorithms[1].accuracyRaw[1]").value(0.77))
                .andExpect(jsonPath("$.data.algorithms[1].accuracySmooth[0]").value(0.7))
                .andExpect(jsonPath("$.data.algorithms[5].taskId").value(13))
                .andExpect(jsonPath("$.data.algorithms[5].accuracyRaw[0]").value(0.6));
    }

    /**
     * 失败场景：数据集不存在时返回 400 业务码。
     */
    @Test
    void getTestCurvesShouldReturnFailureWhenDatasetNotExists() throws Exception {
        mockMvc.perform(get("/api/recommended/test-curves")
                        .param("datasetId", "999")
                        .param("sigma", "2.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：客户端最新指标按算法顺序返回。
     */
    @Test
    void getClientMetricsShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/recommended/clients-metrics")
                        .param("datasetId", "1")
                        .param("metric", "accuracy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.datasetId").value(1))
                .andExpect(jsonPath("$.data.metric").value("accuracy"))
                .andExpect(jsonPath("$.data.algorithmNames[1]").value("FedAvg"))
                .andExpect(jsonPath("$.data.algorithmNames[5]").value("FedJitter"))
                .andExpect(jsonPath("$.data.clients[0].clientIndex").value(0))
                .andExpect(jsonPath("$.data.clients[0].values[1]").value(0.77))
                .andExpect(jsonPath("$.data.clients[0].values[5]").value(0.6));
    }

    /**
     * 失败场景：metric 非法时返回 400 业务码。
     */
    @Test
    void getClientMetricsShouldReturnFailureWhenMetricInvalid() throws Exception {
        mockMvc.perform(get("/api/recommended/clients-metrics")
                        .param("datasetId", "1")
                        .param("metric", "bad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：客户端详情返回按轮次前向填充后的曲线。
     */
    @Test
    void getClientDetailShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/recommended/client-detail")
                        .param("datasetId", "1")
                        .param("clientIndex", "0")
                        .param("metric", "accuracy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.datasetId").value(1))
                .andExpect(jsonPath("$.data.clientIndex").value(0))
                .andExpect(jsonPath("$.data.metric").value("accuracy"))
                .andExpect(jsonPath("$.data.rounds[0]").value(1))
                .andExpect(jsonPath("$.data.rounds[1]").value(2))
                .andExpect(jsonPath("$.data.algorithms[1].taskId").value(2))
                .andExpect(jsonPath("$.data.algorithms[1].values[0]").value(0.7))
                .andExpect(jsonPath("$.data.algorithms[1].values[1]").value(0.77))
                .andExpect(jsonPath("$.data.algorithms[1].values[2]").value(0.77))
                .andExpect(jsonPath("$.data.algorithms[5].taskId").value(13))
                .andExpect(jsonPath("$.data.algorithms[5].values[0]").value(0.6))
                .andExpect(jsonPath("$.data.algorithms[5].values[1]").value(0.6));
    }

    /**
     * 失败场景：clientIndex 非法时返回 400 业务码。
     */
    @Test
    void getClientDetailShouldReturnFailureWhenClientIndexInvalid() throws Exception {
        mockMvc.perform(get("/api/recommended/client-detail")
                        .param("datasetId", "1")
                        .param("clientIndex", "-1")
                        .param("metric", "accuracy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
