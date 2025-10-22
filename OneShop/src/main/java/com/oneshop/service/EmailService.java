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

    public void sendDeliveryConfirmation(String to, Long orderId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Đơn hàng của bạn đã được giao thành công");
        message.setText("Kính gửi quý khách,\n\n" +
                        "Đơn hàng #" + orderId + " đã được giao thành công.\n\n" +
                        "Vui lòng đánh giá sản phẩm để giúp chúng tôi cải thiện dịch vụ. " +
                        "Bạn có thể đăng nhập vào tài khoản và truy cập phần đơn hàng để đánh giá.\n\n" +
                        "Cảm ơn quý khách đã mua sắm tại OneShop!");
        mailSender.send(message);
    }
}