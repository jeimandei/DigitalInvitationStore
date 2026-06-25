package id.baundang.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.pricing")
public class PricingProperties {

    private List<Tier> tiers = List.of();

    public List<Tier> getTiers() {
        return tiers;
    }

    public void setTiers(List<Tier> tiers) {
        this.tiers = tiers;
    }

    public Tier forTier(int tierNum) {
        if (tierNum < 1 || tierNum > tiers.size()) {
            throw new IllegalArgumentException("Invalid tier: " + tierNum);
        }
        return tiers.get(tierNum - 1);
    }

    public static class Tier {
        private String name;
        private long price;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getPrice() { return price; }
        public void setPrice(long price) { this.price = price; }
    }
}
