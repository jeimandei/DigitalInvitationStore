package id.baundang.storefront.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Value("${app.services.template}")
    private String templateServiceUrl;

    @Value("${app.services.order}")
    private String orderServiceUrl;

    @Bean
    public RestClient templateRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(templateServiceUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public RestClient orderRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(orderServiceUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
