#!/usr/bin/env bash
NETWORK_NAME="darwin-docker"
echo "Setting up docker network \"${NETWORK_NAME}\""
source get-docker-cmd.sh

# check if network exists
if [ $($DOCKER_CMD network ls | grep "${NETWORK_NAME}" | wc -l) -eq 0 ]; then
    # network not found, create
    echo "Docker network \"${NETWORK_NAME}\" does not exist; creating it..."
    ${DOCKER_CMD} network create ${NETWORK_NAME}
else
    # network found
    echo "Docker network \"${NETWORK_NAME}\" already exists"
fi