package id.baundang.storefront.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.pricing")
public class PricingProperties {

    private List<Tier> tiers = List.of();

    public List<Tier> getTiers() { return tiers; }
    public void setTiers(List<Tier> tiers) { this.tiers = tiers; }

    public static class Tier {
        private String name;
        private long price;
        private String description;
        private List<String> features = List.of();
        private boolean highlight;

        public String getName()          { return name; }
        public void setName(String v)    { this.name = v; }
        public long getPrice()           { return price; }
        public void setPrice(long v)     { this.price = v; }
        public String getDescription()   { return description; }
        public void setDescription(String v) { this.description = v; }
        public List<String> getFeatures(){ return features; }
        public void setFeatures(List<String> v) { this.features = v; }
        public boolean isHighlight()     { return highlight; }
        public void setHighlight(boolean v) { this.highlight = v; }

        public String getFormattedPrice() {
            return "Rp " + String.format("%,d", price).replace(',', '.');
        }
    }
}
