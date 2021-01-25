#!/bin/bash
ls
source "$HOME/.sdkman/bin/sdkman-init.sh"
java -version
./gradlew user:build -x test
