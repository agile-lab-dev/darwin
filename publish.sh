#!/bin/bash
export CI_RELEASE='+publishSigned;+darwin-hbase2-connector/publishSigned'
export CI_SNAPSHOT_RELEASE='+publish;+darwin-hbase2-connector/publish'
sbt -v ci-release
