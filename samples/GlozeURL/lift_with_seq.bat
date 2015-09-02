set CP=%JAVA_HOME%\jre\lib\rt.jar;..\..\lib\gloze.jar
set CP=%CP%;%JENA_HOME%\lib\xercesImpl.jar;%JENA_HOME%\lib\jena.jar;%JENA_HOME%\lib\commons-logging-1.1.1.jar;%JENA_HOME%\lib\icu4j_3_4.jar;%JENA_HOME%\lib\log4j-1.2.12.jar;%JENA_HOME%\lib\iri.jar

java -cp %CP% -Dgloze.order=seq com.hp.gloze.GlozeURL http://www.agencexml.com/xsltforms/address.xml http://www.w3.org/2002/xforms http://www.w3.org/MarkUp/Forms/2007/XForms-11-Schema.xsd