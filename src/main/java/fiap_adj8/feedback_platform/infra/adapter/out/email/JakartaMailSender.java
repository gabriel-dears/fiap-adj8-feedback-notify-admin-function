package fiap_adj8.feedback_platform.infra.adapter.out.email;

import fiap_adj8.feedback_platform.application.port.out.email.EmailSender;
import fiap_adj8.feedback_platform.application.port.out.email.input.EmailInput;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.logging.Logger;

public class JakartaMailSender implements EmailSender {

    private static final Logger logger = Logger.getLogger(JakartaMailSender.class.getName());

    @Override
    public void send(EmailInput emailInput) {

        try {
            String from = System.getenv("EMAIL_SMTP_FROM");
            String password = System.getenv("EMAIL_SMTP_PASSWORD");
            String host = System.getenv("EMAIL_SMTP_HOST");
            String port = System.getenv("EMAIL_SMTP_PORT");

            if (from == null || password == null || host == null || port == null) {
                throw new RuntimeException("Variáveis SMTP não configuradas corretamente");
            }

            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", port);

            Session session = Session.getInstance(properties, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(from, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(emailInput.to())
            );
            message.setSubject(emailInput.subject());
            message.setContent(emailInput.htmlContent(), "text/html; charset=UTF-8");

            Transport.send(message);

        } catch (Exception e) {
            logger.warning("Sending email failed: " + e.getMessage());
        }
    }
}

