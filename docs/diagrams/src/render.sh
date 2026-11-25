#!/bin/bash
# usage: render.sh file.svg ...   (svg files in D:/A_Craeted/faiza/docs/diagrams/uml)
CH="/c/Program Files/Google/Chrome/Application/chrome.exe"
for f in "$@"; do
  b=$(basename "$f" .svg)
  W=$(grep -o 'width="[0-9]*"' "$f" | head -1 | tr -dc 0-9); H=$(grep -o 'height="[0-9]*"' "$f" | head -1 | tr -dc 0-9)
  out="$(cygpath -m "$(realpath "$(dirname "$f")")")/$b.png"
  "$CH" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --window-size=$W,$H --screenshot="$out" "file:///$(cygpath -m "$(realpath "$f")")" 2>&1 | tail -1
done
