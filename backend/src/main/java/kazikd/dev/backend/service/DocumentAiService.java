package kazikd.dev.backend.service;

import java.io.IOException;

import com.google.cloud.documentai.v1.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.cloud.storage.Storage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocumentAiService {

    @Value("${document-ai.processor-name}")
    private String processorName;

    @Value("${document-ai.gcs-bucket-name}")
    private String gcsBucketName;

    private final DocumentProcessorServiceClient documentAiClient;

    public DocumentAiService(DocumentProcessorServiceClient documentAiClient) {
        this.documentAiClient = documentAiClient;
    }

    public String processPdfAndGetText(String fileName) throws IOException {

        String gcsUri = "gs://" + gcsBucketName + "/" + fileName;

        GcsDocument gcsDocument = GcsDocument.newBuilder()
                .setGcsUri(gcsUri)
                .setMimeType("application/pdf")
                .build();

        ProcessRequest request = ProcessRequest.newBuilder()
                .setName(processorName)
                .setSkipHumanReview(true)
                .setGcsDocument(gcsDocument)
                .build();


        try {
            ProcessResponse response = documentAiClient.processDocument(request);
            Document processedDocument = response.getDocument();

            return processedDocument.getText();

        } catch (Exception e) {
            throw new IOException("Document AI error: " + e.getMessage(), e);
        }
    }
}
