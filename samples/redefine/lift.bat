set CP=%JAVA_HOME%\jre\lib\rt.jar;../../lib/gloze.jar
set CP=%CP%;%JENA_HOME%\lib\xercesImpl.jar;%JENA_HOME%\lib\jena.jar;%JENA_HOME%\lib\commons-logging-1.1.1.jar;%JENA_HOME%\lib\icu4j_3_4.jar;%JENA_HOME%\lib\log4j-1.2.12.jar;%JENA_HOME%\lib\iri.jar

java -cp %CP% -Dgloze.target=. -Dgloze.base=http://example.org/redefine -Dgloze.verbose=true -Dgloze.roundtrip=true com.hp.gloze.Gloze ipo.xml http://www.example.com/IPO ipo.xsd
