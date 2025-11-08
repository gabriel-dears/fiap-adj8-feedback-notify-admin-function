package fiap_adj8.feedback_platform.infra.adapter.out.email;

import fiap_adj8.feedback_platform.application.port.out.email.EmailSender;
import fiap_adj8.feedback_platform.application.port.out.email.input.EmailInput;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.inject.Singleton;

@Singleton
public class EmailSenderImpl implements EmailSender {

    private final Mailer mailer;

    public EmailSenderImpl(Mailer mailer) {
        this.mailer = mailer;
    }

    @Override
    public void send(EmailInput emailInput) {
        mailer.send(
                Mail.withHtml(emailInput.to(), emailInput.subject(), emailInput.htmlContent())
        );
    }
}
