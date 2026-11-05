#!/usr/bin/env bash
set -e

if [ -f .env ]; then
  # Automatically export all variables defined in .env
  set -a
  . ./.env # Now works on macOS and Linux and Windows (Git Bash)
  set +a
else
  echo "Warning: .env file not found. Running with system environment."
fi

./mvnw spring-boot:run