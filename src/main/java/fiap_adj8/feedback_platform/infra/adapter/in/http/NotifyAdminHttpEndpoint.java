package fiap_adj8.feedback_platform.infra.adapter.in.http;

import com.google.gson.Gson;
import fiap_adj8.feedback_platform.domain.model.AlertMessageDetails;
import fiap_adj8.feedback_platform.infra.adapter.in.function.NotifyAdminFunction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.Base64;

@ApplicationScoped
@Path("/test/notify-admin")
public class NotifyAdminHttpEndpoint {


    private final NotifyAdminFunction notifyAdminFunction;

    public NotifyAdminHttpEndpoint(NotifyAdminFunction notifyAdminFunction) {
        this.notifyAdminFunction = notifyAdminFunction;
    }

    @POST
    public Response triggerManually() {
        AlertMessageDetails alert = new AlertMessageDetails(
                "John Doe",
                "Docker Fundamentals",
                "The feedback form is not saving correctly.",
                "Low",
                LocalDateTime.now()
        );

        String json = new Gson().toJson(alert);
        String encoded = Base64.getEncoder().encodeToString(json.getBytes());

        NotifyAdminFunction.PubSubMessage mockMessage = new NotifyAdminFunction.PubSubMessage();
        mockMessage.data = encoded;
        notifyAdminFunction.accept(mockMessage, null);
        return Response.ok("Admin notification triggered successfully").build();
    }
}
