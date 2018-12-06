#!/usr/bin/env bash

# exit on any error
#set -e

# absolute path to this script. /home/user/bin/foo.sh
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
# this variable contains the directory of the script
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

cd ${SCRIPT_DIR}
source get-docker-cmd.sh
source create-test-network.sh

#r=$(tput setaf 1)
#g=$(tput setaf 2)
#y=$(tput setaf 3)
#d=$(tput sgr0)

IMAGE="hseeberger/scala-sbt"
${DOCKER_CMD} pull $IMAGE
${DOCKER_CMD} volume create sbt-vol

DOCKER_OPTS="-i -v $SCRIPT_DIR/../../:/darwin/:rw -v sbt-vol:/root/ --network=${NETWORK_NAME} --rm -w /darwin/"

${DOCKER_CMD} run --network=${NETWORK_NAME} \
 --rm -d --name hbase-darwin hbase_cdh:5.12
${DOCKER_CMD} run --network=${NETWORK_NAME} \
 --rm -d --name postgres-darwin postgres:9.3.25-alpine
 #| sed "s/.*/$y hbase |$d &/" &

${DOCKER_CMD} run ${DOCKER_OPTS} --name sbt \
 ${IMAGE} ./make.sh 2>&1

 # | sed "s/.*/$r sbt |$d &/"

${DOCKER_CMD} stop hbase-darwin
${DOCKER_CMD} stop postgres-darwin