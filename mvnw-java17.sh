#!/bin/bash
# Script wrapper para asegurar que Maven use Java 17

# Buscar Java 17
JAVA_17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)

if [ -z "$JAVA_17_HOME" ]; then
    echo "ERROR: Java 17 no encontrado. Por favor instala Java 17."
    exit 1
fi

# Establecer JAVA_HOME a Java 17
export JAVA_HOME="$JAVA_17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Usando Java 17: $JAVA_HOME"
echo "Versión: $($JAVA_HOME/bin/java -version 2>&1 | head -1)"
echo ""

# Ejecutar Maven con los argumentos pasados
mvn "$@"

