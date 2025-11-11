package fiap_adj8.feedback_platform.infra.adapter.out.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/user")
@RegisterRestClient(configKey = "feedback-app-api")
public interface AdminServiceClient {

    @GET
    @Path("/admin/email")
    List<String> getAdminEmails();

}
