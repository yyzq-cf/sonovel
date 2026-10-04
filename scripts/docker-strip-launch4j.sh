#!/bin/sh
# Strip the launch4j <plugin>...</plugin> block from pom.xml.
# launch4j builds a Windows .exe via the amd64-only windres binary;
# it breaks arm64 QEMU Docker builds and Docker doesn't need the .exe anyway.
set -e

LAUNCH_LINE=$(grep -n 'launch4j' pom.xml | head -1 | cut -d: -f1)
if [ -z "$LAUNCH_LINE" ]; then
  echo "launch4j not found, nothing to strip" >&2
  exit 0
fi

START=$(head -n "$LAUNCH_LINE" pom.xml | grep -n '<plugin>' | tail -1 | cut -d: -f1)
END=$(tail -n +"$LAUNCH_LINE" pom.xml | grep -n '</plugin>' | head -1 | cut -d: -f1)
END=$((LAUNCH_LINE + END - 1))

echo "Stripping launch4j plugin: lines $START-$END" >&2
sed -i "${START},${END}d" pom.xml
echo "Remaining launch4j refs: $(grep -c launch4j pom.xml 2>/dev/null || echo 0)" >&2
