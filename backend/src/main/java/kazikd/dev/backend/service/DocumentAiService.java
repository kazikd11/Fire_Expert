package kazikd.dev.backend.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.documentai.v1.BatchDocumentsInputConfig;
import com.google.cloud.documentai.v1.BatchProcessMetadata;
import com.google.cloud.documentai.v1.BatchProcessRequest;
import com.google.cloud.documentai.v1.BatchProcessResponse;
import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.GcsDocument;
import com.google.cloud.documentai.v1.GcsDocuments;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        log.info("Starting Document AI processing for: {}", fileName);
        
        String gcsUri = String.format("gs://%s/%s", gcsBucketName, fileName);

        return processPdfBatch(fileName, gcsUri);
    }

    private String processPdfBatch(String fileName, String gcsUri) throws IOException {
        log.info("Processing PDF with batch API: {}", fileName);
        
        try {
            GcsDocument gcsDocument = GcsDocument.newBuilder()
                    .setGcsUri(gcsUri)
                    .setMimeType("application/pdf")
                    .build();
            
            GcsDocuments gcsDocuments = GcsDocuments.newBuilder()
                    .addDocuments(gcsDocument)
                    .build();
            
            BatchDocumentsInputConfig inputConfig = BatchDocumentsInputConfig.newBuilder()
                    .setGcsDocuments(gcsDocuments)
                    .build();
            
            String outputGcsUri = String.format("gs://%s/document-ai-output/%s/", gcsBucketName, fileName);
            
            com.google.cloud.documentai.v1.DocumentOutputConfig.GcsOutputConfig gcsOutputConfig = 
                com.google.cloud.documentai.v1.DocumentOutputConfig.GcsOutputConfig.newBuilder()
                    .setGcsUri(outputGcsUri)
                    .build();
            
            com.google.cloud.documentai.v1.DocumentOutputConfig documentOutputConfig = 
                com.google.cloud.documentai.v1.DocumentOutputConfig.newBuilder()
                    .setGcsOutputConfig(gcsOutputConfig)
                    .build();
            
            BatchProcessRequest batchRequest = BatchProcessRequest.newBuilder()
                    .setName(processorName)
                    .setInputDocuments(inputConfig)
                    .setDocumentOutputConfig(documentOutputConfig)
                    .build();
            
            log.info("Submitting batch process request for {}", fileName);
            OperationFuture<BatchProcessResponse, BatchProcessMetadata> operation = 
                    documentAiClient.batchProcessDocumentsAsync(batchRequest);
            
            log.info("Batch processing started, waiting for completion (this may take several minutes)...");
            
            operation.get(30, TimeUnit.MINUTES);
            
            log.info("Batch processing completed for {}", fileName);
            
            String outputPrefix = String.format("document-ai-output/%s/", fileName);
            String extractedText = readBatchOutputFromGcs(outputPrefix);
            
            log.info("Extracted {} characters from {}", extractedText.length(), fileName);
            return extractedText;
            
        } catch (Exception e) {
            log.error("Error in batch processing for {}: {}", fileName, e.getMessage(), e);
            
            log.warn("Falling back to synchronous processing...");
            return processPdfSync(fileName, gcsUri);
        }
    }

    private String processPdfSync(String fileName, String gcsUri) throws IOException {
        log.info("Processing PDF synchronously: {}", fileName);
        
        try {
            GcsDocument gcsDocument = GcsDocument.newBuilder()
                    .setGcsUri(gcsUri)
                    .setMimeType("application/pdf")
                    .build();
            
            ProcessRequest request = ProcessRequest.newBuilder()
                    .setName(processorName)
                    .setGcsDocument(gcsDocument)
                    .build();
            
            ProcessResponse response = documentAiClient.processDocument(request);
            Document processedDocument = response.getDocument();
            String extractedText = processedDocument.getText();
            
            log.info("Synchronous processing completed. Extracted {} characters", extractedText.length());
            return extractedText;
            
        } catch (Exception e) {
            log.error("Error in synchronous processing: {}", e.getMessage(), e);
            throw new IOException("Document AI processing failed: " + e.getMessage(), e);
        }
    }

    private String readBatchOutputFromGcs(String outputPrefix) throws IOException {
        log.debug("Reading batch output from GCS: {}", outputPrefix);
        
        Iterable<Blob> blobs = storage.list(gcsBucketName, 
                Storage.BlobListOption.prefix(outputPrefix)).iterateAll();
        
        StringBuilder fullText = new StringBuilder();
        
        for (Blob blob : blobs) {
            if (blob.getName().endsWith(".json")) {
                log.debug("Reading output file: {}", blob.getName());
                
                byte[] jsonBytes = blob.getContent();
                String jsonString = new String(jsonBytes);
                
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(jsonString);
                    String text = root.path("text").asText();
                    
                    if (text != null && !text.isEmpty()) {
                        fullText.append(text).append("\n");
                        log.debug("Extracted {} characters from {}", text.length(), blob.getName());
                    }
                    
                } catch (Exception e) {
                    log.error("Error parsing Document AI JSON output: {}", e.getMessage(), e);
                }
            }
        }
        
        if (fullText.length() == 0) {
            throw new IOException("No text extracted from Document AI batch output");
        }
        
        return fullText.toString();
    }
}
