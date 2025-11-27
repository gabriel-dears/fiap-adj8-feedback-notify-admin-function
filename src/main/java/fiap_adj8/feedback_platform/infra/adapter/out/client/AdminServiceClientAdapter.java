package fiap_adj8.feedback_platform.infra.adapter.out.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fiap_adj8.feedback_platform.application.port.out.client.AdminServiceClientPort;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class AdminServiceClientAdapter implements AdminServiceClientPort {

    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public AdminServiceClientAdapter() {
        this.baseUrl = System.getenv("ADMIN_SERVICE_BASE_URL");

        if (this.baseUrl == null || this.baseUrl.isBlank()) {
            throw new RuntimeException("ADMIN_SERVICE_BASE_URL não definida");
        }
    }

    @Override
    public List<String> getAdminEmails() {
        try {
            String authHeader = System.getenv("ADMIN_SERVICE_AUTH");

            if (authHeader == null || authHeader.isBlank()) {
                throw new RuntimeException("ADMIN_SERVICE_AUTH não definida");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(baseUrl + "/user/admin/email"))
                    .header("Accept", "application/json")
                    .header("Authorization", "Basic " + authHeader)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Failed to fetch admin emails. Status: " + response.statusCode() +
                        " Body: " + response.body()
                );
            }

            Type listType = new TypeToken<List<String>>() {}.getType();
            return gson.fromJson(response.body(), listType);

        } catch (Exception e) {
            throw new RuntimeException("Error calling Admin Service", e);
        }
    }
}

