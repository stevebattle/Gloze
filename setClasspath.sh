#!/bin/bash

DIR=`dirname "$0"`
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
for f in $DIR/lib/*.jar $DIR/lib-tmp/*.jar $DIR/build/*.jar
do
  LIBS=$LIBS:$f
done

