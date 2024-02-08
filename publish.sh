#!/bin/bash
export CI_RELEASE='+publishSigned;+darwin-hbase2-connector/publishSigned'
export CI_SNAPSHOT_RELEASE='+publishSigned;+darwin-hbase2-connector/publishSigned'
sbt -v ci-release
gpg --list-keys
