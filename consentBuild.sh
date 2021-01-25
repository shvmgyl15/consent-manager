#!/bin/bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
java -version
./gradlew consent:build -x test
