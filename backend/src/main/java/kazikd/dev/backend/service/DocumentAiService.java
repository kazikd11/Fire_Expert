package kazikd.dev.backend.service;

import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DocumentAiService {

    @Value("${document-ai.processor-name}")
    private String processorName;

    @Value("${document-ai.gcs-bucket-name}")
    private String gcsBucketName;

    private final Storage storage;
    private final DocumentProcessorServiceClient documentAiClient;

    public DocumentAiService(Storage storage, DocumentProcessorServiceClient documentAiClient) {
        this.storage = storage;
        this.documentAiClient = documentAiClient;
    }

    public String processPdfAndGetText(String fileName) throws IOException {

        BlobId blobId = BlobId.of(gcsBucketName, fileName);
        byte[] fileBytes = storage.readAllBytes(blobId);

        Document document =
                Document.newBuilder()
                        .setContent(ByteString.copyFrom(fileBytes))
                        .setMimeType("application/pdf")
                        .build();

        ProcessRequest request = ProcessRequest.newBuilder()
                .setName(processorName)
                .setInlineDocument(document)
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
