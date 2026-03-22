package com.codearena.module4_shop.service;

import com.codearena.module4_shop.dto.PurchaseResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // Send order confirmation to participant
    public void sendOrderConfirmation(String toEmail, PurchaseResponse order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("✅ Order Confirmed — #" + order.getId().toString().substring(0, 8).toUpperCase());
            helper.setText(buildOrderEmail(order), true); // true = HTML

            mailSender.send(message);
            log.info("Order confirmation sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    // Send new order alert to admin
    public void sendAdminOrderAlert(String adminEmail, PurchaseResponse order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(adminEmail);
            helper.setSubject("🛒 New Order Received — #" + order.getId().toString().substring(0, 8).toUpperCase());
            helper.setText(buildAdminEmail(order), true);

            mailSender.send(message);
            log.info("Admin alert sent for order: {}", order.getId());

        } catch (MessagingException e) {
            log.error("Failed to send admin alert: {}", e.getMessage());
        }
    }

    // HTML email template for participant
    private String buildOrderEmail(PurchaseResponse order) {
        StringBuilder items = new StringBuilder();
        for (var item : order.getItems()) {
            items.append(String.format("""
                <tr>
                  <td style="padding:10px;border-bottom:1px solid #1a1a2e;color:#e2e8f0">%s</td>
                  <td style="padding:10px;border-bottom:1px solid #1a1a2e;color:#94a3b8;text-align:center">×%d</td>
                  <td style="padding:10px;border-bottom:1px solid #1a1a2e;color:#8b5cf6;text-align:right">$%.2f</td>
                </tr>
                """,
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getUnitPrice() * item.getQuantity()
            ));
        }

        return String.format("""
            <div style="background:#0a0a0f;padding:40px;font-family:'Segoe UI',sans-serif;max-width:600px;margin:0 auto">
              <h1 style="color:#8b5cf6;font-size:24px;letter-spacing:3px;margin-bottom:8px">⚔ CODEARENA SHOP</h1>
              <p style="color:#64748b;letter-spacing:2px;font-size:12px">ORDER CONFIRMATION</p>
              <hr style="border:1px solid #1a1a2e;margin:20px 0">
              <h2 style="color:#e2e8f0;font-size:16px">
                Order #%s
              </h2>
              <p style="color:#94a3b8;font-size:13px">
                Thank you for your purchase! Your order is being processed.
              </p>
              <table style="width:100%%;border-collapse:collapse;margin:20px 0;background:#0d0d15;border-radius:8px;overflow:hidden">
                <thead>
                  <tr style="background:#12121e">
                    <th style="padding:12px;text-align:left;color:#64748b;font-size:11px;letter-spacing:1px">PRODUCT</th>
                    <th style="padding:12px;text-align:center;color:#64748b;font-size:11px;letter-spacing:1px">QTY</th>
                    <th style="padding:12px;text-align:right;color:#64748b;font-size:11px;letter-spacing:1px">TOTAL</th>
                  </tr>
                </thead>
                <tbody>%s</tbody>
              </table>
              <div style="text-align:right;padding:16px 0;border-top:1px solid #1a1a2e">
                <span style="color:#64748b;font-size:12px;letter-spacing:2px">TOTAL: </span>
                <span style="color:#8b5cf6;font-size:22px;font-weight:700">$%.2f</span>
              </div>
              <div style="background:#12121e;border-radius:8px;padding:16px;margin-top:20px">
                <p style="color:#64748b;font-size:11px;letter-spacing:1px;margin:0">
                  STATUS: <span style="color:#f59e0b">PENDING</span> — We'll notify you when it ships!
                </p>
              </div>
              <hr style="border:1px solid #1a1a2e;margin:20px 0">
              <p style="color:#334155;font-size:11px;text-align:center">
                CodeArena Shop — Gear up, code harder, represent.
              </p>
            </div>
            """,
                order.getId().toString().substring(0, 8).toUpperCase(),
                items.toString(),
                order.getTotalPrice()
        );
    }

    // HTML email template for admin
    private String buildAdminEmail(PurchaseResponse order) {
        return String.format("""
            <div style="background:#0a0a0f;padding:40px;font-family:'Segoe UI',sans-serif;max-width:600px;margin:0 auto">
              <h1 style="color:#06b6d4;font-size:20px;letter-spacing:2px">🛒 NEW ORDER ALERT</h1>
              <hr style="border:1px solid #1a1a2e;margin:20px 0">
              <p style="color:#e2e8f0;font-size:14px">
                A new order has been placed on CodeArena Shop.
              </p>
              <div style="background:#0d0d15;border:1px solid #1a1a2e;border-radius:8px;padding:20px;margin:16px 0">
                <p style="color:#64748b;font-size:11px;letter-spacing:1px;margin:0 0 8px">ORDER ID</p>
                <p style="color:#8b5cf6;font-size:18px;font-weight:700;margin:0">
                  #%s
                </p>
              </div>
              <div style="background:#0d0d15;border:1px solid #1a1a2e;border-radius:8px;padding:20px;margin:16px 0">
                <p style="color:#64748b;font-size:11px;letter-spacing:1px;margin:0 0 8px">PARTICIPANT</p>
                <p style="color:#e2e8f0;font-size:14px;margin:0">%s</p>
              </div>
              <div style="background:#0d0d15;border:1px solid #1a1a2e;border-radius:8px;padding:20px;margin:16px 0">
                <p style="color:#64748b;font-size:11px;letter-spacing:1px;margin:0 0 8px">TOTAL</p>
                <p style="color:#10b981;font-size:22px;font-weight:700;margin:0">$%.2f</p>
              </div>
              <p style="color:#64748b;font-size:12px;margin-top:20px">
                Log in to the admin panel to process this order.
              </p>
            </div>
            """,
                order.getId().toString().substring(0, 8).toUpperCase(),
                order.getParticipantId(),
                order.getTotalPrice()
        );
    }
}