#!/bin/bash

set -e

./gradlew clean build test
mkdir -p bin
cp `ls build/libs/nupow-miner-*-all.jar` bin/nupow-miner.jar
cp -n config-example.yml bin/config.yml
cp -n logback.xml bin/logback.xml
java -cp nupow-miner.jar -Dlogback.configurationFile=logback.xml fi.nupow.Miner config.yml
