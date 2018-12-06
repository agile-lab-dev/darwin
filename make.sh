#!/bin/bash
sbt -mem 2048 scalastyle && \
sbt -mem 2048 ++2.10.7 clean test && \
sbt -mem 2048 ++2.11.12 clean test && \
sbt -mem 2048 ++2.12.7 clean test && \
sbt -mem 2048 +doc