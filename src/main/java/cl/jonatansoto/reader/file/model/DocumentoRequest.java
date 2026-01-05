package cl.jonatansoto.reader.file.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DocumentoRequest(
        @JsonProperty("numeroOperacion") String numeroOperacion,
        @JsonProperty("nombreArchivo") String nombreArchivo,
        @JsonProperty("contenidoBase64") String contenidoBase64
) {}

