package kazikd.dev.backend.config;

import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.ai.vertexai.embedding.VertexAiEmbeddingConnectionDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class GcpConfig {

    //storage
    @Bean
    public Storage googleCloudStorage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    //document AI
    @Bean
    public DocumentProcessorServiceClient documentProcessorServiceClient() throws IOException {
        return DocumentProcessorServiceClient.create(DocumentProcessorServiceSettings.newBuilder().setEndpoint("eu-documentai.googleapis.com:443").build());
    }

    //vertex AI embedding
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
