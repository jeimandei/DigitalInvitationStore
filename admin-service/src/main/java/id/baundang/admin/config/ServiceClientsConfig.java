package id.baundang.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ServiceClientsConfig {

    @Bean("orderRestClient")
    RestClient orderRestClient(@Value("${app.services.order-service}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean("invitationRestClient")
    RestClient invitationRestClient(@Value("${app.services.invitation-service}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean("templateRestClient")
    RestClient templateRestClient(@Value("${app.services.template-service}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean("authRestClient")
    RestClient authRestClient(@Value("${app.services.auth-service}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
