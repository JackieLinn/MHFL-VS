package ynu.jackielinn.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import ynu.jackielinn.server.dto.request.CreateTaskRO;
import ynu.jackielinn.server.dto.request.ListTaskRO;
import ynu.jackielinn.server.dto.response.TaskVO;
import ynu.jackielinn.server.entity.Task;

public interface TaskService extends IService<Task> {

    /**
     * 创建任务（校验 did、aid 存在，status 设为 NOT_STARTED）
     *
     * @param ro  创建任务请求参数
     * @param uid 当前用户 id（从 JWT 取）
     * @return 新任务的 id
     * @throws IllegalArgumentException 校验失败时（如 did/aid 不存在）
     */
    Long createTask(CreateTaskRO ro, Long uid);

    /**
     * 分页查询任务列表（非管理员仅返回当前用户任务，管理员返回全部）
     * 关键字叠加搜索：数据集名、算法名；管理员还可按用户名搜索
     *
     * @param ro            查询条件（关键字、分页、时间范围）
     * @param currentUserId 当前用户 id
     * @param isAdmin       是否为管理员
     * @return 分页结果（TaskVO，含 dataName/algorithmName/username）
     */
    IPage<TaskVO> listTasks(ListTaskRO ro, Long currentUserId, boolean isAdmin);
}
