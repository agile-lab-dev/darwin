#!/bin/bash
sbt clean scalastyle +test +publishSigned ++2.11.12 "project darwin-rest-server" clean scalastyle test publishSigned ++2.12.7 "project darwin-rest-server" test publishSigned
