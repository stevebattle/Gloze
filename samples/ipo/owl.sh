#!/bin/bash

CP=$JAVA_HOME/jre/lib/rt.jar:../../lib/gloze.jar:$JENA_HOME/lib/xercesImpl.jar:$JENA_HOME/lib/jena.jar:$JENA_HOME/lib/commons-logging-1.1.jar:$JENA_HOME/lib/icu4j_3_4.jar:$JENA_HOME/lib/log4j-1.2.12.jar:$JENA_HOME/lib/iri.jar

java -cp $CP -Dgloze.target=. -Dgloze.verbose=true -Dgloze.base=http://example.org/ipo  -Dgloze.lang=N3 com.hp.gloze.Gloze ipo.xsd 
