#!/bin/bash
set -ex
sbt clean scalastyle +test +publishSigned
sbt darwin-hbase2-connector/clean darwin-hbase2-connector/scalastyle +darwin-hbase2-connector/test +darwin-hbase2-connector/publishSigned
