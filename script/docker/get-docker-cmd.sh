#!/usr/bin/env bash

echo "Finding whether docker can be run without sudo"

# attempt to run an innocuous docker command
set +e
docker ps &> /dev/null
EXIT_CODE=$?
set -e

# check exit code; if 1, assume permission error
if [ ${EXIT_CODE} -eq 0 ]; then
    echo "Command \"docker ps\" succeeded, using \"docker\" as command"
    DOCKER_CMD="docker"
else
    echo "Command \"docker ps\" failed, using \"sudo docker\" as command"
    DOCKER_CMD="sudo docker"
fi