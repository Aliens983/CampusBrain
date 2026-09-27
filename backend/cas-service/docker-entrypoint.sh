#!/bin/sh
set -eu

# Seed bundled carousel assets into the persistent upload volume without overwriting uploads.
find /app/default-uploads -type f | while IFS= read -r source; do
  relative=${source#/app/default-uploads/}
  target="/app/uploads/$relative"
  mkdir -p "$(dirname "$target")"
  if [ ! -e "$target" ]; then
    cp "$source" "$target"
  fi
done
exec java -Xms256m -Xmx512m -jar /app/app.jar
