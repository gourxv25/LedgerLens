package com.gourav.LedgerLens.Service.ServiceImp;

import com.gourav.LedgerLens.Service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImp implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String text)
            throws MessagingException {

        log.info("Sending email to={} subject='{}'", to, subject);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true); // HTML content

            mailSender.send(message);

            log.info("Email sent successfully to={}", to);

        } catch (MessagingException e) {
            log.error(
                    "Failed to send email to={} subject='{}'",
                    to,
                    subject,
                    e
            );
            throw e; // ✅ propagate to GlobalExceptionHandler
        }
    }
}
