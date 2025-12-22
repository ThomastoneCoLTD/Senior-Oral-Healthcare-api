package com.kaii.dentix.global.common.mail;

import lombok.RequiredArgsConstructor;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendBillingNotice(String toEmail, String orgName, double amount) {
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