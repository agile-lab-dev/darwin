#!/bin/bash
set -ex
COMMAND=sbtn
if ! command -v $COMMAND &> /dev/null
then
    echo "$COMMAND could not be found"
    echo "using sbt"
    COMMAND="sbt"
else
  if [ ! $TRAVIS ]; then
    sbtn shutdown
  fi
fi
$COMMAND "clean; scalastyle; +test; +publishSigned"
$COMMAND "darwin-hbase2-connector/clean; darwin-hbase2-connector/scalastyle; +darwin-hbase2-connector/test; +darwin-hbase2-connector/publishSigned"
