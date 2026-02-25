package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.dto.request.CreateTaskRO;
import ynu.jackielinn.server.dto.request.ListTaskRO;
import ynu.jackielinn.server.dto.response.TaskVO;
import ynu.jackielinn.server.entity.Account;
import ynu.jackielinn.server.entity.Algorithm;
import ynu.jackielinn.server.entity.Dataset;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.mapper.TaskMapper;
import ynu.jackielinn.server.service.AccountService;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.DatasetService;
import ynu.jackielinn.server.service.TaskService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Resource
    private DatasetService datasetService;

    @Resource
    private AlgorithmService algorithmService;

    @Resource
    private AccountService accountService;

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

    @Override
    public String deleteTask(Long id, Long currentUserId, boolean isAdmin) {
        Task task = getById(id);
        if (task == null) {
            return "任务不存在";
        }
        if (!isAdmin && !task.getUid().equals(currentUserId)) {
            return "无权限删除该任务";
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Task> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Task::getId, id)
                .set(Task::getDeleted, 1)
                .set(Task::getDeleteTime, now);
        return update(updateWrapper) ? null : "删除失败，请联系管理员";
    }

    @Override
    public String setRecommend(Long id) {
        Task task = getById(id);
        if (task == null) {
            return "任务不存在";
        }
        Status status = task.getStatus();
        if (status != Status.SUCCESS && status != Status.RECOMMENDED) {
            return "只有训练成功(SUCCESS)或已推荐(RECOMMENDED)的任务才能设置推荐";
        }
        Status newStatus = status == Status.SUCCESS ? Status.RECOMMENDED : Status.SUCCESS;
        task.setStatus(newStatus);
        return updateById(task) ? null : "设置失败，请联系管理员";
    }

    @Override
    public IPage<TaskVO> listTasks(ListTaskRO ro, Long currentUserId, boolean isAdmin) {
        long current = ro.getCurrent() != null ? ro.getCurrent() : 1L;
        long size = ro.getSize() != null ? ro.getSize() : 10L;
        Page<Task> page = new Page<>(current, size);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();

        // 非管理员只查当前用户的任务
        if (!isAdmin) {
            wrapper.eq(Task::getUid, currentUserId);
        }

        // 关键字叠加搜索：数据集名、算法名；管理员还可按用户名
        if (ro.getKeyword() != null && !ro.getKeyword().trim().isEmpty()) {
            String keyword = ro.getKeyword().trim();
            List<Long> datasetIds = datasetService.list(
                            new LambdaQueryWrapper<Dataset>().like(Dataset::getDataName, keyword))
                    .stream()
                    .map(Dataset::getId)
                    .toList();
            List<Long> algorithmIds = algorithmService.list(
                            new LambdaQueryWrapper<Algorithm>().like(Algorithm::getAlgorithmName, keyword))
                    .stream()
                    .map(Algorithm::getId)
                    .toList();
            List<Long> accountIds = isAdmin
                    ? accountService.list(
                            new LambdaQueryWrapper<Account>().like(Account::getUsername, keyword))
                    .stream()
                    .map(Account::getId)
                    .toList()
                    : Collections.emptyList();

            if (datasetIds.isEmpty() && algorithmIds.isEmpty() && accountIds.isEmpty()) {
                wrapper.and(w -> w.eq(Task::getId, -1));
            } else {
                wrapper.and(w -> {
                    if (!datasetIds.isEmpty()) {
                        w.in(Task::getDid, datasetIds);
                    }
                    if (!algorithmIds.isEmpty()) {
                        w.or().in(Task::getAid, algorithmIds);
                    }
                    if (isAdmin && !accountIds.isEmpty()) {
                        w.or().in(Task::getUid, accountIds);
                    }
                });
            }
        }

        // 创建时间范围
        LocalDate startDate = ro.getStartTime();
        LocalDate endDate = ro.getEndTime();
        if (startDate != null && endDate != null) {
            LocalDateTime startTime = startDate.atStartOfDay();
            LocalDateTime endTime = endDate.atTime(23, 59, 59);
            wrapper.between(Task::getCreateTime, startTime, endTime);
        } else if (startDate != null) {
            wrapper.ge(Task::getCreateTime, startDate.atStartOfDay());
        } else if (endDate != null) {
            wrapper.le(Task::getCreateTime, endDate.atTime(23, 59, 59));
        }

        wrapper.orderByAsc(Task::getId);

        IPage<Task> taskPage = page(page, wrapper);
        List<TaskVO> voList = toTaskVOList(taskPage.getRecords());

        Page<TaskVO> voPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private List<TaskVO> toTaskVOList(List<Task> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Set<Long> dids = records.stream().map(Task::getDid).collect(Collectors.toSet());
        Set<Long> aids = records.stream().map(Task::getAid).collect(Collectors.toSet());
        Set<Long> uids = records.stream().map(Task::getUid).collect(Collectors.toSet());

        Map<Long, String> dataNameMap = datasetService.listByIds(dids).stream()
                .collect(Collectors.toMap(Dataset::getId, Dataset::getDataName));
        Map<Long, String> algorithmNameMap = algorithmService.listByIds(aids).stream()
                .collect(Collectors.toMap(Algorithm::getId, Algorithm::getAlgorithmName));
        Map<Long, String> usernameMap = accountService.listByIds(uids).stream()
                .collect(Collectors.toMap(Account::getId, Account::getUsername));

        return records.stream()
                .map(task -> task.asViewObject(TaskVO.class, vo -> {
                    vo.setDataName(dataNameMap.get(task.getDid()));
                    vo.setAlgorithmName(algorithmNameMap.get(task.getAid()));
                    vo.setUsername(usernameMap.get(task.getUid()));
                }))
                .toList();
    }
}
