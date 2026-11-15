#!/bin/bash
set -euo pipefail
export PATH="${PATH}:/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:/usr/local/bin"
cd "$(dirname "$0")/.."
if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home)"
  export JAVA_HOME
fi
if [[ -z "${MARS_JAR:-}" ]]; then
  MARS_JAR="$(bash scripts/preparar-mars.sh)"
fi
if [[ ! -f "$MARS_JAR" ]]; then
  printf 'No existe MARS_JAR: %s\n' "$MARS_JAR" >&2
  exit 1
fi
# Resolver antes de cambiar el directorio para los ejemplos.
MARS_JAR="$(cd "$(dirname "$MARS_JAR")" && pwd)/$(basename "$MARS_JAR")"
export MARS_JAR
# Separar los binarios de Maven de la compilación automática del IDE.
mvn -Dcompiscript.build.directory=target/mips-build clean test
mkdir -p target/mips
for fuente in examples/mips/{arreglos,objetos_herencia,excepciones,funciones_recursion,recursion_locales,mirilla_demo}.cps; do
  nombre="$(basename "$fuente" .cps)"
  salida="target/mips/$nombre.asm"
  mvn -q -Dcompiscript.build.directory=target/mips-build org.codehaus.mojo:exec-maven-plugin:3.2.0:java -Dexec.mainClass=mips.CompilarMIPS -Dexec.args="$fuente $salida"
  java -Djava.awt.headless=true -jar "$MARS_JAR" nc ae1 se2 1000000 "$salida" > "target/mips/$nombre.out"
  java -Djava.awt.headless=true -jar "$MARS_JAR" nc ae1 se2 1000000 "$salida.sin-optimizar.asm" > "target/mips/$nombre.sin-optimizar.out"
  diff -u "target/mips/$nombre.sin-optimizar.out" "target/mips/$nombre.out"
  # MARS añade una línea vacía al terminar; los archivos esperados contienen solo la salida del programa.
  sed '${/^$/d;}' "target/mips/$nombre.out" > "target/mips/$nombre.normalizada.out"
  diff -u "examples/mips/$nombre.expected" "target/mips/$nombre.normalizada.out"
done
printf 'Pruebas y equivalencia MIPS verificadas. Evidencia en target/mips/\n'
