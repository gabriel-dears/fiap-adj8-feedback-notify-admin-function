package fiap_adj8.feedback_platform.infra.adapter.in.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fiap_adj8.feedback_platform.application.port.out.client.AdminServiceClientPort;
import fiap_adj8.feedback_platform.application.port.out.email.EmailSender;
import fiap_adj8.feedback_platform.application.port.out.email.input.EmailInput;
import fiap_adj8.feedback_platform.domain.model.AlertMessageDetails;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

@Named("notifyAdmin")
@RegisterForReflection
@ApplicationScoped
public class NotifyAdminFunction implements RequestHandler<SNSEvent, Void> {

    private static final Logger logger = Logger.getLogger(NotifyAdminFunction.class.getName());

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>)
                            (src, typeOfSrc, ctx) ->
                                    new com.google.gson.JsonPrimitive(
                                            src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<LocalDateTime>)
                            (json, typeOfT, ctx) ->
                                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

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
            Path path = Path.of(templatePath);
            htmlTemplate = Files.readString(path);
            logger.info("✅ Email template loaded from: " + path.toAbsolutePath());
        } catch (IOException e) {
            logger.severe("❌ Failed to load email template: " + e.getMessage());
            htmlTemplate = "<p>Template not found</p>";
        }
    }

    // ======================================================
    // 🔥 HANDLER REAL DA AWS LAMBDA (SNS EVENT)
    // ======================================================
    @Override
    public Void handleRequest(SNSEvent event, Context context) {
        logger.info("Handle NotifyAdminFunction");
        try {
            if (event.getRecords() == null || event.getRecords().isEmpty()) {
                logger.warning("⚠️ SNS event has no records");
                return null;
            }

            String encoded = event.getRecords().getFirst().getSNS().getMessage();
            logger.info("📨 Received SNS payload encoded: " + encoded);

            AlertMessageDetails urgentFeedback = decode(encoded);

            logger.info("Alert Message details received: " + urgentFeedback);

            notifyAdmin(urgentFeedback);

        } catch (Exception e) {
            logger.severe("❌ Error in handleRequest: " + e.getMessage());
        }
        return null;
    }

    // ✓ agora compatível com SNS (mantendo sua lógica original)
    private AlertMessageDetails decode(String encoded) {
        String decoded = new String(Base64.getDecoder().decode(encoded));
        logger.info("📨 Decoded SNS payload: " + decoded);
        return gson.fromJson(decoded, AlertMessageDetails.class);
    }

    // ======================================================
    // 🔥 Lógica de notificação — igual ao seu código original
    // ======================================================
    private void notifyAdmin(AlertMessageDetails urgentFeedback) {
        logger.info("📩 Notifying admins about feedback: " + urgentFeedback.getLessonName());

//        List<String> adminEmails = adminServiceClient.getAdminEmails();
        List<String> adminEmails = List.of("gabrieldears@gmail.com");
        if (adminEmails.isEmpty()) {
            logger.info("⚠️ No admin emails found.");
            return;
        }

        String emailHtmlContent = getHtmlContent(urgentFeedback);
        sendEmailToAllAdmins(urgentFeedback, adminEmails, emailHtmlContent);
    }

    private void sendEmailToAllAdmins(AlertMessageDetails urgentFeedback, List<String> adminEmails, String htmlContent) {
        for (String adminEmail : adminEmails) {
            try {
                sendEmailToAdmin(urgentFeedback, htmlContent, adminEmail);
            } catch (Exception e) {
                logger.warning("❌ Failed to send email to " + adminEmail + ": " + e.getMessage());
            }
        }
    }

    private void sendEmailToAdmin(AlertMessageDetails urgentFeedback, String htmlContent, String adminEmail) {
        EmailInput emailInput = new EmailInput(
                adminEmail,
                String.format("Urgent feedback for lesson %s", urgentFeedback.getLessonName()),
                htmlContent
        );
        sender.send(emailInput);
        logger.info("✅ Email sent to: " + adminEmail);
    }

    private String getHtmlContent(AlertMessageDetails urgentFeedback) {
        return htmlTemplate
                .replace("{student}", urgentFeedback.getStudentName())
                .replace("{lesson}", urgentFeedback.getLessonName())
                .replace("{comment}", urgentFeedback.getComment())
                .replace("{rating}", urgentFeedback.getRating())
                .replace("{date}", urgentFeedback.getDate() != null ? urgentFeedback.getDate().toString() : "");
    }
}
