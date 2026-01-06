package cl.jonatansoto.reader.file.file.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveDocumentResponse {
    
    @JsonProperty("operationId")
    private String operationId;
    
    @JsonProperty("gnid")
    private String gnid;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("description")
    private String description;
}

