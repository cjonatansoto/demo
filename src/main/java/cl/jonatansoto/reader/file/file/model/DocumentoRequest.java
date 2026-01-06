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
public class DocumentoRequest {
    
    @JsonProperty("fileBase64")
    private String fileBase64;
    
    @JsonProperty("documentClass")
    private String documentClass;
    
    @JsonProperty("documentType")
    private String documentType;
    
    @JsonProperty("fileName")
    private String fileName;
    
    @JsonProperty("clientId")
    private String clientId;
    
    @JsonProperty("documentServer")
    private String documentServer;
}
