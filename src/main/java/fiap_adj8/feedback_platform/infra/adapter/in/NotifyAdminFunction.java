package fiap_adj8.feedback_platform.infra.adapter.in;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fiap_adj8.feedback_platform.application.port.out.client.AdminServiceClientPort;
import fiap_adj8.feedback_platform.application.port.out.email.EmailSender;
import fiap_adj8.feedback_platform.application.port.out.email.input.EmailInput;
import fiap_adj8.feedback_platform.application.port.out.template.TemplateProvider;
import fiap_adj8.feedback_platform.domain.model.AlertMessageDetails;
import fiap_adj8.feedback_platform.domain.model.PubSubMessage;
import fiap_adj8.feedback_platform.infra.adapter.out.client.AdminServiceClientAdapter;
import fiap_adj8.feedback_platform.infra.adapter.out.email.JakartaMailSender;
import fiap_adj8.feedback_platform.infra.adapter.out.template.TemplateLoader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

public class NotifyAdminFunction implements BackgroundFunction<PubSubMessage> {

    private static final Logger logger = Logger.getLogger(NotifyAdminFunction.class.getName());

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>)
                    (src, typeOfSrc, ctx) -> new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>)
                    (json, typeOfT, ctx) -> LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    private final EmailSender emailSender = new JakartaMailSender();

    private final AdminServiceClientPort adminServiceClientPort = new AdminServiceClientAdapter();

    private final String template;

    public NotifyAdminFunction() {
        TemplateProvider templateProvider = new TemplateLoader();
        this.template = templateProvider.getTemplate("feedback-alert.html");
    }

    @Override
    public void accept(PubSubMessage message, Context context) {
        try {
            if (message == null || message.data == null) {
                logger.warning("⚠️ No message data");
                return;
            }

            String decoded = new String(Base64.getDecoder().decode(message.data));
            logger.info("📨 Decoded Pub/Sub message: " + decoded);

            AlertMessageDetails feedback = gson.fromJson(decoded, AlertMessageDetails.class);

            notifyAdmins(feedback);
        } catch (Exception e) {
            logger.warning("Error when receiving message: " + e.getMessage());
        }
    }

    private void notifyAdmins(AlertMessageDetails feedback) {
        List<String> adminEmails = adminServiceClientPort.getAdminEmails();

        for (String email : adminEmails) {
            try {
                sendEmail(email, feedback);
                logger.info("✅ Email sent to: " + email);
            } catch (Exception e) {
                logger.warning("❌ Failed to send email to " + email + ": " + e.getMessage());
            }
        }
    }

    private void sendEmail(String to, AlertMessageDetails feedback) {
        String content = template
                .replace("{student}", feedback.getStudentName())
                .replace("{lesson}", feedback.getLessonName())
                .replace("{comment}", feedback.getComment())
                .replace("{rating}", feedback.getRating())
                .replace("{date}", feedback.getDate() != null ? feedback.getDate().toString() : "");
        emailSender.send(new EmailInput(
                to,
                "Urgent feedback for lesson " + feedback.getLessonName(),
                content
        ));
    }
}
