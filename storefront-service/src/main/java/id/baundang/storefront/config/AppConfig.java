package id.baundang.storefront.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PricingProperties.class)
public class AppConfig {

    @Value("${app.services.template}")
    private String templateServiceUrl;

    @Bean
    public RestClient templateRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(templateServiceUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
