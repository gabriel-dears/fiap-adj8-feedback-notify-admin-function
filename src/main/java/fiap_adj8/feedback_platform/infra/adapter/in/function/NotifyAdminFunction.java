package fiap_adj8.feedback_platform.infra.adapter.in.function;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.Gson;
import fiap_adj8.feedback_platform.application.port.out.client.AdminServiceClientPort;
import fiap_adj8.feedback_platform.application.port.out.email.EmailSender;
import fiap_adj8.feedback_platform.application.port.out.email.input.EmailInput;
import fiap_adj8.feedback_platform.domain.model.AlertMessageDetails;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class NotifyAdminFunction implements BackgroundFunction<NotifyAdminFunction.PubSubMessage> {

    private static final Logger logger = Logger.getLogger(NotifyAdminFunction.class.getName());
    private static final Gson gson = new Gson();

    @Inject
    EmailSender sender;

    @Inject
    AdminServiceClientPort adminServiceClient;

    @Inject
    @ConfigProperty(name = "email.template.path", defaultValue = "templates/feedback-alert.html")
    String templatePath;

    private String htmlTemplate;

    void onStart(@Observes StartupEvent event) {
        try {
            Path path = Path.of("src/main/resources", templatePath);
            htmlTemplate = Files.readString(path);
            logger.info("✅ Email template loaded from: " + path.toAbsolutePath());
        } catch (IOException e) {
            logger.severe("❌ Failed to load email template: " + e.getMessage());
            htmlTemplate = "<p>Template not found</p>"; // fallback simples
        }
    }

    public static class PubSubMessage {
        public String data;
    }

    @Override
    public void accept(PubSubMessage message, Context context) throws IOException {
        String decoded = new String(Base64.getDecoder().decode(message.data));
        logger.info("📨 Received Pub/Sub message: " + decoded);

        AlertMessageDetails feedback = gson.fromJson(decoded, AlertMessageDetails.class);
        notifyAdmin(feedback);
    }

    private void notifyAdmin(AlertMessageDetails feedback) throws IOException {
        logger.info("📩 Notifying admins about feedback: " + feedback.getLessonName());

        List<String> adminEmails = adminServiceClient.getAdminEmails();
        if (adminEmails.isEmpty()) {
            logger.info("⚠️ No admin emails found.");
            return;
        }

        String htmlContent = htmlTemplate
                .replace("{student}", feedback.getStudentName())
                .replace("{lesson}", feedback.getLessonName())
                .replace("{comment}", feedback.getComment())
                .replace("{rating}", String.valueOf(feedback.getRating()))
                .replace("{date}", feedback.getDate().toString());

        for (String adminEmail : adminEmails) {
            try {
                EmailInput emailInput = new EmailInput(
                        adminEmail,
                        String.format("Urgent feedback for lesson %s", feedback.getLessonName()),
                        htmlContent
                );
                sender.send(emailInput);
                logger.info("✅ Email sent to: " + adminEmail);
            } catch (Exception e) {
                logger.warning("❌ Failed to send email to " + adminEmail + ": " + e.getMessage());
            }
        }
    }
}
