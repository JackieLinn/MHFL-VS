package ynu.jackielinn.server.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MailQueueListener 单元测试：register/reset 类型发送邮件，default 不发送。
 */
@ExtendWith(MockitoExtension.class)
class MailQueueListenerTest {

    @Mock
    private JavaMailSender sender;

    @InjectMocks
    private MailQueueListener listener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "username", "noreply@example.com");
    }

    @Test
    void shouldSendRegisterMailWhenTypeIsRegister() {
        Map<String, Object> data = Map.of(
                "email", "user@example.com",
                "code", 123456,
                "type", "register"
        );
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        listener.sendMailMessage(data);

        verify(sender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertEquals("欢迎注册 MHFL-VS 网站", msg.getSubject());
        assertNotNull(msg.getText());
        assertTrue(msg.getText().contains("123456"));
        assertTrue(msg.getText().contains("有效时间3分钟"));
        assertNotNull(msg.getTo());
        assertEquals("user@example.com", msg.getTo()[0]);
        assertEquals("noreply@example.com", msg.getFrom());
    }

    @Test
    void shouldSendResetMailWhenTypeIsReset() {
        Map<String, Object> data = Map.of(
                "email", "reset@example.com",
                "code", 654321,
                "type", "reset"
        );
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        listener.sendMailMessage(data);

        verify(sender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertEquals("您的 MHFL-VS 网站密码重置邮件", msg.getSubject());
        assertNotNull(msg.getText());
        assertTrue(msg.getText().contains("654321"));
        assertTrue(msg.getText().contains("重置密码"));
        assertNotNull(msg.getTo());
        assertEquals("reset@example.com", msg.getTo()[0]);
    }

    @Test
    void shouldNotSendWhenTypeIsUnknown() {
        Map<String, Object> data = Map.of(
                "email", "user@example.com",
                "code", 111111,
                "type", "unknown"
        );

        listener.sendMailMessage(data);

        verify(sender, never()).send(any(SimpleMailMessage.class));
    }
}
