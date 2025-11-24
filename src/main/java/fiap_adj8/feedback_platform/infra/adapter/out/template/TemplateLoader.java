package fiap_adj8.feedback_platform.infra.adapter.out.template;

import fiap_adj8.feedback_platform.application.port.out.template.TemplateProvider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class TemplateLoader implements TemplateProvider {
    @Override
    public String getTemplate(String name) {
        try (InputStream is = getClass().getResourceAsStream("/templates/" + name)) {
            if (is == null) throw new RuntimeException("Template not found: " + name);
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template", e);
        }
    }
}
