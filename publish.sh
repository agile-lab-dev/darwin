#!/bin/bash
set -ex
if [ ! $TRAVIS ]; then
  sbtn shutdown
fi
sbtn "clean; scalastyle; +test; +publishSigned"
sbtn "darwin-hbase2-connector/clean; darwin-hbase2-connector/scalastyle; +darwin-hbase2-connector/test; +darwin-hbase2-connector/publishSigned"
