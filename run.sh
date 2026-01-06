#!/bin/bash
# Script para ejecutar la aplicación Spring Boot con Java 17

# Buscar Java 17
JAVA_17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)

if [ -z "$JAVA_17_HOME" ]; then
    echo "ERROR: Java 17 no encontrado. Por favor instala Java 17."
    exit 1
fi

# Establecer JAVA_HOME a Java 17
export JAVA_HOME="$JAVA_17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo "=========================================="
echo "Ejecutando aplicación con Java 17"
echo "JAVA_HOME: $JAVA_HOME"
echo "Versión: $($JAVA_HOME/bin/java -version 2>&1 | head -1)"
echo "=========================================="
echo ""

# Ejecutar Spring Boot
mvn spring-boot:run

