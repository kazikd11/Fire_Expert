package kazikd.dev.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerationResult {
    private String answer;
    private boolean usedFullDocuments;
}
