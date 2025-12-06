package kazikd.dev.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String answer;
    private String status;
    private boolean searchingSourceDocuments;
    
    public ChatResponse(String answer, String status) {
        this.answer = answer;
        this.status = status;
        this.searchingSourceDocuments = false;
    }
}
