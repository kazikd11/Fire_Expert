package kazikd.dev.backend.config;

import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
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
        return DocumentProcessorServiceClient.create();
    }
}
