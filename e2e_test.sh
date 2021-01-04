#!/usr/bin/env bash
DIR=$(mktemp -d)
cd "$DIR"
ls -la

printf "\n\n$1" | cfn init -t AWS::Foo::Bar -a RESOURCE -vv
ls -la
mvn verify
