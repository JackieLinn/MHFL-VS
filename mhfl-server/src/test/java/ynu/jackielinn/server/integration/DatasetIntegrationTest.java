package ynu.jackielinn.server.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import ynu.jackielinn.server.service.DatasetService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 数据集模块接口集成测试。
 * 覆盖 Controller -> Service -> Mapper -> H2 链路。
 */
@Sql(
        scripts = {
                "classpath:integration/dataset/schema.sql",
                "classpath:integration/dataset/data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class DatasetIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatasetService datasetService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 成功场景：管理员创建数据集成功。
     */
    @Test
    void createDatasetShouldReturnSuccess() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(post("/api/dataset/admin/create")
                        .requestAttr("id", 1L)
                        .param("dataName", "ImageNet-Subset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(datasetService.query().eq("data_name", "ImageNet-Subset").count()).isEqualTo(1);
    }

    /**
     * 失败场景：创建数据集时名称重复。
     */
    @Test
    void createDatasetShouldReturnFailureWhenDuplicateName() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(post("/api/dataset/admin/create")
                        .requestAttr("id", 1L)
                        .param("dataName", "CIFAR-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：管理员删除数据集成功。
     */
    @Test
    void deleteDatasetShouldReturnSuccess() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(delete("/api/dataset/admin/2")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(datasetService.getById(2L)).isNull();
    }

    /**
     * 失败场景：删除不存在的数据集失败。
     */
    @Test
    void deleteDatasetShouldReturnFailureWhenNotExists() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(delete("/api/dataset/admin/99")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：管理员更新数据集成功。
     */
    @Test
    void updateDatasetShouldReturnSuccess() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(put("/api/dataset/admin/2")
                        .requestAttr("id", 1L)
                        .param("dataName", "Tiny-ImageNet-V2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(datasetService.getById(2L).getDataName()).isEqualTo("Tiny-ImageNet-V2");
    }

    /**
     * 失败场景：更新数据集时名称重复。
     */
    @Test
    void updateDatasetShouldReturnFailureWhenDuplicateName() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(put("/api/dataset/admin/2")
                        .requestAttr("id", 1L)
                        .param("dataName", "CIFAR-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：查询数据集列表成功。
     */
    @Test
    void listDatasetsShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/dataset/list")
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
    void listDatasetsShouldReturnFailureWhenPageParamInvalid() throws Exception {
        mockMvc.perform(get("/api/dataset/list")
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
