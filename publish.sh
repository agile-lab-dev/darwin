#!/bin/bash
sbt -mem 2048 clean scalastyle +publishSigned ++2.11.12 "project darwin-rest-server" publishSigned ++2.12.7 "project darwin-rest-server" publishSigned
