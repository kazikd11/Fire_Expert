package kazikd.dev.backend.config;

import java.io.IOException;

import org.springframework.ai.vertexai.embedding.VertexAiEmbeddingConnectionDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

@Configuration
public class GcpConfig {

    @Bean
    public Storage googleCloudStorage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    @Bean
    public DocumentProcessorServiceClient documentProcessorServiceClient(
            @Value("${document-ai.endpoint}") String endpoint
    ) throws IOException {
        return DocumentProcessorServiceClient.create(DocumentProcessorServiceSettings.newBuilder().setEndpoint(endpoint).build());
    }

    @Bean
    public VertexAiEmbeddingConnectionDetails vertexAiEmbeddingConnectionDetails(
            @Value("${spring.ai.vertex.ai.embedding.project-id}") String projectId,
            @Value("${spring.ai.vertex.ai.embedding.location}") String location) {
        return VertexAiEmbeddingConnectionDetails.builder()
                .projectId(projectId)
                .location(location)
                .build();
    }
}
