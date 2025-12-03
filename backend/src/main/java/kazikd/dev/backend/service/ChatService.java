package kazikd.dev.backend.service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MimeType;

@Slf4j
@Service
public class ChatService {

    private final ChatModel chatModel;
    private final QdrantService qdrantService;
    private final StorageService storageService;

    @Value("${rag.system-prompt}")
    private String systemPromptTemplate;

    @Value("${rag.top-k:4}")
    private int topK;

    public ChatService(ChatModel chatModel, QdrantService qdrantService, StorageService storageService) {
        this.chatModel = chatModel;
        this.qdrantService = qdrantService;
        this.storageService = storageService;
    }

    public ChatResponse generateResponse(String userQuestion) {

        List<Document> relevantChunks = qdrantService.searchDocuments(userQuestion, 4);
        
        if (relevantChunks.isEmpty()) {
            log.warn("No relevant chunks found for question: {}", userQuestion);
            return chatModel.call(new Prompt(new UserMessage(userQuestion)));
        }

        Set<String> documentNames = new HashSet<>();
        
        for (Document doc : relevantChunks) {
            String documentName = (String) doc.getMetadata().get("document_name");
            if (documentName != null) {
                documentNames.add(documentName);
            }
        }
        
        log.info("Found {} relevant chunks from {} documents for RAG", 
                relevantChunks.size(), documentNames.size());
        
        // Step 3: Fetch complete PDFs and send as media files to Gemini
        try {
            List<ByteArrayResource> pdfResources = new ArrayList<>();

            for (String documentName : documentNames) {
                log.info("Fetching complete PDF: {}", documentName);

                byte[] pdfBytes = storageService.downloadFile(documentName);
//                ClassPathResource pdfResource = new ClassPathResource(documentName)
//

                ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes);

                pdfResources.add(pdfResource);
            }

            String docName = documentNames.iterator().next();
            // jak tutaj dodać pdfa do gemini
//            var pdfData = new ClassPathResource(docName);
            String uri = storageService.getPresignedUrl(docName);
//            Resource resource = new UrlResource(uri);


            log.info("Sending {} complete PDF documents to Gemini", pdfResources.size());
            
            // Create system message
            String systemPrompt = "Jestes ekspert asystentem od dokumentacji technicznej." +
                    "Przeanalizuj dokladnie dostarczone dokumenty PDF i wykorzystaj ich tresc do odpowiedzi na pytanie uzytkownika. " +
                "Na podstawie dostarczonych dokumentow w sposob rzeczowy i mozlwie jak najlbizej tekstu zrodlowego odpowiedz na pytanie." +
                    "W odpowiedzi nie pisz nic w stylu na podstawie dostarczonego dokumentu, bo dokumenty NIE są dostarczane przez uzytkownika. ";
            
            Message systemMessage = new SystemPromptTemplate(systemPromptTemplate).createMessage(Map.of());
            
            List<Message> messages = new ArrayList<>();
            messages.add(systemMessage);

//            UserMessage userMessage = new UserMessage.Builder().text(userQuestion).media(
//                    new Media(new MimeType("application","pdf"), pdfData)
//            ).build();

//            var srata = new ClassPathResource("/5.pdf");
//
//
//            messages.add(UserMessage.builder().text(userQuestion).media(
//                    List.of(new Media(new MimeType("application", "pdf"), srata))
//            ).build());


//            messages.add(new UserMessage(userQuestion, List.of(new Media(new MimeType("application","pdf"), new ClassPathResource(docName)))));
            

            for (var pdfResource : pdfResources) {
                messages.add(UserMessage.builder().text(userQuestion).media(
                        List.of(new Media(new MimeType("application", "pdf"), pdfResource))
                ).build());
            }
//            messages.add(new UserMessage(resource));
//            messages.add(new UserMessage(userQuestion));


//new Media()

            Prompt prompt = new Prompt(messages);
            return chatModel.call(prompt);
            
        } catch (Exception e) {
            log.error("Error fetching PDF files: {}", e.getMessage(), e);
            throw new RuntimeException(e);
//            return generateTextOnlyResponse(userQuestion, relevantChunks);
        }
    }

    private ChatResponse generateTextOnlyResponse(String userQuestion, List<Document> paragraphDocuments) {
        log.warn("Using text-only fallback for response generation");
        
        String context = buildContextFromParagraphs(paragraphDocuments);
        
        Message systemMessage = new SystemPromptTemplate(systemPromptTemplate).createMessage(Map.of("context", context));
        UserMessage userMessage = new UserMessage(userQuestion);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        return chatModel.call(prompt);
    }

    private String buildContextFromParagraphs(List<Document> paragraphDocuments) {
        return paragraphDocuments.stream()
                .map(doc -> {
                    String documentName = (String) doc.getMetadata().get("document_name");
                    String paragraphId = (String) doc.getMetadata().get("paragraph_id");
                    String paragraphTitle = (String) doc.getMetadata().get("paragraph_title");
                    String text = doc.getText();
                    
                    return String.format("**[%s - %s: %s]**\n%s", 
                            documentName, paragraphId, paragraphTitle, text);
                })
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    public String generateTextResponse(String userQuestion) {
        return generateResponse(userQuestion).getResult().getOutput().getText();
    }
}