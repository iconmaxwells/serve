package com.restaurantApp.serve.service;

import com.sun.mail.smtp.SMTPTransport;
import org.springframework.stereotype.Service;



@Service
public interface EmailService {
    void sendHtmlEmailWithEmbeddedFiles(String name, String to, String token);

    void sendOtpMail(String to, String otp);
}
