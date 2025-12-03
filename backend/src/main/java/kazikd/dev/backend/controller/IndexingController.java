package kazikd.dev.backend.controller;

import java.util.Map;

import kazikd.dev.backend.service.QdrantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import kazikd.dev.backend.service.IndexingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/index")
public class IndexingController {

    private final IndexingService indexingService;

    private final QdrantService qdrantService;

    @Autowired
    public IndexingController(IndexingService indexingService, QdrantService qdrantService) {
        this.indexingService = indexingService;
        this.qdrantService = qdrantService;
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteIndex() {
        qdrantService.clearCollection("frctrl_documents_2");
        log.info("Cleared Qdrant collection: frctrl_documents");
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cleared Qdrant collection: frctrl_documents"
        ));
    }

    /**
     * Index all PDF documents from GCS bucket
     * POST /api/v1/index
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> startIndexing() {
        try {
            log.info("Starting indexing of all documents...");
            long startTime = System.currentTimeMillis();
            
            int indexedCount = indexingService.indexAllDocuments();
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("Indexing completed successfully. {} paragraphs indexed in {}ms", 
                    indexedCount, duration);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "paragraphs_indexed", indexedCount,
                    "duration_ms", duration,
                    "message", "Successfully indexed " + indexedCount + " paragraphs"
            ));
            
        } catch (Exception e) {
            log.error("Indexing failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Indexing failed: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Index a single document by filename
     * POST /api/v1/index/document?filename=example.pdf
     */
    @PostMapping("/document")
    public ResponseEntity<Map<String, Object>> indexDocument(@RequestParam String filename) {
        try {
            log.info("Starting indexing of document: {}", filename);
            long startTime = System.currentTimeMillis();
            
            int paragraphsIndexed = indexingService.indexDocument(filename);
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("Document {} indexed successfully. {} paragraphs in {}ms", 
                    filename, paragraphsIndexed, duration);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "document", filename,
                    "paragraphs_indexed", paragraphsIndexed,
                    "duration_ms", duration
            ));
            
        } catch (Exception e) {
            log.error("Failed to index document {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "document", filename,
                    "message", "Failed to index: " + e.getMessage()
            ));
        }
    }
}