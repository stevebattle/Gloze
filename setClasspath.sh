#!/bin/bash

for f in lib/*.jar lib-tmp/*.jar build/*.jar
do
  LIBS=$LIBS:$f
done

