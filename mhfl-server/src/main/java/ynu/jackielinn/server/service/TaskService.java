package ynu.jackielinn.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ynu.jackielinn.server.dto.request.CreateTaskRO;
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
}
