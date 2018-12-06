#!/bin/bash
sbt clean scalastyle +publishLocal +test:compile +doc
#sbt "project darwin-hbase-connector" test && sbt "project darwin-postgres-connector" test && sbt "project
# darwin-mock-application" test