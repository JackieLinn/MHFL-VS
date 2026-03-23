package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import ynu.jackielinn.server.common.Feedback;
import ynu.jackielinn.server.dto.request.ChatRequestRO;
import ynu.jackielinn.server.dto.request.FeedbackRO;
import ynu.jackielinn.server.dto.request.ListAlgorithmRO;
import ynu.jackielinn.server.dto.request.ListDatasetRO;
import ynu.jackielinn.server.dto.request.UpdateConversationRO;
import ynu.jackielinn.server.dto.response.AlgorithmVO;
import ynu.jackielinn.server.dto.response.ChatResponseVO;
import ynu.jackielinn.server.dto.response.ConversationDetailVO;
import ynu.jackielinn.server.dto.response.ConversationVO;
import ynu.jackielinn.server.dto.response.DashboardStatCardsVO;
import ynu.jackielinn.server.dto.response.DatasetVO;
import ynu.jackielinn.server.dto.response.TaskVO;
import ynu.jackielinn.server.entity.Conversation;
import ynu.jackielinn.server.entity.Message;
import ynu.jackielinn.server.mapper.ConversationMapper;
import ynu.jackielinn.server.mapper.MessageMapper;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.DashboardService;
import ynu.jackielinn.server.service.DatasetService;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceImplTest {

    @Spy
    @InjectMocks
    private AssistantServiceImpl service;

    @Mock
    private ConversationMapper conversationMapper;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WebClient assistantWebClient;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private AlgorithmService algorithmService;

    @Mock
    private DatasetService datasetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pythonFastApiUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(service, "assistantStreamEmitterTimeoutMs", 1000L);
        ReflectionTestUtils.setField(service, "assistantChatStreamExecutor", (Executor) Runnable::run);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createConversationShouldReuseEmptyConversation() {
        Conversation empty = Conversation.builder().id(11L).uid(1L).messageCount(0).build();
        when(conversationMapper.selectOne(any())).thenReturn(empty);

        var vo = service.createConversation(1L);

        assertThat(vo.getId()).isEqualTo(11L);
        verify(conversationMapper, never()).insert(any(Conversation.class));
    }

    @Test
    void createConversationShouldInsertWhenNoEmptyConversation() {
        when(conversationMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(22L);
            return 1;
        }).when(conversationMapper).insert(any(Conversation.class));

        var vo = service.createConversation(2L);

        assertThat(vo.getId()).isEqualTo(22L);
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationMapper).insert(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("新建对话");
        assertThat(captor.getValue().getMessageCount()).isEqualTo(0);
    }

    @Test
    void listByUserIdShouldReturnConversationListWithPreviewAndKeyword() {
        Conversation c1 = Conversation.builder().id(1L).uid(9L).title("abc").messageCount(2).updateTime(LocalDateTime.now()).build();
        when(conversationMapper.selectList(any())).thenReturn(List.of(c1));
        when(messageMapper.selectOne(any())).thenReturn(Message.builder().content("hello").sequenceNum(2).build());

        List<ConversationVO> list = service.listByUserId(9L, "  abc  ");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getPreview()).isEqualTo("hello");
    }

    @Test
    void listByUserIdShouldTruncateLongPreview() {
        Conversation c1 = Conversation.builder().id(1L).uid(9L).title("abc").messageCount(2).updateTime(LocalDateTime.now()).build();
        when(conversationMapper.selectList(any())).thenReturn(List.of(c1));
        when(messageMapper.selectOne(any())).thenReturn(Message.builder().content("x".repeat(81)).sequenceNum(2).build());

        List<ConversationVO> list = service.listByUserId(9L, null);

        assertThat(list.get(0).getPreview()).hasSize(83);
        assertThat(list.get(0).getPreview()).endsWith("...");
    }

    @Test
    void getDetailShouldReturnNullWhenConversationMissingOrUnauthorized() {
        when(conversationMapper.selectById(1L)).thenReturn(null);
        assertThat(service.getDetail(1L, 1L)).isNull();

        when(conversationMapper.selectById(1L)).thenReturn(Conversation.builder().id(1L).uid(2L).build());
        assertThat(service.getDetail(1L, 1L)).isNull();
    }

    @Test
    void getDetailShouldMapMessagesAndParseSourcesAndDefaultFeedback() {
        Conversation conv = Conversation.builder().id(1L).uid(1L).title("t").messageCount(2).build();
        Message m1 = Message.builder().id(10L).cid(1L).role("assistant").content("a").sequenceNum(1).sourcesJson("[\"s1\",null]").feedback(null).build();
        Message m2 = Message.builder().id(11L).cid(1L).role("assistant").content("b").sequenceNum(2).sourcesJson("{bad").feedback(Feedback.LIKED).build();
        when(conversationMapper.selectById(1L)).thenReturn(conv);
        when(messageMapper.selectList(any())).thenReturn(List.of(m1, m2));

        ConversationDetailVO detail = service.getDetail(1L, 1L);

        assertThat(detail).isNotNull();
        assertThat(detail.getMessages()).hasSize(2);
        assertThat(detail.getMessages().get(0).getSources()).containsExactly("s1");
        assertThat(detail.getMessages().get(0).getFeedback()).isEqualTo(Feedback.NONE);
        assertThat(detail.getMessages().get(1).getSources()).isEmpty();
        assertThat(detail.getMessages().get(1).getFeedback()).isEqualTo(Feedback.LIKED);
    }

    @Test
    void deleteConversationShouldHandleMissingUnauthorizedAndUpdateFail() {
        when(conversationMapper.selectById(1L)).thenReturn(null);
        assertThat(service.deleteConversation(1L, 1L)).isNotNull();

        when(conversationMapper.selectById(1L)).thenReturn(Conversation.builder().id(1L).uid(2L).build());
        assertThat(service.deleteConversation(1L, 1L)).isNotNull();

        when(conversationMapper.selectById(1L)).thenReturn(Conversation.builder().id(1L).uid(1L).build());
        when(conversationMapper.update(eq(null), any())).thenReturn(0);
        assertThat(service.deleteConversation(1L, 1L)).isNotNull();
    }

    @Test
    void deleteConversationShouldReturnNullWhenSuccess() {
        when(conversationMapper.selectById(1L)).thenReturn(Conversation.builder().id(1L).uid(1L).build());
        when(conversationMapper.update(eq(null), any())).thenReturn(1);

        assertThat(service.deleteConversation(1L, 1L)).isNull();
    }

    @Test
    void updateTitleShouldCoverBranches() {
        when(conversationMapper.selectById(1L)).thenReturn(null);
        assertThat(service.updateTitle(1L, 1L, "x")).isNotNull();

        when(conversationMapper.selectById(1L)).thenReturn(Conversation.builder().id(1L).uid(2L).build());
        assertThat(service.updateTitle(1L, 1L, "x")).isNotNull();

        when(conversationMapper.selectById(1L)).thenReturn(Conversation.builder().id(1L).uid(1L).build());
        when(conversationMapper.updateById(any(Conversation.class))).thenReturn(0, 1);
        assertThat(service.updateTitle(1L, 1L, "x")).isNotNull();
        assertThat(service.updateTitle(1L, 1L, "x")).isNull();
    }

    @Test
    void updateMessageFeedbackShouldCoverAllBranches() {
        when(messageMapper.selectById(9L)).thenReturn(null);
        assertThat(service.updateMessageFeedback(9L, 1L, FeedbackRO.builder().feedback(1).build())).isNotNull();

        Message msg = Message.builder().id(9L).cid(100L).role("assistant").build();
        when(messageMapper.selectById(9L)).thenReturn(msg);
        when(conversationMapper.selectById(100L)).thenReturn(null);
        assertThat(service.updateMessageFeedback(9L, 1L, FeedbackRO.builder().feedback(1).build())).isNotNull();

        when(conversationMapper.selectById(100L)).thenReturn(Conversation.builder().id(100L).uid(2L).build());
        assertThat(service.updateMessageFeedback(9L, 1L, FeedbackRO.builder().feedback(1).build())).isNotNull();

        when(conversationMapper.selectById(100L)).thenReturn(Conversation.builder().id(100L).uid(1L).build());
        when(messageMapper.selectById(9L)).thenReturn(Message.builder().id(9L).cid(100L).role("user").build());
        assertThat(service.updateMessageFeedback(9L, 1L, FeedbackRO.builder().feedback(1).build())).isNotNull();

        when(messageMapper.selectById(9L)).thenReturn(Message.builder().id(9L).cid(100L).role("assistant").build());
        when(messageMapper.updateById(any(Message.class))).thenReturn(0, 1);
        assertThat(service.updateMessageFeedback(9L, 1L, FeedbackRO.builder().feedback(-1).build())).isNotNull();
        assertThat(service.updateMessageFeedback(9L, 1L, FeedbackRO.builder().feedback(-1).build())).isNull();
    }

    @Test
    void getOrCreateConversationShouldHandleCidBranches() {
        when(conversationMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.getOrCreateConversation(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        when(conversationMapper.selectById(1L)).thenReturn(Conversation.builder().id(1L).uid(2L).build());
        assertThatThrownBy(() -> service.getOrCreateConversation(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        when(conversationMapper.selectById(1L)).thenReturn(Conversation.builder().id(1L).uid(1L).build());
        assertThat(service.getOrCreateConversation(1L, 1L).getId()).isEqualTo(1L);
    }

    @Test
    void getOrCreateConversationShouldReuseOrInsertWhenCidNull() {
        when(conversationMapper.selectOne(any())).thenReturn(Conversation.builder().id(3L).uid(1L).messageCount(0).build());
        assertThat(service.getOrCreateConversation(null, 1L).getId()).isEqualTo(3L);

        when(conversationMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(4L);
            return 1;
        }).when(conversationMapper).insert(any(Conversation.class));
        assertThat(service.getOrCreateConversation(null, 1L).getId()).isEqualTo(4L);
    }

    @Test
    void getNextSequenceNumShouldReturnOneOrLastPlusOne() {
        when(messageMapper.selectOne(any())).thenReturn(null, Message.builder().sequenceNum(7).build());
        assertThat(service.getNextSequenceNum(1L)).isEqualTo(1);
        assertThat(service.getNextSequenceNum(1L)).isEqualTo(8);
    }

    @Test
    void saveMessageShouldUseDefaultFeedbackWhenNull() {
        service.saveMessage(ynu.jackielinn.server.dto.request.SaveMessageRO.builder()
                .cid(1L).role("assistant").content("c").sequenceNum(1).feedback(null).build());

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(captor.capture());
        assertThat(captor.getValue().getFeedback()).isEqualTo(Feedback.NONE);
    }

    @Test
    void updateConversationShouldOnlySetNonNullFields() {
        service.updateConversation(UpdateConversationRO.builder().id(1L).messageCount(2).build());

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getMessageCount()).isEqualTo(2);
        assertThat(captor.getValue().getTitle()).isNull();
        assertThat(captor.getValue().getSummary()).isNull();
    }

    @Test
    void updateConversationShouldSetAllFieldsWhenProvided() {
        service.updateConversation(UpdateConversationRO.builder()
                .id(2L)
                .messageCount(3)
                .title("t")
                .summary("s")
                .build());

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(2L);
        assertThat(captor.getValue().getMessageCount()).isEqualTo(3);
        assertThat(captor.getValue().getTitle()).isEqualTo("t");
        assertThat(captor.getValue().getSummary()).isEqualTo("s");
    }

    @Test
    void chatShouldRunHappyPathAndUpdateTitleWhenDefault() {
        Conversation conv = Conversation.builder().id(9L).uid(1L).title("新建对话").messageCount(0).build();
        doReturn(conv).when(service).getOrCreateConversation(null, 1L);
        doReturn(1).when(service).getNextSequenceNum(9L);
        when(messageMapper.selectList(any())).thenReturn(List.of());
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"needs_tasks\":false,\"needs_algorithms\":false,\"needs_datasets\":false,\"needs_kb\":true}}"));
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/chat"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"content\":\"answer\",\"sources\":[\"s1\"]}}"));
        when(conversationMapper.updateById(any(Conversation.class))).thenReturn(1);

        ChatResponseVO vo = service.chat(1L, ChatRequestRO.builder().message("abcdefghijklmnopqrstuvwxyz").build());

        assertThat(vo.getContent()).isEqualTo("answer");
        verify(messageMapper, times(2)).insert(any(Message.class));
        ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationMapper).updateById(convCaptor.capture());
        assertThat(convCaptor.getValue().getMessageCount()).isEqualTo(2);
        assertThat(convCaptor.getValue().getTitle()).isEqualTo("abcdefghijklmnopqr...");
    }

    @Test
    void chatShouldKeepTitleWhenNotDefaultAndHandleNullSeqAndSources() {
        Conversation conv = Conversation.builder().id(9L).uid(1L).title("custom").messageCount(2).build();
        doReturn(conv).when(service).getOrCreateConversation(9L, 1L);
        doReturn(null).when(service).getNextSequenceNum(9L);
        when(messageMapper.selectList(any())).thenReturn(List.of());
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"needs_tasks\":false,\"needs_algorithms\":false,\"needs_datasets\":false}}"));
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/chat"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"content\":\"answer\"}}"));
        when(conversationMapper.updateById(any(Conversation.class))).thenReturn(1);

        ChatResponseVO vo = service.chat(1L, ChatRequestRO.builder().cid(9L).message("short").build());

        assertThat(vo.getSources()).isEmpty();
        ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationMapper).updateById(convCaptor.capture());
        assertThat(convCaptor.getValue().getTitle()).isEqualTo("custom");
        assertThat(convCaptor.getValue().getMessageCount()).isEqualTo(4);
    }

    @Test
    void chatStreamShouldReturnEmitterAndHandleOuterException() {
        doThrow(new RuntimeException("boom")).when(service).getOrCreateConversation(any(), anyLong());

        SseEmitter emitter = service.chatStream(1L, ChatRequestRO.builder().cid(1L).message("m").build());

        assertThat(emitter).isNotNull();
    }

    @Test
    void privateCallPythonClassifyShouldCoverSuccessAndFallbacks() throws Exception {
        Method needsTasks = null;
        Method needsKb = null;

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"needs_tasks\":true,\"needs_algorithms\":false,\"needs_datasets\":true}}"));
        Object result = ReflectionTestUtils.invokeMethod(service, "callPythonClassify", "x");
        needsTasks = result.getClass().getDeclaredMethod("needsTasks");
        needsKb = result.getClass().getDeclaredMethod("needsKb");
        assertThat((boolean) needsTasks.invoke(result)).isTrue();
        assertThat((boolean) needsKb.invoke(result)).isTrue();

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bad"));
        Object result2 = ReflectionTestUtils.invokeMethod(service, "callPythonClassify", "x");
        assertThat((boolean) needsTasks.invoke(result2)).isFalse();
        assertThat((boolean) needsKb.invoke(result2)).isTrue();
    }

    @Test
    void privateCallPythonClassifyShouldCoverCodeDataAndExceptionBranches() throws Exception {
        Object r1;
        Object r2;
        Object r3;
        Object r4;
        Object r5;
        Method needsTasks;
        Method needsAlgorithms;
        Method needsDatasets;
        Method needsKb;

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"needs_tasks\":false,\"needs_algorithms\":false,\"needs_datasets\":false,\"needs_kb\":false}}"));
        r1 = ReflectionTestUtils.invokeMethod(service, "callPythonClassify", "x");
        needsTasks = r1.getClass().getDeclaredMethod("needsTasks");
        needsAlgorithms = r1.getClass().getDeclaredMethod("needsAlgorithms");
        needsDatasets = r1.getClass().getDeclaredMethod("needsDatasets");
        needsKb = r1.getClass().getDeclaredMethod("needsKb");
        assertThat((boolean) needsKb.invoke(r1)).isFalse();

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok((String) null));
        r2 = ReflectionTestUtils.invokeMethod(service, "callPythonClassify", "x");
        assertThat((boolean) needsTasks.invoke(r2)).isFalse();
        assertThat((boolean) needsKb.invoke(r2)).isTrue();

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"message\":\"x\"}"));
        r3 = ReflectionTestUtils.invokeMethod(service, "callPythonClassify", "x");
        assertThat((boolean) needsAlgorithms.invoke(r3)).isFalse();

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":null}"));
        r4 = ReflectionTestUtils.invokeMethod(service, "callPythonClassify", "x");
        assertThat((boolean) needsDatasets.invoke(r4)).isFalse();

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("x"));
        r5 = ReflectionTestUtils.invokeMethod(service, "callPythonClassify", "x");
        assertThat((boolean) needsKb.invoke(r5)).isTrue();
    }

    @Test
    void privateBuildContextDataShouldFetchDataWithAdminRole() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u", "p", List.of(new SimpleGrantedAuthority("ROLE_admin")))
        );
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"needs_tasks\":true,\"needs_algorithms\":true,\"needs_datasets\":true,\"needs_kb\":false}}"));
        Object classifyResult = ReflectionTestUtils.invokeMethod(service, "callPythonClassify", "x");

        List<TaskVO> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) tasks.add(TaskVO.builder().id((long) i).build());
        when(dashboardService.getRecentTasks(1L, true)).thenReturn(tasks);
        when(dashboardService.getStatCards(1L, true)).thenReturn(DashboardStatCardsVO.builder().total(1L).build());
        IPage<AlgorithmVO> algorithmPage = new Page<>(1, 10);
        algorithmPage.setRecords(List.of(AlgorithmVO.builder().id(1L).build()));
        when(algorithmService.listAlgorithms(any(ListAlgorithmRO.class))).thenReturn(algorithmPage);
        IPage<DatasetVO> datasetPage = new Page<>(1, 10);
        datasetPage.setRecords(List.of(DatasetVO.builder().id(1L).build()));
        when(datasetService.listDatasets(any(ListDatasetRO.class))).thenReturn(datasetPage);

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) ReflectionTestUtils.invokeMethod(service, "buildContextData", 1L, classifyResult);

        assertThat(ctx).containsKeys("recentTasks", "taskStats", "algorithms", "datasets");
        assertThat((List<?>) ctx.get("recentTasks")).hasSize(5);
    }

    @Test
    void privateBuildContextDataShouldReturnEmptyWhenNoNeed() {
        SecurityContextHolder.getContext().setAuthentication(null);
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"needs_tasks\":false,\"needs_algorithms\":false,\"needs_datasets\":false,\"needs_kb\":true}}"));
        Object classifyResult = ReflectionTestUtils.invokeMethod(service, "callPythonClassify", "x");

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) ReflectionTestUtils.invokeMethod(service, "buildContextData", 1L, classifyResult);

        assertThat(ctx).isEmpty();
        verify(dashboardService, never()).getRecentTasks(anyLong(), anyBoolean());
    }

    @Test
    void privateBuildContextDataShouldWorkForNonAdminAndShortTasks() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u", "p", List.of(new SimpleGrantedAuthority("ROLE_user")))
        );
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"needs_tasks\":true,\"needs_algorithms\":false,\"needs_datasets\":false}}"));
        Object classifyResult = ReflectionTestUtils.invokeMethod(service, "callPythonClassify", "x");

        when(dashboardService.getRecentTasks(2L, false)).thenReturn(List.of(TaskVO.builder().id(1L).build()));
        when(dashboardService.getStatCards(2L, false)).thenReturn(DashboardStatCardsVO.builder().total(2L).build());

        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) ReflectionTestUtils.invokeMethod(service, "buildContextData", 2L, classifyResult);
        assertThat((List<?>) ctx.get("recentTasks")).hasSize(1);
        verify(dashboardService).getRecentTasks(2L, false);
    }

    @Test
    void privateBuildMemoryContextShouldCoverEmptyRecentAndSummaryUpdate() {
        Conversation conv = Conversation.builder().id(1L).title("新建对话").summary(null).messageCount(10).build();

        when(messageMapper.selectList(any())).thenReturn(List.of());
        String empty = ReflectionTestUtils.invokeMethod(service, "buildMemoryContextAndMaybeUpdateSummary", 1L, conv, 1);
        assertThat(empty).isEqualTo("");

        Message only = Message.builder().cid(1L).role("user").content("u1").sequenceNum(1).build();
        when(messageMapper.selectList(any())).thenReturn(List.of(only));
        String one = ReflectionTestUtils.invokeMethod(service, "buildMemoryContextAndMaybeUpdateSummary", 1L, conv, 1);
        assertThat(one).contains("[最近对话]");

        List<Message> many = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            many.add(Message.builder()
                    .cid(1L)
                    .role(i % 2 == 0 ? "assistant" : "user")
                    .content("m" + i)
                    .sequenceNum(i)
                    .feedback(i % 2 == 0 ? Feedback.LIKED : null)
                    .build());
        }
        when(messageMapper.selectList(any())).thenReturn(many);
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/summarize"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"summary\":\"new-summary\"}}"));
        when(conversationMapper.updateById(any(Conversation.class))).thenReturn(1);

        String context = ReflectionTestUtils.invokeMethod(service, "buildMemoryContextAndMaybeUpdateSummary", 1L, conv, 11);

        assertThat(context).contains("new-summary");
        assertThat(conv.getSummary()).isEqualTo("new-summary");
        verify(conversationMapper, times(1)).updateById(any(Conversation.class));
    }

    @Test
    void privateBuildMemoryContextShouldCoverNoSummaryUpdateBranches() {
        Conversation conv = Conversation.builder().id(2L).title("custom").summary("old").messageCount(20).build();
        List<Message> many = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            many.add(Message.builder()
                    .cid(2L)
                    .role(i % 2 == 0 ? "assistant" : "user")
                    .content("m" + i)
                    .sequenceNum(i)
                    .feedback(null)
                    .build());
        }
        when(messageMapper.selectList(any())).thenReturn(many);
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/summarize"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"summary\":\"\"}}"));

        String context = ReflectionTestUtils.invokeMethod(service, "buildMemoryContextAndMaybeUpdateSummary", 2L, conv, 11);

        assertThat(context).contains("[历史摘要]");
        verify(conversationMapper, never()).updateById(any(Conversation.class));
    }

    @Test
    void privateBuildMemoryContextShouldSkipSummarizeWhenOlderCountOutOfRange() {
        Conversation conv = Conversation.builder().id(3L).title("custom").summary(null).messageCount(50).build();
        List<Message> small = List.of(
                Message.builder().cid(3L).role("user").content("u").sequenceNum(1).build(),
                Message.builder().cid(3L).role("assistant").content("a").sequenceNum(2).feedback(Feedback.NONE).build()
        );
        when(messageMapper.selectList(any())).thenReturn(small);

        String context = ReflectionTestUtils.invokeMethod(service, "buildMemoryContextAndMaybeUpdateSummary", 3L, conv, 30);

        assertThat(context).contains("[最近对话]");
        verify(restTemplate, never()).postForEntity(eq("http://localhost:8000/api/assistant/summarize"), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void privateCallPythonSummarizeShouldFallbackOnFailures() {
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/summarize"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bad"));
        String r1 = ReflectionTestUtils.invokeMethod(service, "callPythonSummarize", "prev", List.of());
        assertThat(r1).isEqualTo("prev");

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/summarize"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":500,\"message\":\"e\"}"));
        String r2 = ReflectionTestUtils.invokeMethod(service, "callPythonSummarize", "prev", List.of());
        assertThat(r2).isEqualTo("prev");

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/summarize"), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("boom"));
        String r3 = ReflectionTestUtils.invokeMethod(service, "callPythonSummarize", "prev", List.of());
        assertThat(r3).isEqualTo("prev");
    }

    @Test
    void privateCallPythonSummarizeShouldCoverBodyCodeDataAndSummaryNull() {
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/summarize"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok((String) null));
        String r1 = ReflectionTestUtils.invokeMethod(service, "callPythonSummarize", "prev", List.of());
        assertThat(r1).isEqualTo("prev");

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/summarize"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"message\":\"x\"}"));
        String r2 = ReflectionTestUtils.invokeMethod(service, "callPythonSummarize", "prev", List.of());
        assertThat(r2).isEqualTo("prev");

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/summarize"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":null}"));
        String r3 = ReflectionTestUtils.invokeMethod(service, "callPythonSummarize", "prev", List.of());
        assertThat(r3).isEqualTo("prev");

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/summarize"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"summary\":null}}"));
        String r4 = ReflectionTestUtils.invokeMethod(service, "callPythonSummarize", "prev", List.of());
        assertThat(r4).isEqualTo("prev");
    }

    @Test
    void privateCallPythonChatShouldReturnContentAndThrowOnErrors() {
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/chat"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"content\":\"ok\",\"sources\":[\"s1\",null]}}"));
        ChatResponseVO ok = ReflectionTestUtils.invokeMethod(service, "callPythonChat", "m", Map.of(), true, "");
        assertThat(ok.getContent()).isEqualTo("ok");
        assertThat(ok.getSources()).containsExactly("s1");

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/chat"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("x"));
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "callPythonChat", "m", Map.of(), true, ""))
                .isInstanceOf(RuntimeException.class);

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/chat"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":500,\"message\":\"err\"}"));
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "callPythonChat", "m", Map.of(), true, ""))
                .isInstanceOf(RuntimeException.class);

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/chat"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":null}"));
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "callPythonChat", "m", Map.of(), true, ""))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void privateCallPythonChatShouldCoverNullBodyCodeAndContent() {
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/chat"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok((String) null));
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "callPythonChat", "m", null, true, null))
                .isInstanceOf(RuntimeException.class);

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/chat"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"message\":\"x\"}"));
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "callPythonChat", "m", null, true, null))
                .isInstanceOf(RuntimeException.class);

        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/chat"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"content\":null}}"));
        ChatResponseVO vo = ReflectionTestUtils.invokeMethod(service, "callPythonChat", "m", null, true, null);
        assertThat(vo.getContent()).isEqualTo("");
        assertThat(vo.getSources()).isEmpty();
    }

    @Test
    void privateParseSourcesJsonShouldHandleNullInvalidAndValid() {
        @SuppressWarnings("unchecked")
        List<String> n = (List<String>) ReflectionTestUtils.invokeMethod(service, "parseSourcesJson", (Object) null);
        assertThat(n).isEmpty();

        @SuppressWarnings("unchecked")
        List<String> bad = (List<String>) ReflectionTestUtils.invokeMethod(service, "parseSourcesJson", "{bad");
        assertThat(bad).isEmpty();

        @SuppressWarnings("unchecked")
        List<String> ok = (List<String>) ReflectionTestUtils.invokeMethod(service, "parseSourcesJson", "[\"a\",null,\"b\"]");
        assertThat(ok).containsExactly("a", "b");
    }

    @Test
    void privateParseSourcesJsonShouldHandleEmptyAndJsonNullArray() {
        @SuppressWarnings("unchecked")
        List<String> empty = (List<String>) ReflectionTestUtils.invokeMethod(service, "parseSourcesJson", "");
        assertThat(empty).isEmpty();

        @SuppressWarnings("unchecked")
        List<String> nullArr = (List<String>) ReflectionTestUtils.invokeMethod(service, "parseSourcesJson", "null");
        assertThat(nullArr).isEmpty();
    }

    @Test
    void privateHandleSseEventShouldCoverDeltaStartDoneErrorAndFallback() {
        Conversation conv = Conversation.builder().id(1L).title("新建对话").messageCount(0).build();
        SseEmitter emitter = new SseEmitter(1000L);
        AtomicBoolean completed = new AtomicBoolean(false);

        ReflectionTestUtils.invokeMethod(service, "handleSseEvent", "not-json", emitter, 1L, conv, 1, "q", completed);
        assertThat(completed.get()).isFalse();

        ReflectionTestUtils.invokeMethod(service, "handleSseEvent", "{\"type\":\"start\"}", emitter, 1L, conv, 1, "q", completed);
        ReflectionTestUtils.invokeMethod(service, "handleSseEvent", "{\"type\":\"delta\",\"content\":\"x\"}", emitter, 1L, conv, 1, "q", completed);

        when(conversationMapper.updateById(any(Conversation.class))).thenReturn(1);
        ReflectionTestUtils.invokeMethod(service, "handleSseEvent", "{\"type\":\"done\",\"content\":\"a\",\"sources\":[\"s\"]}", emitter, 1L, conv, 1, "question", completed);
        assertThat(completed.get()).isTrue();
        verify(messageMapper).insert(any(Message.class));

        AtomicBoolean completed2 = new AtomicBoolean(false);
        ReflectionTestUtils.invokeMethod(service, "handleSseEvent", "{\"type\":\"error\",\"content\":\"e\"}", new SseEmitter(1000L), 1L, conv, 1, "q", completed2);
        assertThat(completed2.get()).isTrue();
    }

    @Test
    void privateHandleSseEventShouldCoverBlankDataPrefixUnknownAndCompletedErrorBranch() {
        Conversation conv = Conversation.builder().id(1L).title("custom").messageCount(0).build();
        AtomicBoolean completed = new AtomicBoolean(false);

        ReflectionTestUtils.invokeMethod(service, "handleSseEvent", "   ", new SseEmitter(1000L), 1L, conv, 1, "q", completed);
        assertThat(completed.get()).isFalse();

        ReflectionTestUtils.invokeMethod(service, "handleSseEvent", "data: {\"type\":\"delta\",\"content\":\"x\"}", new SseEmitter(1000L), 1L, conv, 1, "q", completed);
        assertThat(completed.get()).isFalse();

        ReflectionTestUtils.invokeMethod(service, "handleSseEvent", "{\"type\":\"unknown\"}", new SseEmitter(1000L), 1L, conv, 1, "q", completed);
        assertThat(completed.get()).isFalse();

        AtomicBoolean already = new AtomicBoolean(true);
        ReflectionTestUtils.invokeMethod(service, "handleSseEvent", "{\"type\":\"error\",\"content\":\"x\"}", new SseEmitter(1000L), 1L, conv, 1, "q", already);
        assertThat(already.get()).isTrue();
    }

    @Test
    void privateSendTerminalEventShouldCoverExceptionPath() {
        SseEmitter emitter = new SseEmitter(1000L);
        emitter.complete();

        ReflectionTestUtils.invokeMethod(service, "sendTerminalEvent", emitter, "error", "x");

        assertThat(true).isTrue();
    }

    @Test
    void privateHandleSseEventShouldCatchExceptionWhenEmitterSendFails() {
        Conversation conv = Conversation.builder().id(1L).title("新建对话").messageCount(0).build();
        SseEmitter emitter = new SseEmitter(1000L);
        emitter.complete();
        AtomicBoolean completed = new AtomicBoolean(false);

        ReflectionTestUtils.invokeMethod(service, "handleSseEvent", "{\"type\":\"delta\",\"content\":\"x\"}", emitter, 1L, conv, 1, "q", completed);

        assertThat(completed.get()).isTrue();
    }

    @Test
    void privatePersistAssistantReplyShouldKeepNonDefaultTitleAndHandleNulls() {
        Conversation conv = Conversation.builder().id(1L).title("custom").messageCount(5).build();
        com.alibaba.fastjson2.JSONObject done = new com.alibaba.fastjson2.JSONObject();
        done.put("type", "done");
        when(conversationMapper.updateById(any(Conversation.class))).thenReturn(1);

        ReflectionTestUtils.invokeMethod(service, "persistAssistantReply", 1L, conv, 3, "question", done);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(msgCaptor.capture());
        assertThat(msgCaptor.getValue().getContent()).isEqualTo("");
        assertThat(msgCaptor.getValue().getSourcesJson()).isEqualTo("[]");

        ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationMapper).updateById(convCaptor.capture());
        assertThat(convCaptor.getValue().getTitle()).isEqualTo("custom");
        assertThat(convCaptor.getValue().getMessageCount()).isEqualTo(7);
    }

    @Test
    void chatStreamShouldHandleFluxOnNextDoneAndPersistAssistantMessage() throws Exception {
        mockStreamChain(Flux.just(ServerSentEvent.builder("{\"type\":\"done\",\"content\":\"ok\",\"sources\":[\"s1\"]}").build()));
        Conversation conv = Conversation.builder().id(7L).uid(1L).title("新建对话").messageCount(0).build();
        doReturn(conv).when(service).getOrCreateConversation(any(), anyLong());
        doReturn(1).when(service).getNextSequenceNum(7L);
        when(messageMapper.selectList(any())).thenReturn(List.of());
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"needs_tasks\":false,\"needs_algorithms\":false,\"needs_datasets\":false,\"needs_kb\":true}}"));
        when(conversationMapper.updateById(any(Conversation.class))).thenReturn(1);

        SseEmitter emitter = service.chatStream(1L, ChatRequestRO.builder().cid(7L).message("hello").build());

        assertThat(emitter).isNotNull();
        verify(messageMapper, times(2)).insert(any(Message.class));
        verify(conversationMapper, times(1)).updateById(any(Conversation.class));
    }

    @Test
    void chatStreamShouldHandleFluxOnError() {
        mockStreamChain(Flux.error(new RuntimeException("stream error")));
        Conversation conv = Conversation.builder().id(8L).uid(1L).title("custom").messageCount(2).build();
        doReturn(conv).when(service).getOrCreateConversation(any(), anyLong());
        doReturn(2).when(service).getNextSequenceNum(8L);
        when(messageMapper.selectList(any())).thenReturn(List.of());
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"needs_tasks\":false,\"needs_algorithms\":false,\"needs_datasets\":false,\"needs_kb\":true}}"));

        SseEmitter emitter = service.chatStream(1L, ChatRequestRO.builder().cid(8L).message("hello").build());

        assertThat(emitter).isNotNull();
        verify(messageMapper, times(1)).insert(any(Message.class));
    }

    @Test
    void chatStreamShouldHandleFluxOnCompleteBeforeDone() {
        mockStreamChain(Flux.empty());
        Conversation conv = Conversation.builder().id(10L).uid(1L).title("custom").messageCount(2).build();
        doReturn(conv).when(service).getOrCreateConversation(any(), anyLong());
        doReturn(2).when(service).getNextSequenceNum(10L);
        when(messageMapper.selectList(any())).thenReturn(List.of());
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"needs_tasks\":false,\"needs_algorithms\":false,\"needs_datasets\":false,\"needs_kb\":true}}"));

        SseEmitter emitter = service.chatStream(1L, ChatRequestRO.builder().cid(10L).message("hello").build());

        assertThat(emitter).isNotNull();
        verify(messageMapper, times(1)).insert(any(Message.class));
        verify(conversationMapper, never()).updateById(any(Conversation.class));
    }

    @Test
    void chatStreamEmitterCallbacksShouldDisposeSubscription() throws Exception {
        Disposable disposable = org.mockito.Mockito.mock(Disposable.class);
        when(disposable.isDisposed()).thenReturn(false);

        @SuppressWarnings("unchecked")
        Flux<ServerSentEvent<String>> flux = org.mockito.Mockito.mock(Flux.class);
        when(flux.subscribe(any(Consumer.class), any(Consumer.class), any(Runnable.class))).thenReturn(disposable);
        mockStreamChain(flux);

        Conversation conv = Conversation.builder().id(12L).uid(1L).title("custom").messageCount(2).build();
        doReturn(conv).when(service).getOrCreateConversation(any(), anyLong());
        doReturn(2).when(service).getNextSequenceNum(12L);
        when(messageMapper.selectList(any())).thenReturn(List.of());
        when(restTemplate.postForEntity(eq("http://localhost:8000/api/assistant/classify"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"data\":{\"needs_tasks\":false,\"needs_algorithms\":false,\"needs_datasets\":false,\"needs_kb\":true}}"));

        SseEmitter emitter = service.chatStream(1L, ChatRequestRO.builder().cid(12L).message("hello").build());

        Object completionCallback = ReflectionTestUtils.getField(emitter, "completionCallback");
        var completionMethod = completionCallback.getClass().getDeclaredMethod("run");
        completionMethod.setAccessible(true);
        completionMethod.invoke(completionCallback);
        verify(disposable, times(1)).dispose();

        Disposable disposable2 = org.mockito.Mockito.mock(Disposable.class);
        when(disposable2.isDisposed()).thenReturn(false);
        @SuppressWarnings("unchecked")
        Flux<ServerSentEvent<String>> flux2 = org.mockito.Mockito.mock(Flux.class);
        when(flux2.subscribe(any(Consumer.class), any(Consumer.class), any(Runnable.class))).thenReturn(disposable2);
        mockStreamChain(flux2);
        SseEmitter emitter2 = service.chatStream(1L, ChatRequestRO.builder().cid(12L).message("hello").build());
        Object timeoutCallback = ReflectionTestUtils.getField(emitter2, "timeoutCallback");
        var timeoutMethod = timeoutCallback.getClass().getDeclaredMethod("run");
        timeoutMethod.setAccessible(true);
        timeoutMethod.invoke(timeoutCallback);
        verify(disposable2, times(1)).dispose();

        Disposable disposable3 = org.mockito.Mockito.mock(Disposable.class);
        when(disposable3.isDisposed()).thenReturn(false);
        @SuppressWarnings("unchecked")
        Flux<ServerSentEvent<String>> flux3 = org.mockito.Mockito.mock(Flux.class);
        when(flux3.subscribe(any(Consumer.class), any(Consumer.class), any(Runnable.class))).thenReturn(disposable3);
        mockStreamChain(flux3);
        SseEmitter emitter3 = service.chatStream(1L, ChatRequestRO.builder().cid(12L).message("hello").build());
        Object errorCallback = ReflectionTestUtils.getField(emitter3, "errorCallback");
        var errorMethod = errorCallback.getClass().getDeclaredMethod("accept", Throwable.class);
        errorMethod.setAccessible(true);
        errorMethod.invoke(errorCallback, new RuntimeException("x"));
        verify(disposable3, times(1)).dispose();
    }

    @SuppressWarnings("unchecked")
    private void mockStreamChain(Flux<ServerSentEvent<String>> flux) {
        WebClient.RequestBodyUriSpec requestBodyUriSpec = org.mockito.Mockito.mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec<?> requestHeadersSpec = org.mockito.Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = org.mockito.Mockito.mock(WebClient.ResponseSpec.class);

        when(assistantWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/assistant/chat/stream")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.accept(org.springframework.http.MediaType.TEXT_EVENT_STREAM)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any(Map.class))).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(any(org.springframework.core.ParameterizedTypeReference.class))).thenReturn((Flux) flux);
    }
}
