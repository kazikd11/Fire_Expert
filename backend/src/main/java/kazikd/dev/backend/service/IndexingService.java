package kazikd.dev.backend.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class IndexingService {

    private final VectorStore vectorStore;
    private final DocumentAiService documentAiService;
    private final TextSplitter textSplitter;

    @Autowired
    public IndexingService(VectorStore vectorStore, DocumentAiService documentAiService) {

        this.vectorStore = vectorStore;
        this.documentAiService = documentAiService;
        this.textSplitter = new TokenTextSplitter();
    }

    public int indexDocument(String fileName) throws IOException {

        String rawText = documentAiService.processPdfAndGetText(fileName);

        Document baseDocument = new Document(rawText,
                Map.of("file_name", fileName, "source_service", "GCP Document AI"));

        List<Document> chunks = textSplitter.split(Collections.singletonList(baseDocument));

        vectorStore.add(chunks);

        return chunks.size();
    }
}
