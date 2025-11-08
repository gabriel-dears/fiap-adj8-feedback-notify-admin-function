package fiap_adj8.feedback_platform.application.port.out.email;

import fiap_adj8.feedback_platform.application.port.out.email.input.EmailInput;

public interface EmailSender {
    void send(EmailInput emailInput);
}
