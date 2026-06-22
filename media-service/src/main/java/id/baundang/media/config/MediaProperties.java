package id.baundang.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "app.media")
public class MediaProperties {

    private int presignPutExpiryMinutes = 15;
    private int presignGetExpiryHours = 1;
    private long maxImageBytes = 10_485_760L;
    private long maxVideoBytes = 52_428_800L;
    private String allowedTypes = "image/jpeg,image/png,image/webp,video/mp4";

    public int getPresignPutExpiryMinutes() { return presignPutExpiryMinutes; }
    public void setPresignPutExpiryMinutes(int v) { this.presignPutExpiryMinutes = v; }

    public int getPresignGetExpiryHours() { return presignGetExpiryHours; }
    public void setPresignGetExpiryHours(int v) { this.presignGetExpiryHours = v; }

    public long getMaxImageBytes() { return maxImageBytes; }
    public void setMaxImageBytes(long v) { this.maxImageBytes = v; }

    public long getMaxVideoBytes() { return maxVideoBytes; }
    public void setMaxVideoBytes(long v) { this.maxVideoBytes = v; }

    public String getAllowedTypes() { return allowedTypes; }
    public void setAllowedTypes(String v) { this.allowedTypes = v; }

    public Set<String> allowedTypeSet() {
        return Arrays.stream(allowedTypes.split(","))
                .map(String::trim).collect(Collectors.toSet());
    }

    public long maxBytesFor(String contentType) {
        return contentType != null && contentType.startsWith("video/")
                ? maxVideoBytes : maxImageBytes;
    }
}
