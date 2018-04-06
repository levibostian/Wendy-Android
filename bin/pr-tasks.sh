#!/bin/bash

set -e

./gradlew dokka
./gradlew assembleDebug
./gradlew test
