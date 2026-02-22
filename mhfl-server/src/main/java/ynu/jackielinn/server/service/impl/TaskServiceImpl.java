package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.mapper.TaskMapper;
import ynu.jackielinn.server.service.TaskService;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {
}
