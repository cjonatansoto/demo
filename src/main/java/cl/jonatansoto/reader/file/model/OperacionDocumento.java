package cl.jonatansoto.reader.file.model;

public record OperacionDocumento(
        String nroOperacion,
        String nombreArchivo,
        byte[] contenido,
        String pathCompleto,
        String contenidoBase64
) {
    public OperacionDocumento(String nroOperacion, String nombreArchivo, byte[] contenido, String pathCompleto) {
        this(nroOperacion, nombreArchivo, contenido, pathCompleto, null);
    }
}
