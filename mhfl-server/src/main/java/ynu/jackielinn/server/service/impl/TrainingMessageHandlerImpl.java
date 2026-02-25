package ynu.jackielinn.server.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.dto.message.ClientMessage;
import ynu.jackielinn.server.dto.message.RoundMessage;
import ynu.jackielinn.server.dto.message.StatusMessage;
import ynu.jackielinn.server.entity.Client;
import ynu.jackielinn.server.entity.Round;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.ClientService;
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

    private final ConcurrentHashMap<Long, Long> lastRoundTime = new ConcurrentHashMap<>();

    @Override
    public void handleRoundMessage(RoundMessage message) {
        try {
            lastRoundTime.put(message.getTaskId(), System.currentTimeMillis());

            Round round = roundService.getByTidAndRoundNum(message.getTaskId(), message.getRoundNum());
            if (round != null) {
                round.setLoss(message.getLoss());
                round.setAccuracy(message.getAccuracy());
                round.setPrecision(message.getPrecision());
                round.setRecall(message.getRecall());
                round.setF1Score(message.getF1Score());
                roundService.updateById(round);
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

            Task task = taskService.getById(message.getTaskId());
            if (task != null && message.getRoundNum().equals(task.getNumSteps())) {
                task.setStatus(Status.SUCCESS);
                taskService.updateById(task);
                log.info("Task {} completed successfully (last round)", message.getTaskId());
            } else if (task != null && task.getStatus() != Status.IN_PROGRESS) {
                task.setStatus(Status.IN_PROGRESS);
                taskService.updateById(task);
            }
            if (task != null && message.getAccuracy() != null) {
                if (task.getAccuracy() == null || message.getAccuracy() > task.getAccuracy()) {
                    task.setAccuracy(message.getAccuracy());
                    task.setLoss(message.getLoss());
                    task.setPrecision(message.getPrecision());
                    task.setRecall(message.getRecall());
                    task.setF1Score(message.getF1Score());
                    taskService.updateById(task);
                }
            }

            sessionManager.sendToTask(message.getTaskId(), message);
        } catch (Exception e) {
            log.error("Failed to handle round message for task {}: {}", message.getTaskId(), e.getMessage(), e);
        }
    }

    @Override
    public void handleClientMessage(ClientMessage message) {
        try {
            Round round = roundService.getByTidAndRoundNum(message.getTaskId(), message.getRoundNum());
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

            sessionManager.sendToTask(message.getTaskId(), message);
        } catch (Exception e) {
            log.error("Failed to handle client message for task {}: {}", message.getTaskId(), e.getMessage(), e);
        }
    }

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
                task.setStatus(status);
                taskService.updateById(task);
                log.info("Task {} status updated to {}", message.getTaskId(), status);
                if (status == Status.SUCCESS || status == Status.FAILED || status == Status.CANCELLED) {
                    lastRoundTime.remove(message.getTaskId());
                }
            }

            sessionManager.sendToTask(message.getTaskId(), message);
        } catch (Exception e) {
            log.error("Failed to handle status message for task {}: {}", message.getTaskId(), e.getMessage(), e);
        }
    }

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
