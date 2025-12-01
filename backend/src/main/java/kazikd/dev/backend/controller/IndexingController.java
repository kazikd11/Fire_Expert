package kazikd.dev.backend.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import kazikd.dev.backend.service.IndexingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/index")
public class IndexingController {

    private final IndexingService indexingService;
    private final Storage storage;

    @Value("${document-ai.gcs-bucket-name}")
    private String gcsBucketName;

    @Autowired
    public IndexingController(IndexingService indexingService, Storage storage) {
        this.indexingService = indexingService;
        this.storage = storage;
    }

    @PostMapping
    public ResponseEntity<String> startIndexing(
            @RequestParam String fileName,
            @RequestBody byte[] pdfData) {

        if (fileName == null || fileName.isBlank()) {
            return ResponseEntity.badRequest().body("Brak nazwy pliku.");
        }
        
        if (pdfData.length == 0) {
            return ResponseEntity.badRequest().body("Plik jest pusty.");
        }
        
        try {
            BlobId blobId = BlobId.of(gcsBucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("application/pdf")
                    .build();
            storage.create(blobInfo, pdfData);

            int indexedCount = indexingService.indexDocument(fileName);
            return ResponseEntity.ok("Indeksowanie pliku '" + fileName + "' zakończone pomyślnie. Zaindeksowano " + indexedCount + " fragmentów.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Błąd Document AI lub IO podczas indeksowania: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Wystąpił nieoczekiwany błąd: " + e.getMessage());
        }
    }
}