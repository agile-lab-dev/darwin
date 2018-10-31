#!/bin/bash
sbt clean scalastyle +compile +test:compile +doc
