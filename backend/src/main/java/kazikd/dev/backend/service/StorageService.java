package kazikd.dev.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.google.auth.Credentials;
import com.google.auth.ServiceAccountSigner;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.documentai.v1.GcsDocument;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import static com.google.cloud.storage.Storage.BlobListOption.currentDirectory;
import static com.google.cloud.storage.Storage.BlobListOption.prefix;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StorageService {

    private static final String MIME_TYPE_PDF = "application/pdf";
    private static final String MIME_TYPE_PNG = "image/png";
    private static final String GCS_URI_TEMPLATE = "gs://%s/%s";
    private static final String PAGE_IMAGES_FOLDER = "page-images/";
    private static final String KEY_FILE_NAME = "dupa.json";



    private final Storage storage;
    
//    private final Map<String, byte[]> pdfCache = new HashMap<>();

    @Value("${document-ai.gcs-bucket-name}")
    private String gcsBucketName;

    public StorageService(Storage storage) {
        this.storage = storage;
    }

    private String getGcsUri(String fileName) {
        return String.format(GCS_URI_TEMPLATE, gcsBucketName, fileName);
    }

    public List<String> listAllPdfFiles() {
        return storage.list(gcsBucketName, prefix(""), currentDirectory()).
                streamValues()
                .map(Blob::getName)
                .filter(name -> name.toLowerCase().endsWith(".pdf"))
                .toList();
    }

    public List<GcsDocument> getAllGcsDocuments() {
        return listAllPdfFiles()
                .stream()
                .map(name ->
                        GcsDocument.newBuilder()
                                .setGcsUri(getGcsUri(name))
                                .setMimeType(MIME_TYPE_PDF)
                                .build())
                .toList();
    }
    

    public byte[] downloadFile(String fileName) {

        
        log.info("Downloading file from GCS: {}", fileName);
        try {
            Blob blob = storage.get(gcsBucketName, fileName);
            if (blob == null) {
                throw new RuntimeException("File not found: " + fileName);
            }


            return blob.getContent();
        } catch (Exception e) {
            log.error("Error downloading file {}: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to download file: " + fileName, e);
        }
    }
    
    public String getGcsUriForFile(String fileName) {
        return getGcsUri(fileName);
    }
}
