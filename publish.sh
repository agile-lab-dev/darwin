#!/bin/bash
sbt clean scalastyle +publishSigned ++2.11.12 "project darwin-rest-server" clean publishSigned ++2.12.7 "project darwin-rest-server" publishSigned
