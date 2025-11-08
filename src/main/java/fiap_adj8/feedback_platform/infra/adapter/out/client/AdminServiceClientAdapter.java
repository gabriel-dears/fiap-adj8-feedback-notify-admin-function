package fiap_adj8.feedback_platform.infra.adapter.out.client;

import fiap_adj8.feedback_platform.application.port.out.client.AdminServiceClientPort;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@Singleton
public class AdminServiceClientAdapter implements AdminServiceClientPort {

    private final AdminServiceClient adminServiceClient;

    public AdminServiceClientAdapter(@RestClient AdminServiceClient adminServiceClient) {
        this.adminServiceClient = adminServiceClient;
    }

    @Override
    public List<String> getAdminEmails() {
        return adminServiceClient.getAdminEmails();
    }
}
