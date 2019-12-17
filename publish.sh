#!/bin/bash
sbt clean scalastyle +test +publishSigned
