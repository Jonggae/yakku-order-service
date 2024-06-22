package com.jonggae.yakku.mailVerification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;
    private static final String senderEmail = "muvnelika@gmail.com";

    public MimeMessage createMail(String mail, String token) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            message.setFrom(senderEmail);
            message.setRecipients(MimeMessage.RecipientType.TO, mail);
            message.setSubject("[Yakku] 회원 가입 확인 메일입니다.");
            String confirmationUrl = "http://localhost:8080/api/customer/confirm?token=" + token;
            String body = "<h3>회원 가입 인증 링크입니다.</h3>"
                    + "<a href=\"" + confirmationUrl + "\">회원가입을 완료하려면 여기를 클릭하세요</a>"
                    + "<h3>감사합니다.</h3>";
            message.setText(body, "UTF-8", "html");
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return message;
    }

    public void sendMail(String mail, String token) {
        MimeMessage message = createMail(mail, token);
        javaMailSender.send(message);

    }
}
