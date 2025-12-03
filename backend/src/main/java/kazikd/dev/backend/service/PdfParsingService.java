package kazikd.dev.backend.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PdfParsingService {

    private final DocumentAiService documentAiService;
    private final TokenTextSplitter textSplitter;

    public PdfParsingService(DocumentAiService documentAiService) {
        this.documentAiService = documentAiService;
        this.textSplitter = new TokenTextSplitter(1500, 400, 100, 10000, true);
    }

    public List<org.springframework.ai.document.Document> extractDocuments(String fileName, String gcsUri) {
        log.info("Starting PDF parsing with Document AI for: {}", fileName);
        
        try {
            String fullText = documentAiService.processPdfAndGetText(fileName);
            
            log.debug("Document AI extracted {} characters from {}", fullText.length(), fileName);
            
            Map<String, Object> baseMetadata = new HashMap<>();
            baseMetadata.put("document_name", fileName);
            baseMetadata.put("gcs_uri", gcsUri);
            baseMetadata.put("source", "document-ai");
            
            org.springframework.ai.document.Document sourceDoc =
                new org.springframework.ai.document.Document(fullText, baseMetadata);
            
            List<org.springframework.ai.document.Document> chunks = textSplitter.split(sourceDoc);
            
            List<org.springframework.ai.document.Document> filteredChunks = new java.util.ArrayList<>();
            int chunkIndex = 0;
            
            for (org.springframework.ai.document.Document chunk : chunks) {
                String text = chunk.getText();
                
                if (text.length() < 100) {
                    log.debug("Skipping short chunk ({} chars)", text.length());
                    continue;
                }
                
                Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                metadata.put("chunk_index", chunkIndex++);
                
                filteredChunks.add(new org.springframework.ai.document.Document(text, metadata));
            }
            
            log.info("Created {} text chunks from {} (filtered from {} raw chunks)", 
                    filteredChunks.size(), fileName, chunks.size());
            return filteredChunks;
            
        } catch (IOException e) {
            log.error("Error processing PDF with Document AI {}: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to process PDF: " + fileName, e);
        }
    }
}
