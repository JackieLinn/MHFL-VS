package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.dto.request.CreateTaskRO;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.mapper.TaskMapper;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.DatasetService;
import ynu.jackielinn.server.service.TaskService;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Resource
    private DatasetService datasetService;

    @Resource
    private AlgorithmService algorithmService;

    @Override
    public Long createTask(CreateTaskRO ro, Long uid) {
        if (datasetService.getById(ro.getDid()) == null) {
            throw new IllegalArgumentException("数据集不存在");
        }
        if (algorithmService.getById(ro.getAid()) == null) {
            throw new IllegalArgumentException("算法不存在");
        }
        Task task = Task.builder()
                .uid(uid)
                .did(ro.getDid())
                .aid(ro.getAid())
                .numNodes(ro.getNumNodes())
                .fraction(ro.getFraction())
                .classesPerNode(ro.getClassesPerNode())
                .lowProb(ro.getLowProb())
                .numSteps(ro.getNumSteps())
                .epochs(ro.getEpochs())
                .status(Status.NOT_STARTED)
                .build();
        save(task);
        return task.getId();
    }
}
