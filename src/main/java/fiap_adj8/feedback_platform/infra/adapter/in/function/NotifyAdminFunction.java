package fiap_adj8.feedback_platform.infra.adapter.in.function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fiap_adj8.feedback_platform.application.port.out.client.AdminServiceClientPort;
import fiap_adj8.feedback_platform.application.port.out.email.EmailSender;
import fiap_adj8.feedback_platform.application.port.out.email.input.EmailInput;
import fiap_adj8.feedback_platform.domain.model.AlertMessageDetails;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class NotifyAdminFunction {

    // TODO: fazer o mesmo para o weekly report -> analisar último commit
    // TODO: deployar banco POSTGRES_16
    // TODO: deployar app
    // TODO: apontar host certo nas functions
    // TODO: segurança -> secrets... variáveis de ambiente...
    // TODO: questão de acesso...
    // TODO: monitoramento...
    // TODO: documentação

    private static final Logger logger = Logger.getLogger(NotifyAdminFunction.class.getName());

    // CORREÇÃO: Gson movido para instância final não estática para segurança de thread e CDI
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>)
                            // Context é um parâmetro de lambda/interface, não o Context do GCF
                            (src, typeOfSrc, context) ->
                                    new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            )
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<LocalDateTime>)
                            (json, typeOfT, context) ->
                                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
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

    public static class PubSubEnvelope {
        public Message message;

        public static class Message {
            public String data;
        }
    }

    @Funq("notifyAdminPubSubHandler")
    @CloudEventMapping(trigger = "google.cloud.pubsub.topic.v1.messagePublished")
    public void notifyAdminPubSubHandler(CloudEvent<PubSubEnvelope> event) {
        try {
            if (event.data() == null ||
                event.data().message == null ||
                event.data().message.data == null) {
                logger.warning("⚠️ Received Pub/Sub event with no data");
                return;
            }
            String encoded = event.data().message.data;
            AlertMessageDetails urgentFeedback = getAlertMessageDetails(encoded);
            logger.info("Alert Message details received: " + urgentFeedback);
            notifyAdmin(urgentFeedback);
        } catch (Exception e) {
            logger.severe("Error with notifyAdminPubSubHandler: " + e.getMessage());
        }
    }

    // CORREÇÃO: Método não é mais estático para poder usar 'this.gson'
    private AlertMessageDetails getAlertMessageDetails(String encoded) {
        String decoded = new String(Base64.getDecoder().decode(encoded));
        logger.info("📨 Received Pub/Sub message: " + decoded);
        String test = new String(Base64.getDecoder().decode(decoded));
        logger.info("📨 Received Pub/Sub message test: " + test);
        return gson.fromJson(test, AlertMessageDetails.class);
    }

    private void notifyAdmin(AlertMessageDetails urgentFeedback) {
        logger.info("📩 Notifying admins about feedback: " + urgentFeedback.getLessonName());

        List<String> adminEmails = adminServiceClient.getAdminEmails();
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
                .replace("{date}", urgentFeedback.getDate().toString());
    }
}