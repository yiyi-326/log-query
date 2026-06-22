package com.example.logquery.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    private NotificationService initService() {
        NotificationService service = new NotificationService(mailSender);
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@example.com");
        return service;
    }

    @Nested
    class Webhook {

        @Test
        void shouldSkipWhenUrlIsNull() {
            String result = initService().sendWebhook(null, "title", "content");
            assertTrue(result.contains("跳过"));
        }

        @Test
        void shouldSkipWhenUrlIsBlank() {
            String result = initService().sendWebhook("   ", "title", "content");
            assertTrue(result.contains("跳过"));
        }

        @Test
        void shouldReturnFailedOnNetworkError() {
            String result = initService().sendWebhook("http://invalid-host-that-does-not-exist.local", "t", "c");
            assertTrue(result.contains("webhook_failed"));
        }
    }

    @Nested
    class Email {

        @Test
        void shouldSkipWhenRecipientsNull() {
            String result = initService().sendEmail(null, "subject", "body");
            assertTrue(result.contains("跳过"));
        }

        @Test
        void shouldSkipWhenRecipientsBlank() {
            String result = initService().sendEmail("  ", "subject", "body");
            assertTrue(result.contains("跳过"));
        }

        @Test
        void shouldSkipWhenMailFromNotConfigured() {
            NotificationService service = new NotificationService(mailSender);
            // mailFrom defaults to empty string from @Value

            String result = service.sendEmail("user@example.com", "subject", "body");
            assertTrue(result.contains("跳过"));
        }

        @Test
        void shouldSendEmailWhenConfigured() {
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            String result = initService().sendEmail("a@b.com,c@d.com", "Alert", "body text");

            assertEquals("email_sent", result);
            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        void shouldReturnFailedOnMailError() {
            doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

            String result = initService().sendEmail("user@example.com", "subject", "body");

            assertTrue(result.contains("email_failed"));
        }
    }
}
