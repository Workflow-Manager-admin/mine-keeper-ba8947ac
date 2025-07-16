#!/bin/bash
cd /home/kavia/workspace/code-generation/mine-keeper-ba8947ac/mine_keeper_frontend
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

