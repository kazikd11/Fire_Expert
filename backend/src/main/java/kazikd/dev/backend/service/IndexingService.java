package kazikd.dev.backend.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class IndexingService {

    private final QdrantService qdrantService;
    private final StorageService storageService;
    private final PdfParsingService pdfParsingService;

    public IndexingService(QdrantService qdrantService, StorageService storageService, PdfParsingService pdfParsingService) {
        this.qdrantService = qdrantService;
        this.storageService = storageService;
        this.pdfParsingService = pdfParsingService;
    }

    public int indexAllDocuments() {
        log.info("Starting indexing of all documents from GCS bucket");
        
        try {
            List<String> pdfFiles = storageService.listAllPdfFiles();
            log.info("Found {} PDF files to index", pdfFiles.size());
            
            int totalChunksIndexed = 0;
            
            for (String fileName : pdfFiles) {
                try {
                    int chunksIndexed = indexDocument(fileName);
                    totalChunksIndexed += chunksIndexed;
                    log.info("Successfully indexed {} with {} chunks", fileName, chunksIndexed);
                } catch (Exception e) {
                    log.error("Failed to index document {}: {}", fileName, e.getMessage(), e);
                }
            }
            
            log.info("Indexing completed. Total chunks indexed: {}", totalChunksIndexed);
            return totalChunksIndexed;
            
        } catch (Exception e) {
            log.error("Error during indexing: {}", e.getMessage(), e);
            throw new RuntimeException("Indexing failed", e);
        }
    }

    public int indexDocument(String fileName) {
        log.info("Indexing document with Document AI: {}", fileName);
        
        String gcsUri = storageService.getGcsUriForFile(fileName);
        
        List<Document> documents = pdfParsingService.extractDocuments(fileName, gcsUri);
        
        if (documents.isEmpty()) {
            log.warn("No chunks extracted from {}", fileName);
            return 0;
        }
        
        qdrantService.addDocuments(documents);
        
        log.info("Indexed {} chunks from {}", documents.size(), fileName);
        return documents.size();
    }
}