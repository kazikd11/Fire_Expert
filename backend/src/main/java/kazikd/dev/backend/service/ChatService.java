package kazikd.dev.backend.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import kazikd.dev.backend.model.ChatMessageDto;
import kazikd.dev.backend.model.GenerationResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatService {

    private final ChatModel chatModel;
    private final QdrantService qdrantService;
    private final StorageService storageService;

    @Value("${rag.system-prompt}")
    private String systemPromptTemplate;

    @Value("${rag.context-check-prompt}")
    private String contextCheckPromptTemplate;

    @Value("${rag.top-k:4}")
    private int topK;

    public ChatService(ChatModel chatModel, QdrantService qdrantService, StorageService storageService) {
        this.chatModel = chatModel;
        this.qdrantService = qdrantService;
        this.storageService = storageService;
    }


    
    private boolean checkIfContextIsSufficient(String question, String context, List<ChatMessageDto> previousMessages) {
        try {
            StringBuilder historyBuilder = new StringBuilder();
            if (previousMessages != null && !previousMessages.isEmpty()) {
                for (ChatMessageDto msg : previousMessages) {
                    historyBuilder.append(String.format("%s: %s\n", 
                        msg.getRole().equals("user") ? "Użytkownik" : "Asystent", 
                        msg.getContent()));
                }
            } else {
                historyBuilder.append("Brak wcześniejszej historii.\n");
            }
            
            String checkPrompt = contextCheckPromptTemplate
                    .replace("{question}", question)
                    .replace("{context}", context)
                    .replace("{conversationHistory}", historyBuilder.toString());
            
            Message checkMessage = new UserMessage(checkPrompt);
            Prompt prompt = new Prompt(List.of(checkMessage));
            
            ChatResponse chatResponse = chatModel.call(prompt);
            if (chatResponse == null || chatResponse.getResult() == null || 
                chatResponse.getResult().getOutput() == null) {
                log.warn("Null response from context sufficiency check");
                return false;
            }
            
            String responseText = chatResponse.getResult().getOutput().getText();
            if (responseText == null) {
                log.warn("Null text from context sufficiency check");
                return false;
            }
            
            String response = responseText.trim().toUpperCase();
            
            log.info("Context sufficiency check response: {}", response);
            return response.contains("TAK");
            
        } catch (Exception e) {
            log.error("Error checking context sufficiency: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private ChatResponse buildResponseWithTextContext(String userQuestion, String textContext, 
                                                      List<ChatMessageDto> previousMessages) {
        List<Message> messages = new ArrayList<>();
        
        Message systemMessage = new SystemPromptTemplate(systemPromptTemplate).createMessage(Map.of());
        messages.add(systemMessage);
        
        addPreviousMessages(messages, previousMessages);
        
        String contextMessage = "KONTEKST Z DOKUMENTACJI:\n\n" + textContext + 
                                "\n\nPYTANIE: " + userQuestion;
        messages.add(new UserMessage(contextMessage));
        
        return chatModel.call(new Prompt(messages));
    }
    
    private ChatResponse buildResponseWithPDFs(String userQuestion, Set<String> documentNames,
                                               List<ChatMessageDto> previousMessages) {
        try {
            List<ByteArrayResource> pdfResources = new ArrayList<>();
            for (String documentName : documentNames) {
                log.info("Fetching complete PDF: {}", documentName);
                byte[] pdfBytes = storageService.downloadFile(documentName);
                pdfResources.add(new ByteArrayResource(pdfBytes));
            }

            log.info("Sending {} complete PDF documents to Gemini", pdfResources.size());
            
            List<Message> messages = new ArrayList<>();
            
            Message systemMessage = new SystemPromptTemplate(systemPromptTemplate).createMessage(Map.of());
            messages.add(systemMessage);
            
            addPreviousMessages(messages, previousMessages);
            
            for (ByteArrayResource pdfResource : pdfResources) {
                messages.add(UserMessage.builder()
                        .text(userQuestion)
                        .media(List.of(new Media(new MimeType("application", "pdf"), pdfResource)))
                        .build());
            }

            Prompt prompt = new Prompt(messages);
            return chatModel.call(prompt);
            
        } catch (Exception e) {
            log.error("Error fetching PDF files: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate response with PDFs", e);
        }
    }
    
    private void addPreviousMessages(List<Message> messages, List<ChatMessageDto> previousMessages) {
        if (previousMessages != null && !previousMessages.isEmpty()) {
            for (ChatMessageDto prevMsg : previousMessages) {
                if ("user".equals(prevMsg.getRole())) {
                    messages.add(new UserMessage(prevMsg.getContent()));
                } else if ("assistant".equals(prevMsg.getRole())) {
                    messages.add(new org.springframework.ai.chat.messages.AssistantMessage(prevMsg.getContent()));
                }
            }
        }
    }

    private ChatResponse buildResponseWithoutRAG(String userQuestion, List<ChatMessageDto> previousMessages) {
        List<Message> messages = new ArrayList<>();
        
        Message systemMessage = new SystemPromptTemplate(systemPromptTemplate).createMessage(Map.of());
        messages.add(systemMessage);
        
        if (previousMessages != null && !previousMessages.isEmpty()) {
            for (ChatMessageDto prevMsg : previousMessages) {
                if ("user".equals(prevMsg.getRole())) {
                    messages.add(new UserMessage(prevMsg.getContent()));
                } else if ("assistant".equals(prevMsg.getRole())) {
                    messages.add(new org.springframework.ai.chat.messages.AssistantMessage(prevMsg.getContent()));
                }
            }
        }
        
        messages.add(new UserMessage(userQuestion));
        
        return chatModel.call(new Prompt(messages));
    }

    public GenerationResult generateTextResponse(String userQuestion, List<ChatMessageDto> previousMessages) {
        List<Document> relevantChunks = qdrantService.searchDocuments(userQuestion, topK);
        
        if (relevantChunks.isEmpty()) {
            log.warn("No relevant chunks found for question: {}", userQuestion);
            ChatResponse response = buildResponseWithoutRAG(userQuestion, previousMessages);
            String answer = response.getResult().getOutput().getText();
            return new GenerationResult(answer, false);
        }

        Set<String> documentNames = new HashSet<>();
        StringBuilder contextBuilder = new StringBuilder();
        
        for (Document doc : relevantChunks) {
            String documentName = (String) doc.getMetadata().get("document_name");
            String paragraphId = (String) doc.getMetadata().get("paragraph_id");
            String paragraphTitle = (String) doc.getMetadata().get("paragraph_title");
            
            if (documentName != null) {
                documentNames.add(documentName);
            }
            
            contextBuilder.append(String.format("[%s - %s: %s]\n%s\n\n",
                    documentName, paragraphId, paragraphTitle, doc.getText()));
        }
        
        String textContext = contextBuilder.toString();
        
        log.info("Found {} relevant chunks from {} documents", 
                relevantChunks.size(), documentNames.size());
        
        boolean contextIsSufficient = checkIfContextIsSufficient(userQuestion, textContext, previousMessages);
        
        if (contextIsSufficient) {
            log.info("Context is sufficient - answering without PDFs");
            ChatResponse response = buildResponseWithTextContext(userQuestion, textContext, previousMessages);
            String answer = response.getResult().getOutput().getText();
            return new GenerationResult(answer, false);
        }
        
        log.info("Context is NOT sufficient - fetching full PDFs");
        ChatResponse response = buildResponseWithPDFs(userQuestion, documentNames, previousMessages);
        String answer = response.getResult().getOutput().getText();
        return new GenerationResult(answer, true);
    }
}