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

SBT_IMAGE="hseeberger/scala-sbt:8u181_2.12.7_1.2.6"
${DOCKER_CMD} volume create sbt-vol

PG_CONTAINER=$(docker ps | grep postgres-darwin | tr -d '[:space:]')
HB_CONTAINER=$(docker ps | grep hbase-darwin | tr -d '[:space:]')

if [ -z "$PG_CONTAINER" ]; then
  echo "Spawning POSTGRES container postgres-darwin"
  SPAWNED_PG_CONTAINER=$(${DOCKER_CMD} run --network=${NETWORK_NAME} --rm -d --name postgres-darwin postgres:9.3.25-alpine)
else
  echo "Not spawning POSTGRES container because it was already up with name postgres-darwin"
fi

if [ -z "$HB_CONTAINER" ]; then
  echo "Spawning HBASE container hbase-darwin"
  SPAWNED_HB_CONTAINER=$(${DOCKER_CMD} run --network=${NETWORK_NAME} --rm -d --name hbase-darwin hbase_cdh:5.12)
else
  echo "Not spawning HBASE container because it was already up with name hbase-darwin"
fi

DOCKER_OPTS="-i -v $SCRIPT_DIR/../../:/darwin/:rw -v sbt-vol:/root/ --network=${NETWORK_NAME} --rm -w /darwin/"

${DOCKER_CMD} run ${DOCKER_OPTS} --name sbt \
 ${SBT_IMAGE} ./make.sh 2>&1

if [[ ! -z "$SPAWNED_PG_CONTAINER" ]]; then
  echo "Stopping POSTGRES container postgres-darwin"
  ${DOCKER_CMD} stop postgres-darwin &
fi

if [[ ! -z "$SPAWNED_HB_CONTAINER" ]]; then
  echo "Stopping HBASE container hbase-darwin"
  ${DOCKER_CMD} stop hbase-darwin
fi
