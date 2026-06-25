package id.baundang.common;

import id.baundang.common.logging.ContentCachingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public FilterRegistrationBean<ContentCachingFilter> contentCachingFilter() {
        FilterRegistrationBean<ContentCachingFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new ContentCachingFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(1);
        reg.setName("contentCachingFilter");
        return reg;
    }
}
