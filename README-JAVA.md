# Configuración de Java

## Problema Resuelto

El error `ExceptionInInitializerError` con `TypeTag :: UNKNOWN` se debía a un desajuste entre la versión de Java del sistema (Java 25) y la versión requerida por el proyecto (Java 17).

## Solución

El proyecto está configurado para usar **Java 17**. Se han agregado archivos de configuración para que Maven use automáticamente Java 17.

### Configuración del IDE

Si estás usando **IntelliJ IDEA** o **Eclipse**:

1. **IntelliJ IDEA:**
   - Ve a `File` → `Project Structure` → `Project`
   - Establece `Project SDK` a Java 17
   - Establece `Project language level` a 17
   - Ve a `File` → `Settings` → `Build, Execution, Deployment` → `Compiler` → `Java Compiler`
   - Establece `Project bytecode version` a 17

2. **Eclipse:**
   - Click derecho en el proyecto → `Properties`
   - `Java Build Path` → `Libraries` → Asegúrate de usar Java 17
   - `Java Compiler` → Establece `Compiler compliance level` a 17

3. **VS Code:**
   - Abre `.vscode/settings.json` y agrega:
   ```json
   {
     "java.configuration.runtimes": [
       {
         "name": "JavaSE-17",
         "path": "/ruta/a/java-17"
       }
     ],
     "java.compile.nullAnalysis.mode": "automatic"
   }
   ```

### Verificar Versión de Java

Para verificar qué versión de Java está usando Maven:

```bash
mvn -version
```

Debería mostrar Java 17. Si muestra otra versión, configura `JAVA_HOME`:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn clean compile
```

### Compilación

El proyecto ahora compila correctamente con:

```bash
mvn clean compile
```

O usando el script wrapper que asegura Java 17:

```bash
./mvnw-java17.sh clean compile
```

### Ejecutar la Aplicación

Para ejecutar la aplicación Spring Boot con Java 17 garantizado:

```bash
./run.sh
```

O manualmente estableciendo JAVA_HOME:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn spring-boot:run
```

### Scripts Disponibles

1. **`run.sh`** - Ejecuta la aplicación Spring Boot con Java 17
2. **`mvnw-java17.sh`** - Wrapper de Maven que fuerza el uso de Java 17
   ```bash
   ./mvnw-java17.sh clean install
   ./mvnw-java17.sh spring-boot:run
   ```

### Solución Rápida para el Error

Si sigues viendo el error `ExceptionInInitializerError` con `TypeTag :: UNKNOWN`:

1. **Desde terminal:**
   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 17)
   mvn clean compile
   ```

2. **Desde IDE (IntelliJ IDEA):**
   - `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven` → `Runner`
   - Marca "Use Project JDK" o establece "JRE" a Java 17

3. **Usar el script wrapper:**
   ```bash
   ./mvnw-java17.sh clean compile
   ```

## Notas

- El proyecto requiere **Java 17** como mínimo
- Lombok está configurado para procesar anotaciones correctamente
- Todos los archivos de configuración están en el `pom.xml`
- Se ha actualizado Lombok a la versión 1.18.34 para mejor compatibilidad
- Los scripts `run.sh` y `mvnw-java17.sh` garantizan el uso de Java 17

