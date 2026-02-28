package ynu.jackielinn.server.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.dto.message.ClientMessage;
import ynu.jackielinn.server.dto.message.RoundMessage;
import ynu.jackielinn.server.dto.message.StatusMessage;
import ynu.jackielinn.server.entity.Client;
import ynu.jackielinn.server.entity.Round;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.ClientService;
import ynu.jackielinn.server.service.RedisSubscriptionService;
import ynu.jackielinn.server.service.RoundService;
import ynu.jackielinn.server.service.TaskService;
import ynu.jackielinn.server.service.TrainingMessageHandler;
import ynu.jackielinn.server.websocket.WebSocketSessionManager;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 训练消息处理实现：先写 MySQL（Round/Client 及 Task 状态），再推 WebSocket。
 * Client 消息先于 Round 到达，故按 (taskId, roundNum) 先建占位 Round 再写 Client；Round 消息到达后更新该 Round 指标。
 */
@Slf4j
@Service
public class TrainingMessageHandlerImpl implements TrainingMessageHandler {

    @Resource
    RoundService roundService;

    @Resource
    ClientService clientService;

    @Resource
    TaskService taskService;

    @Resource
    WebSocketSessionManager sessionManager;

    @Resource
    private ApplicationContext applicationContext;

    private final ConcurrentHashMap<Long, Long> lastRoundTime = new ConcurrentHashMap<>();

    /**
     * 按 (taskId, roundNum) 加锁，避免 Client 与 Round 消息并发时重复创建 Round
     */
    private final ConcurrentHashMap<String, Object> roundLocks = new ConcurrentHashMap<>();

    /**
     * 按 (taskId, roundNum) 获取锁对象，用于 Client 与 Round 消息并发时串行化。
     *
     * @param taskId   任务 id
     * @param roundNum 轮次编号
     * @return 锁对象
     */
    private Object lockFor(Long taskId, Integer roundNum) {
        return roundLocks.computeIfAbsent(taskId + ":" + roundNum, k -> new Object());
    }

    /**
     * 处理轮次消息：写入或更新 Round，更新 Task 状态与指标，写库后推送 WebSocket。
     *
     * @param message 轮次指标消息
     */
    @Override
    public void handleRoundMessage(RoundMessage message) {
        try {
            lastRoundTime.put(message.getTaskId(), System.currentTimeMillis());

            synchronized (lockFor(message.getTaskId(), message.getRoundNum())) {
                Round round = roundService.getByTidAndRoundNum(message.getTaskId(), message.getRoundNum());
                if (round != null) {
                    Round partialRound = Round.builder()
                            .id(round.getId())
                            .loss(message.getLoss())
                            .accuracy(message.getAccuracy())
                            .precision(message.getPrecision())
                            .recall(message.getRecall())
                            .f1Score(message.getF1Score())
                            .build();
                    roundService.updateById(partialRound);
                } else {
                    Round newRound = Round.builder()
                            .tid(message.getTaskId())
                            .roundNum(message.getRoundNum())
                            .loss(message.getLoss())
                            .accuracy(message.getAccuracy())
                            .precision(message.getPrecision())
                            .recall(message.getRecall())
                            .f1Score(message.getF1Score())
                            .build();
                    roundService.saveRound(newRound);
                }
            }

            Task task = taskService.getById(message.getTaskId());
            Integer numSteps = task != null ? task.getNumSteps() : null;
            boolean isLastRound = numSteps != null && message.getRoundNum() != null
                    && message.getRoundNum().equals(numSteps - 1);
            if (task != null && isLastRound) {
                Task partialTask = Task.builder().id(task.getId()).status(Status.SUCCESS).build();
                taskService.updateById(partialTask);
                log.info("Task {} completed successfully (last round)", message.getTaskId());
            } else if (task != null && task.getStatus() != Status.IN_PROGRESS) {
                Task partialTask = Task.builder().id(task.getId()).status(Status.IN_PROGRESS).build();
                taskService.updateById(partialTask);
            }
            if (task != null && message.getAccuracy() != null) {
                if (task.getAccuracy() == null || message.getAccuracy() > task.getAccuracy()) {
                    Task partialTask = Task.builder()
                            .id(task.getId())
                            .accuracy(message.getAccuracy())
                            .loss(message.getLoss())
                            .precision(message.getPrecision())
                            .recall(message.getRecall())
                            .f1Score(message.getF1Score())
                            .build();
                    taskService.updateById(partialTask);
                }
            }

            sessionManager.sendToTask(message.getTaskId(), message);
        } catch (Exception e) {
            log.error("Failed to handle round message for task {}: {}", message.getTaskId(), e.getMessage(), e);
        }
    }

