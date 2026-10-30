#!/usr/bin/env bash
set -e

if [ -f .env ]; then
  # Automatically export all variables defined in .env
  set -a
  # Filter out blank lines and comment lines before sourcing
  source <(grep -v '^\s*#' .env | grep -v '^\s*$')
  set +a
else
  echo "Warning: .env file not found. Running with system environment."
fi

./mvnw spring-boot:run