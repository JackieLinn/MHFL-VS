package ynu.jackielinn.server.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import ynu.jackielinn.server.service.AlgorithmService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 算法模块接口集成测试。
 * 覆盖 Controller -> Service -> Mapper -> H2 链路。
 */
@Sql(
        scripts = {
                "classpath:integration/algorithm/schema.sql",
                "classpath:integration/algorithm/data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class AlgorithmIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlgorithmService algorithmService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 成功场景：管理员创建算法成功。
     */
    @Test
    void createAlgorithmShouldReturnSuccess() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(post("/api/algorithm/admin/create")
                        .requestAttr("id", 1L)
                        .param("algorithmName", "FedJitter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(algorithmService.query().eq("algorithm_name", "FedJitter").count()).isEqualTo(1);
    }

    /**
     * 失败场景：创建算法时名称重复。
     */
    @Test
    void createAlgorithmShouldReturnFailureWhenDuplicateName() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(post("/api/algorithm/admin/create")
                        .requestAttr("id", 1L)
                        .param("algorithmName", "FedAvg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：管理员删除算法成功。
     */
    @Test
    void deleteAlgorithmShouldReturnSuccess() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(delete("/api/algorithm/admin/2")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(algorithmService.getById(2L)).isNull();
    }

    /**
     * 失败场景：删除不存在的算法失败。
     */
    @Test
    void deleteAlgorithmShouldReturnFailureWhenNotExists() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(delete("/api/algorithm/admin/99")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：管理员更新算法成功。
     */
    @Test
    void updateAlgorithmShouldReturnSuccess() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(put("/api/algorithm/admin/2")
                        .requestAttr("id", 1L)
                        .param("algorithmName", "FedProtoV2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(algorithmService.getById(2L).getAlgorithmName()).isEqualTo("FedProtoV2");
    }

    /**
     * 失败场景：更新算法时名称重复。
     */
    @Test
    void updateAlgorithmShouldReturnFailureWhenDuplicateName() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(put("/api/algorithm/admin/2")
                        .requestAttr("id", 1L)
                        .param("algorithmName", "FedAvg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：查询算法列表成功。
     */
    @Test
    void listAlgorithmsShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/algorithm/list")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(3));
    }

    /**
     * 失败场景：分页参数非法时返回 4xx。
     */
    @Test
    void listAlgorithmsShouldReturnFailureWhenPageParamInvalid() throws Exception {
        mockMvc.perform(get("/api/algorithm/list")
                        .param("current", "abc")
                        .param("size", "10"))
                .andExpect(status().is4xxClientError());
    }

    /**
     * 设置管理员鉴权上下文。
     */
    private void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_admin"))
                )
        );
    }
}
