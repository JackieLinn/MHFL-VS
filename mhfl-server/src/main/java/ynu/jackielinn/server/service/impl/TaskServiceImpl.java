package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ynu.jackielinn.server.dto.request.TrainStartRO;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.dto.request.CreateTaskRO;
import ynu.jackielinn.server.dto.request.ListTaskRO;
import ynu.jackielinn.server.dto.response.ClientVO;
import ynu.jackielinn.server.dto.response.RoundVO;
import ynu.jackielinn.server.dto.response.TaskVO;
import ynu.jackielinn.server.entity.Account;
import ynu.jackielinn.server.entity.Algorithm;
import ynu.jackielinn.server.entity.Client;
import ynu.jackielinn.server.entity.Dataset;
import ynu.jackielinn.server.entity.Round;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.mapper.TaskMapper;
import ynu.jackielinn.server.dto.message.StatusMessage;
import ynu.jackielinn.server.service.AccountService;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.DatasetService;
import ynu.jackielinn.server.service.RedisSubscriptionService;
import ynu.jackielinn.server.service.ClientService;
import ynu.jackielinn.server.service.RoundService;
import ynu.jackielinn.server.service.TaskService;
import ynu.jackielinn.server.websocket.WebSocketSessionManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Value("${python.fastapi.url:http://localhost:8000}")
    private String pythonFastApiUrl;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private WebSocketSessionManager sessionManager;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private RoundService roundService;

    @Resource
    private ClientService clientService;

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

    @Override
    public TaskVO getTaskDetail(Long id, Long currentUserId, boolean isAdmin) {
        Task task = getById(id);
        if (task == null) {
            return null;
        }
        if (!task.getUid().equals(currentUserId)
                && task.getStatus() != Status.RECOMMENDED
                && !isAdmin) {
            return null;
        }
        Dataset ds = datasetService.getById(task.getDid());
        Algorithm algo = algorithmService.getById(task.getAid());
        Account account = accountService.getById(task.getUid());
        final String dataName = ds != null ? ds.getDataName() : null;
        final String algorithmName = algo != null ? algo.getAlgorithmName() : null;
        final String username = account != null ? account.getUsername() : null;
        return task.asViewObject(TaskVO.class, vo -> {
            vo.setDataName(dataName);
            vo.setAlgorithmName(algorithmName);
            vo.setUsername(username);
        });
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

    @Override
    public String startTask(Long taskId, Long currentUserId) {
        Task task = getById(taskId);
        if (task == null) {
            return "任务不存在";
        }
        if (!task.getUid().equals(currentUserId)) {
            return "无权限启动该任务（仅任务创建者可启动）";
        }
        if (task.getStatus() != Status.NOT_STARTED) {
            if (task.getStatus() == Status.IN_PROGRESS) {
                return "任务已在训练中";
            }
            return "该任务已训练过，不能再次训练";
        }
        Dataset dataset = datasetService.getById(task.getDid());
        if (dataset == null) {
            return "数据集不存在";
        }
        Algorithm algorithm = algorithmService.getById(task.getAid());
        if (algorithm == null) {
            return "算法不存在";
        }
        TrainStartRO ro = TrainStartRO.builder()
                .taskId(task.getId().intValue())
                .dataName(dataset.getDataName())
                .algorithmName(algorithm.getAlgorithmName())
                .numNodes(task.getNumNodes())
                .fraction(task.getFraction())
                .classesPerNode(task.getClassesPerNode())
                .lowProb(task.getLowProb())
                .numSteps(task.getNumSteps())
                .epochs(task.getEpochs())
                .build();
        String url = pythonFastApiUrl + "/api/train/start";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TrainStartRO> entity = new HttpEntity<>(ro, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "启动训练失败，请联系管理员";
            }
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(response.getBody());
            if (json.getIntValue("code") != 200) {
                return json.getString("message") != null ? json.getString("message") : "启动训练失败";
            }
            task.setStatus(Status.IN_PROGRESS);
            updateById(task);
            applicationContext.getBean(RedisSubscriptionService.class).subscribeTask(taskId);
            return null;
        } catch (Exception e) {
            return "调用训练服务失败: " + e.getMessage();
        }
    }

    @Override
    public String stopTask(Long taskId, Long currentUserId) {
        Task task = getById(taskId);
        if (task == null) {
            return "任务不存在";
        }
        if (!task.getUid().equals(currentUserId)) {
            return "无权限停止该任务（仅任务创建者可停止）";
        }
        if (task.getStatus() != Status.IN_PROGRESS) {
            return "任务未在训练中，无需停止";
        }
        String url = pythonFastApiUrl + "/api/train/stop/" + taskId;
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "停止训练失败，请联系管理员";
            }
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(response.getBody());
            if (json.getIntValue("code") != 200) {
                return json.getString("message") != null ? json.getString("message") : "任务未在运行或停止失败";
            }
        } catch (Exception e) {
            return "调用训练服务失败: " + e.getMessage();
        }
        task.setStatus(Status.CANCELLED);
        updateById(task);
        StatusMessage statusMessage = StatusMessage.builder()
                .taskId(taskId)
                .status("CANCELLED")
                .message("用户停止训练")
                .timestamp(Instant.now().toString())
                .build();
        sessionManager.sendToTask(taskId, statusMessage);
        sessionManager.closeAllSessionsForTask(taskId);
        return null;
    }

    @Override
    public List<RoundVO> getTaskRounds(Long taskId, Long currentUserId, boolean isAdmin) {
        Task task = getById(taskId);
        if (task == null) {
            return null;
        }
        if (!task.getUid().equals(currentUserId)
                && task.getStatus() != Status.RECOMMENDED
                && !isAdmin) {
            return null;
        }
        List<Round> rounds = roundService.listByTidOrderByRoundNum(taskId);
        return rounds.stream()
                .map(r -> r.asViewObject(RoundVO.class))
                .toList();
    }

    @Override
    public List<ClientVO> getTaskClientsLatest(Long taskId, Long currentUserId, boolean isAdmin) {
        Task task = getById(taskId);
        if (task == null) {
            return null;
        }
        if (!task.getUid().equals(currentUserId)
                && task.getStatus() != Status.RECOMMENDED
                && !isAdmin) {
            return null;
        }
        int numNodes = task.getNumNodes() != null ? task.getNumNodes() : 0;
        List<Round> rounds = roundService.listByTidOrderByRoundNum(taskId);
        List<Long> rids = rounds.stream().map(Round::getId).toList();
        Map<Long, Integer> ridToRoundNum = rounds.stream().collect(Collectors.toMap(Round::getId, Round::getRoundNum));
        List<ClientVO> result = new ArrayList<>(numNodes);
        for (int i = 0; i < numNodes; i++) {
            Client c = clientService.getLatestByRidsAndClientIndex(rids, i);
            if (c == null) {
                result.add(ClientVO.builder()
                        .id(null)
                        .roundNum(-1)
                        .clientIndex(i)
                        .loss(-1.0)
                        .accuracy(-1.0)
                        .precision(-1.0)
                        .recall(-1.0)
                        .f1Score(-1.0)
                        .timestamp(null)
                        .build());
            } else {
                final Integer roundNum = c.getRid() != null ? ridToRoundNum.getOrDefault(c.getRid(), -1) : -1;
                result.add(c.asViewObject(ClientVO.class, vo -> vo.setRoundNum(roundNum)));
            }
        }
        return result;
    }

    @Override
    public List<ClientVO> getTaskClientDetail(Long taskId, Integer clientIndex, Long currentUserId, boolean isAdmin) {
        Task task = getById(taskId);
        if (task == null) {
            return null;
        }
        if (!task.getUid().equals(currentUserId)
                && task.getStatus() != Status.RECOMMENDED
                && !isAdmin) {
            return null;
        }
        List<Round> rounds = roundService.listByTidOrderByRoundNum(taskId);
        List<Long> rids = rounds.stream().map(Round::getId).toList();
        Map<Long, Integer> ridToRoundNum = rounds.stream().collect(Collectors.toMap(Round::getId, Round::getRoundNum));
        List<Client> clients = clientService.listByRidsAndClientIndex(rids, clientIndex);
        List<ClientVO> list = new ArrayList<>(clients.stream()
                .map(c -> {
                    final Integer roundNum = c.getRid() != null ? ridToRoundNum.getOrDefault(c.getRid(), -1) : -1;
                    return c.asViewObject(ClientVO.class, vo -> vo.setRoundNum(roundNum));
                })
                .toList());
        list.sort((a, b) -> {
            int ra = a.getRoundNum() != null ? a.getRoundNum() : -1;
            int rb = b.getRoundNum() != null ? b.getRoundNum() : -1;
            return Integer.compare(ra, rb);
        });
        return list;
    }
}
