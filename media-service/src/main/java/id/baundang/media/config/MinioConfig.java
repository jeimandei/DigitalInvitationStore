package id.baundang.media.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${app.minio.endpoint}")
    private String endpoint;

    @Value("${app.minio.access-key}")
    private String accessKey;

    @Value("${app.minio.secret-key}")
    private String secretKey;

    @Value("${app.minio.bucket.templates}")
    private String templatesBucket;

    @Value("${app.minio.bucket.couples}")
    private String couplesBucket;

    @Value("${app.minio.bucket.admin}")
    private String adminBucket;

    @Bean
    MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @PostConstruct
    void ensureBuckets() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        ensureBucket(client, templatesBucket, publicReadPolicy(templatesBucket));
        ensureBucket(client, couplesBucket, null);
        ensureBucket(client, adminBucket, null);
    }

    private void ensureBucket(MinioClient client, String bucket, String policy) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
            if (policy != null) {
                client.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket).config(policy).build());
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket {}: {}", bucket, e.getMessage());
        }
    }

    private String publicReadPolicy(String bucket) {
        return """
                {
                  "Version":"2012-10-17",
                  "Statement":[{
                    "Effect":"Allow",
                    "Principal":{"AWS":["*"]},
                    "Action":["s3:GetObject"],
                    "Resource":["arn:aws:s3:::%s/*"]
                  }]
                }
                """.formatted(bucket);
    }
}
