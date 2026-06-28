package id.baundang.storefront.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.midtrans")
public class MidtransProperties {

    private String clientKey;
    private String snapJsUrl = "https://app.sandbox.midtrans.com/snap/snap.js";

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getSnapJsUrl() {
        return snapJsUrl;
    }

    public void setSnapJsUrl(String snapJsUrl) {
        this.snapJsUrl = snapJsUrl;
    }
}
