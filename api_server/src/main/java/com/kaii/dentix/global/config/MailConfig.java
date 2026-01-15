package com.kaii.dentix.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    /**
     *JavaMailSender Bean 강제 등록
     * - spring-boot-starter-mail 의존성이 추가되어 있어야 정상 동작
     * - 설정 파일(application.yml)의 spring.mail.* 값을 자동으로 읽어감
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        //기본 설정 (application.yml이 없을 때 대비)
        mailSender.setHost("smtp.sendgrid.net");
        mailSender.setPort(587);
        mailSender.setUsername("apikey");
        mailSender.setPassword("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");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true"); // 콘솔에 SMTP 로그 출력

        return mailSender;
    }
}
