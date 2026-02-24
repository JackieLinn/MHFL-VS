package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ynu.jackielinn.server.common.ApiResponse;
import ynu.jackielinn.server.common.BaseController;
import ynu.jackielinn.server.dto.request.CreateTaskRO;
import ynu.jackielinn.server.service.TaskService;

@RestController
@RequestMapping("/api/task")
@Tag(name = "任务接口", description = "任务操作相关接口")
public class TaskController extends BaseController {

    @Resource
    private TaskService taskService;

    /**
     * 创建任务（uid 从 JWT token 取，status 为 NOT_STARTED）
     *
     * @param ro      创建任务参数（did, aid, numNodes, fraction, classesPerNode, lowProb, numSteps, epochs）
     * @param request 用于获取当前用户 id
     * @return 新任务的 id
     */
    @Operation(summary = "创建任务接口", description = "创建训练任务，数据集与算法 id 需已存在")
    @PostMapping
    public ApiResponse<Long> createTask(@RequestBody @Valid CreateTaskRO ro, HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("id");
        if (uid == null) {
            return ApiResponse.failure(401, "未登录或登录已过期");
        }
        try {
            Long taskId = taskService.createTask(ro, uid);
            return ApiResponse.success(taskId);
        } catch (IllegalArgumentException e) {
            return ApiResponse.failure(400, e.getMessage());
        }
    }
}