    /**
     * 处理客户端消息：按 (taskId, roundNum) 加锁，无 Round 则先建占位再写 Client，写库后推送 WebSocket。
     *
     * @param message 客户端指标消息
     */
    @Override
    public void handleClientMessage(ClientMessage message) {
        try {
            Round round;
            synchronized (lockFor(message.getTaskId(), message.getRoundNum())) {
                round = roundService.getByTidAndRoundNum(message.getTaskId(), message.getRoundNum());
                if (round == null) {
                    round = Round.builder()
                            .tid(message.getTaskId())
                            .roundNum(message.getRoundNum())
                            .loss(0.0)
                            .accuracy(0.0)
                            .precision(0.0)
                            .recall(0.0)
                            .f1Score(0.0)
                            .build();
                    roundService.saveRound(round);
                }

                Client client = Client.builder()
                        .rid(round.getId())
                        .clientIndex(message.getClientIndex())
                        .loss(message.getLoss())
                        .accuracy(message.getAccuracy())
                        .precision(message.getPrecision())
                        .recall(message.getRecall())
                        .f1Score(message.getF1Score())
                        .timestamp(parseTimestamp(message.getTimestamp()))
                        .build();
                clientService.saveClient(client);
            }

            sessionManager.sendToTask(message.getTaskId(), message);
        } catch (Exception e) {
            log.error("Failed to handle client message for task {}: {}", message.getTaskId(), e.getMessage(), e);
        }
    }

    /**
     * 处理状态消息：更新 Task 状态，终态时取消 Redis 订阅并推送 WebSocket。
     *
     * @param message 状态消息
     */
    @Override
    public void handleStatusMessage(StatusMessage message) {
        try {
            Status status = toStatus(message.getStatus());
            if (status == null) {
                log.warn("Unknown status: {}", message.getStatus());
                return;
            }

            Task task = taskService.getById(message.getTaskId());
            if (task != null) {
                Task partialTask = Task.builder().id(task.getId()).status(status).build();
                taskService.updateById(partialTask);
                log.info("Task {} status updated to {}", message.getTaskId(), status);
                if (status == Status.SUCCESS || status == Status.FAILED || status == Status.CANCELLED) {
                    lastRoundTime.remove(message.getTaskId());
                    applicationContext.getBean(RedisSubscriptionService.class).unsubscribeTask(message.getTaskId());
                }
            }

            sessionManager.sendToTask(message.getTaskId(), message);
        } catch (Exception e) {
            log.error("Failed to handle status message for task {}: {}", message.getTaskId(), e.getMessage(), e);
        }
    }

    /**
     * 将 Redis 消息中的状态字符串转为 Status 枚举。
     *
     * @param s 状态字符串（IN_PROGRESS/SUCCESS/FAILED/CANCELLED）
     * @return 对应 Status，无法解析返回 null
     */
    private static Status toStatus(String s) {
        if (s == null) {
            return null;
        }
        return switch (s) {
            case "IN_PROGRESS" -> Status.IN_PROGRESS;
            case "SUCCESS" -> Status.SUCCESS;
            case "FAILED" -> Status.FAILED;
            case "CANCELLED" -> Status.CANCELLED;
            default -> null;
        };
    }

    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 将 Python 发来的 ISO 时间戳转为 LocalDateTime，与 Java 写入 MySQL 兼容。
     *
     * @param timestamp ISO 格式时间戳字符串
     * @return LocalDateTime，解析失败返回当前时间
     */
    private static LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            if (timestamp.contains("Z") || timestamp.indexOf('+') > 10 || (timestamp.lastIndexOf('-') > 10 && timestamp.contains("T"))) {
                return ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
            }
            return LocalDateTime.parse(timestamp, ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
