#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCS_DIR="$(dirname "$SCRIPT_DIR")"

mkdir -p "$DOCS_DIR/releases"

cd "$DOCS_DIR/requirements"

pandoc [0-9]*.md \
  --from markdown+fenced_divs+raw_html \
  --pdf-engine=weasyprint \
  --css="$SCRIPT_DIR/pdf-style.css" \
  --resource-path="$DOCS_DIR/requirements:$DOCS_DIR/requirements/assets" \
  --metadata title="Requirements Document — Iteration 2" \
  -o "$DOCS_DIR/releases/requirements-iteration2.pdf"