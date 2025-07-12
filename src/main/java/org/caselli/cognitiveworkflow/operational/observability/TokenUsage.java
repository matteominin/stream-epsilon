package org.caselli.cognitiveworkflow.operational.observability;

import lombok.Data;
import org.springframework.ai.chat.metadata.Usage;

import java.util.List;

/**
 * @author niccolocaselli
 */
@Data
public class TokenUsage {
    int completionTokens;
    int promptTokens;
    int totalTokens;

    public TokenUsage(int completionTokens, int promptTokens, int totalTokens) {
        this.completionTokens = completionTokens;
        this.promptTokens = promptTokens;
        this.totalTokens = totalTokens;
    }

    public TokenUsage(){
        this.completionTokens = 0;
        this.promptTokens = 0;
        this.totalTokens = 0;
    }

    public TokenUsage(Usage usage) {
        if(usage == null) {
            this.completionTokens = 0;
            this.promptTokens = 0;
            this.totalTokens = 0;
        } else{
            this.completionTokens = usage.getCompletionTokens();
            this.promptTokens = usage.getPromptTokens();
            this.totalTokens = usage.getTotalTokens();
        }
    }


    public TokenUsage(List<TokenUsage> tokenUsages){
        this.completionTokens = 0;
        this.promptTokens = 0;
        this.totalTokens = 0;

        for (TokenUsage tokenUsage : tokenUsages) {
            this.completionTokens += tokenUsage.getCompletionTokens();
            this.promptTokens += tokenUsage.getPromptTokens();
            this.totalTokens += tokenUsage.getTotalTokens();
        }
    }

    public boolean isEmpty() {
        return completionTokens == 0 && promptTokens == 0 && totalTokens == 0;
    }
}
