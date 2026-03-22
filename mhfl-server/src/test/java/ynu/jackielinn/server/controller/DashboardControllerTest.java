package ynu.jackielinn.server.controller;

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
import ynu.jackielinn.server.dto.response.DashboardPlatformStatsVO;
import ynu.jackielinn.server.dto.response.DashboardStatCardsVO;
import ynu.jackielinn.server.dto.response.DashboardSystemHealthVO;
import ynu.jackielinn.server.dto.response.DashboardTaskStatusStatsVO;
import ynu.jackielinn.server.dto.response.DashboardTaskTrendVO;
import ynu.jackielinn.server.service.DashboardService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = DashboardController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPlatformStatsShouldReturnSuccess() throws Exception {
        when(dashboardService.getPlatformStats()).thenReturn(new DashboardPlatformStatsVO());

        mockMvc.perform(get("/api/dashboard/admin/platform-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dashboardService).getPlatformStats();
    }

    @Test
    void getTasksByAlgorithmShouldReturnMap() throws Exception {
        when(dashboardService.getTasksByAlgorithm()).thenReturn(Map.of("FedAvg", 3L));

        mockMvc.perform(get("/api/dashboard/admin/tasks-by-algorithm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.FedAvg").value(3));
    }

    @Test
    void getTaskStatusStatsShouldReturnUnauthorizedWhenRequestIdMissing() throws Exception {
        mockMvc.perform(get("/api/dashboard/task-status-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getTaskStatusStatsShouldUseAdminFlagFromSecurityContext() throws Exception {
        setAdminAuthentication();
        when(dashboardService.getTaskStatusStats(1L, true)).thenReturn(new DashboardTaskStatusStatsVO());

        mockMvc.perform(get("/api/dashboard/task-status-stats").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dashboardService).getTaskStatusStats(1L, true);
    }

    @Test
    void getTaskStatusStatsShouldUseNonAdminFlag() throws Exception {
        when(dashboardService.getTaskStatusStats(2L, false)).thenReturn(new DashboardTaskStatusStatsVO());

        mockMvc.perform(get("/api/dashboard/task-status-stats").requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dashboardService).getTaskStatusStats(2L, false);
    }

    @Test
    void getTaskTrend7DaysShouldUseNonAdminFlag() throws Exception {
        when(dashboardService.getTaskTrend7Days(2L, false)).thenReturn(new DashboardTaskTrendVO());

        mockMvc.perform(get("/api/dashboard/task-trend-7days").requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dashboardService).getTaskTrend7Days(2L, false);
    }

    @Test
    void getTaskTrend7DaysShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/dashboard/task-trend-7days"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getTaskTrend7DaysShouldUseAdminFlag() throws Exception {
        setAdminAuthentication();
        when(dashboardService.getTaskTrend7Days(1L, true)).thenReturn(new DashboardTaskTrendVO());

        mockMvc.perform(get("/api/dashboard/task-trend-7days").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dashboardService).getTaskTrend7Days(1L, true);
    }

    @Test
    void getStatCardsShouldReturnSuccess() throws Exception {
        when(dashboardService.getStatCards(2L, false)).thenReturn(new DashboardStatCardsVO());

        mockMvc.perform(get("/api/dashboard/stat-cards").requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getStatCardsShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/dashboard/stat-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getStatCardsShouldUseAdminFlag() throws Exception {
        setAdminAuthentication();
        when(dashboardService.getStatCards(1L, true)).thenReturn(new DashboardStatCardsVO());

        mockMvc.perform(get("/api/dashboard/stat-cards").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dashboardService).getStatCards(1L, true);
    }

    @Test
    void getRecentTasksShouldReturnSuccess() throws Exception {
        when(dashboardService.getRecentTasks(2L, false)).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/recent-tasks").requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getRecentTasksShouldReturnUnauthorizedWhenIdMissing() throws Exception {
        mockMvc.perform(get("/api/dashboard/recent-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getRecentTasksShouldUseAdminFlag() throws Exception {
        setAdminAuthentication();
        when(dashboardService.getRecentTasks(1L, true)).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/recent-tasks").requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dashboardService).getRecentTasks(1L, true);
    }

    @Test
    void getSystemHealthShouldReturnSuccess() throws Exception {
        when(dashboardService.getSystemHealth()).thenReturn(new DashboardSystemHealthVO());

        mockMvc.perform(get("/api/dashboard/system-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
    }
}
