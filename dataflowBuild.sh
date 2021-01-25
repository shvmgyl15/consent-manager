#!/bin/bash
ls
source "$HOME/.sdkman/bin/sdkman-init.sh"
java -version
./gradlew dataflow:build -x test
