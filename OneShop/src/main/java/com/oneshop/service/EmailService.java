package com.oneshop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp, String type) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(type.equals("REGISTRATION") ? "OneShop Registration OTP" : "OneShop Password Reset OTP");
        message.setText("Your OTP is: " + otp + ". It expires in 5 minutes.");
        mailSender.send(message);
    }
}