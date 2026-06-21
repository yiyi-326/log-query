package com.example.logquery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.restTemplate = new RestTemplate();
    }

    public String sendWebhook(String webhookUrl, String title, String content) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return "webhook URL 未配置，跳过";
        }
        try {
            Map<String, Object> body = Map.of(
                    "msgtype", "markdown",
                    "markdown", Map.of(
                            "title", title,
                            "text", content
                    )
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(webhookUrl, entity, String.class);
            log.info("Webhook 发送成功: {}", webhookUrl);
            return "webhook_sent";
        } catch (Exception e) {
            log.error("Webhook 发送失败: {}", e.getMessage());
            return "webhook_failed: " + e.getMessage();
        }
    }

    public String sendEmail(String recipients, String subject, String body) {
        if (recipients == null || recipients.isBlank()) {
            return "邮件收件人未配置，跳过";
        }
        if (mailFrom == null || mailFrom.isBlank()) {
            return "邮件发送者(spring.mail.username)未配置，跳过";
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(recipients.split(","));
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("邮件发送成功: {}", recipients);
            return "email_sent";
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage());
            return "email_failed: " + e.getMessage();
        }
    }
}
