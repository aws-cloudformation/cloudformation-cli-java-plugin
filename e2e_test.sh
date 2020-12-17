#!/usr/bin/env bash
DIR=$(mktemp -d)
cd "$DIR"
ls -la
cfn init -t AWS::Foo::Bar -a RESOURCE
printf "\n\n$1" | cfn init -vv
ls -la
mvn verify
ls -la