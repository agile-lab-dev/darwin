#!/bin/bash
export CI_RELEASE='clean;scalastyle;+test;+publishSigned;darwin-hbase2-connector/clean;darwin-hbase2-connector/scalastyle;+darwin-hbase2-connector/test;+darwin-hbase2-connector/publishSigned'
export CI_SNAPSHOT_RELEASE='clean;scalastyle;+test;+publish;darwin-hbase2-connector/clean;darwin-hbase2-connector/scalastyle;+darwin-hbase2-connector/test;+darwin-hbase2-connector/publish'
sbt -v ci-release
