package fiap_adj8.feedback_platform.application.port.out.email.input;

public record EmailInput(
        String to,
        String subject,
        String htmlContent
) {
}
