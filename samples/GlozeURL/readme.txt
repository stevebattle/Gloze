GlozeURL is an extension written by Jean-Marc Vanel to process input URL's instead of Files. 
It was written specifically for use with EulerGUI.

There's a "short" list of important XML Schema that are automatically added to the Gloze argument, 
depending on XML namespaces present in input file.

The "short" list of important XML Schema is here in class XMLExport :
http://eulergui.svn.sourceforge.net/viewvc/eulergui/trunk/eulergui/src/main/java/eulergui/export/XMLExport.java

The automatic addition to the Gloze argument, depending on XML namespaces present in input file, occurs here in class N3SourceFromXML_Gloze :
http://eulergui.svn.sourceforge.net/viewvc/eulergui/trunk/eulergui/src/main/java/eulergui/inputs/N3SourceFromXML_Gloze.java
There is a small SAX ContentHandler that just records the XML namespaces present in input file.

It may be tested with this document, both as an URL and as a local file:
http://www.agencexml.com/xsltforms/address.xml

with the ouput :

Start GlozeURL.main() with args:
http://www.agencexml.com/xsltforms/address.xml
http://www.w3.org/2002/xforms
http://www.w3.org/MarkUp/Forms/2007/XForms-11-Schema.xsd
