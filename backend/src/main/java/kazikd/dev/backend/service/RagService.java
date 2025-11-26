package kazikd.dev.backend.service;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    @Value("${rag.system-prompt}")
    private String systemPromptTemplate;

    @Value("${rag.top-k:4}")
    private int topK;

    // Wstrzykiwanie przez konstruktor
    public RagService(ChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    public ChatResponse generateResponse(String userQuestion) {

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder().query(userQuestion).topK(topK).build());

        String context = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(systemPromptTemplate);

        Message systemMessage =
                promptTemplate.createMessage(Map.of("context", context));

        UserMessage userMessage = new UserMessage(userQuestion);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        return chatModel.call(prompt);
    }

    public String generateTextResponse(String userQuestion) {
        return generateResponse(userQuestion).getResult().getOutput().getText();
    }
}