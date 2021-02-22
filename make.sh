#!/bin/bash
sbt -v clean scalastyle +test +doc darwin-hbase2-connector/clean darwin-hbase2-connector/scalastyle +darwin-hbase2-connector/test +darwin-hbase2-connector/doc
