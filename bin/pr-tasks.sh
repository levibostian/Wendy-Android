#!/bin/bash

set -e

./gradlew assembleDebug
./gradlew test
