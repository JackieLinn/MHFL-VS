package ynu.jackielinn.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ynu.jackielinn.server.common.ApiResponse;
import ynu.jackielinn.server.common.BaseController;
import ynu.jackielinn.server.dto.request.CreateTaskRO;
import ynu.jackielinn.server.dto.request.ListTaskRO;
import ynu.jackielinn.server.dto.response.TaskVO;
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

    /**
     * 任务列表（分页）。非管理员仅返回当前用户任务，管理员返回全部。
     * 关键字叠加搜索：数据集名、算法名；管理员还可按用户名搜索。
     *
     * @param ro      查询条件（keyword、current、size、startTime、endTime）
     * @param request 用于获取当前用户 id
     * @return 分页结果 TaskVO（含 dataName、algorithmName、username）
     */
    @Operation(summary = "任务列表接口", description = "分页查询任务列表，支持关键字与时间范围；管理员看全部，普通用户仅看自己的")
    @GetMapping("/list")
    public ApiResponse<IPage<TaskVO>> listTasks(@Valid @ModelAttribute ListTaskRO ro, HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("id");
        if (uid == null) {
            return ApiResponse.failure(401, "未登录或登录已过期");
        }
        boolean isAdmin = isAdmin();
        IPage<TaskVO> result = taskService.listTasks(ro, uid, isAdmin);
        return ApiResponse.success(result);
    }
}
