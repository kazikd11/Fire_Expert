package kazikd.dev.backend.service;

import io.qdrant.client.QdrantClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class QdrantService {

    private final VectorStore vectorStore;

    private final QdrantClient qdrantClient;

    public QdrantService(VectorStore vectorStore, QdrantClient qdrantClient) {
        this.vectorStore = vectorStore;
        this.qdrantClient = qdrantClient;
    }

    public void addDocuments(List<Document> chunks) {
        try {
            vectorStore.add(chunks);
        }
        catch (Exception e) {
            log.error("Error adding documents to Qdrant vector store: {}", e.getMessage(), e);
        }
    }

    public List<Document> searchDocuments(String query, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build());
    }

    public void clearCollection(String collectionName) {
        try {
            qdrantClient.deleteCollectionAsync(collectionName).get();
            log.info("Cleared Qdrant collection: {}", collectionName);
        } catch (Exception e) {
            log.error("Error clearing Qdrant collection {}: {}", collectionName, e.getMessage(), e);
        }
    }
}
