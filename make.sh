#!/bin/bash
sbt clean scalastyle +publishLocal +test:compile +doc
