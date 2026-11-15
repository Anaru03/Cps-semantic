#!/bin/bash
set -euo pipefail
export PATH="${PATH}:/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:/usr/local/bin"
cd "$(dirname "$0")/.."
archivo=".cache/mars/Mars4_5.jar"
sha="ac340b676ba2b62246b9df77e62f81ad4447bcfd329ab539716bcd09950b7096"
mkdir -p .cache/mars
if [[ ! -f "$archivo" ]]; then
  curl --fail --location --retry 2 \
    https://github.com/dpetersanderson/MARS/releases/download/v.4.5.1/Mars4_5.jar \
    -o "$archivo.descarga"
  mv "$archivo.descarga" "$archivo"
fi
printf '%s  %s\n' "$sha" "$archivo" | shasum -a 256 -c - >&2
printf '%s/%s\n' "$PWD" "$archivo"
