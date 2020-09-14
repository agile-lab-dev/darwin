#!/bin/bash
set -ex
sbt clean scalastyle +test +doc
sbt darwin-hbase2-connector/clean darwin-hbase2-connector/scalastyle +darwin-hbase2-connector/test +darwin-hbase2-connector/doc