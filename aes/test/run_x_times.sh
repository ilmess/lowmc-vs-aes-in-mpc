#!/bin/bash

TIMES=$1

for ((i=1; i<=TIMES; i++)); do
  echo ""
  echo "=============================="
  echo "RUN $i / $TIMES"
  echo "=============================="
  ./run.sh
done
