package com.codearena.module9_arenatalk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;

@Service
@RequiredArgsConstructor
public class ArenatalkEmailService {

    private final JavaMailSender mailSender;

    private static final String FROM_EMAIL = "arenacodeesprit@gmail.com";
    private static final String FROM_NAME  = "ArenaTalk";

    public void sendHubAcceptedEmail(String toEmail, String userName, String hubName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            helper.setTo(toEmail);
            helper.setSubject("You've been accepted into " + hubName + " - ArenaTalk");

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#06070f;font-family:'Segoe UI',sans-serif;">
                  <div style="max-width:560px;margin:40px auto;background:#0c0e1a;border-radius:20px;overflow:hidden;border:1px solid rgba(141,99,255,0.25);">

                    <div style="background:linear-gradient(90deg,#8d63ff,#22c7ff);padding:32px 40px;">
                      <h1 style="margin:0;color:white;font-size:24px;font-weight:800;letter-spacing:-0.02em;">
                        ArenaTalk
                      </h1>
                      <p style="margin:6px 0 0;color:rgba(255,255,255,0.8);font-size:14px;">
                        Community Platform
                      </p>
                    </div>

                    <div style="padding:36px 40px;">
                      <div style="display:inline-block;background:rgba(30,255,163,0.1);border:1px solid rgba(30,255,163,0.3);border-radius:999px;padding:6px 16px;margin-bottom:20px;">
                        <span style="color:#1effa3;font-size:13px;font-weight:700;">REQUEST ACCEPTED</span>
                      </div>

                      <h2 style="margin:0 0 12px;color:#f0f4ff;font-size:22px;font-weight:800;">
                        Welcome to <span style="color:#8d63ff;">%s</span>!
                      </h2>

                      <p style="margin:0 0 24px;color:#95a4c3;font-size:15px;line-height:1.7;">
                        Hey <strong style="color:#f0f4ff;">%s</strong>, your request to join
                        <strong style="color:#f0f4ff;">%s</strong> has been
                        <strong style="color:#1effa3;">accepted</strong>.
                        You can now access all channels and participate in the community.
                      </p>

                      <a href="http://localhost:4200/arenatalk"
                         style="display:inline-block;background:linear-gradient(90deg,#8d63ff,#22c7ff);color:white;text-decoration:none;padding:14px 28px;border-radius:12px;font-weight:700;font-size:15px;">
                        Go to ArenaTalk
                      </a>
                    </div>

                    <div style="padding:20px 40px;border-top:1px solid rgba(255,255,255,0.06);">
                      <p style="margin:0;color:#4a5568;font-size:12px;">
                        This email was sent by ArenaTalk - CodeArena Platform.<br/>
                        If you did not request to join this community, ignore this email.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(hubName, userName, hubName);

            helper.setText(html, true);
            mailSender.send(message);
            System.out.println("Acceptance email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("Failed to send acceptance email: " + e.getMessage());
        }
    }

    public void sendHubRejectedEmail(String toEmail, String userName, String hubName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            helper.setTo(toEmail);
            helper.setSubject("Your request to join " + hubName + " was declined - ArenaTalk");

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#06070f;font-family:'Segoe UI',sans-serif;">
                  <div style="max-width:560px;margin:40px auto;background:#0c0e1a;border-radius:20px;overflow:hidden;border:1px solid rgba(255,107,129,0.25);">

                    <div style="background:linear-gradient(90deg,#8d63ff,#22c7ff);padding:32px 40px;">
                      <h1 style="margin:0;color:white;font-size:24px;font-weight:800;letter-spacing:-0.02em;">
                        ArenaTalk
                      </h1>
                      <p style="margin:6px 0 0;color:rgba(255,255,255,0.8);font-size:14px;">
                        Community Platform
                      </p>
                    </div>

                    <div style="padding:36px 40px;">
                      <div style="display:inline-block;background:rgba(255,107,129,0.1);border:1px solid rgba(255,107,129,0.3);border-radius:999px;padding:6px 16px;margin-bottom:20px;">
                        <span style="color:#ff6b81;font-size:13px;font-weight:700;">REQUEST DECLINED</span>
                      </div>

                      <h2 style="margin:0 0 12px;color:#f0f4ff;font-size:22px;font-weight:800;">
                        Request for <span style="color:#ff6b81;">%s</span> declined
                      </h2>

                      <p style="margin:0 0 24px;color:#95a4c3;font-size:15px;line-height:1.7;">
                        Hey <strong style="color:#f0f4ff;">%s</strong>, unfortunately your request to join
                        <strong style="color:#f0f4ff;">%s</strong> has been
                        <strong style="color:#ff6b81;">declined</strong>
                        by the community owner. You can explore other communities on ArenaTalk.
                      </p>

                      <a href="http://localhost:4200/arenatalk/join"
                         style="display:inline-block;background:linear-gradient(90deg,#8d63ff,#22c7ff);color:white;text-decoration:none;padding:14px 28px;border-radius:12px;font-weight:700;font-size:15px;">
                        Explore Communities
                      </a>
                    </div>

                    <div style="padding:20px 40px;border-top:1px solid rgba(255,255,255,0.06);">
                      <p style="margin:0;color:#4a5568;font-size:12px;">
                        This email was sent by ArenaTalk - CodeArena Platform.<br/>
                        If you did not request to join this community, ignore this email.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(hubName, userName, hubName);

            helper.setText(html, true);
            mailSender.send(message);
            System.out.println("Rejection email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("Failed to send rejection email: " + e.getMessage());
        }
    }
}