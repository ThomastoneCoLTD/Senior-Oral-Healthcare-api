package com.kaii.dentix.global.common.mail;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenders;
    private final boolean enabled;

    public EmailService(ObjectProvider<JavaMailSender> mailSenders,
                        @Value("${soh.mail.enabled:false}") boolean enabled) {
        this.mailSenders = mailSenders;
        this.enabled = enabled;
    }

    public void sendBillingNotice(String toEmail, String orgName, double amount) {
        if (!enabled) return;
        JavaMailSender javaMailSender = mailSenders.getIfAvailable();
        if (javaMailSender == null) {
            throw new IllegalStateException("메일 사용 설정에 SMTP 구성이 필요합니다.");
        }
        String subject = "[DentiGlobal] " + orgName + " 기관의 추가 과금 안내";
        String text = String.format(
                """
                안녕하세요, %s 관리자님.

                귀 기관의 구강 분석 사용량이 모두 소진되어,
                추가 1건 요금(%.0f원)이 청구되었습니다.

                관리자 페이지에서 청구 내역을 확인해주세요.

                감사합니다.
                """, orgName, amount
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("cs@thomastone.co.kr");
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);

        javaMailSender.send(message);
    }
}
