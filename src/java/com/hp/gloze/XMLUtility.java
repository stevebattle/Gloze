/*
 *  (c) Copyright Hewlett-Packard Company 2001 - 2009
 *  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @author steven.a.battle@googlemail.com
 */

package com.hp.gloze;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLUtility {
	
	private static final boolean INDENTING = true;
	// <!ENTITY eg "http://example.com/">
	static Pattern entityPattern = Pattern.compile("<!ENTITY\\s+(\\w+)\\s+\"(.*)\">");

	public static DocumentBuilderFactory factory = DocumentBuilderFactory
			.newInstance();

	public static Document newDocument() throws ParserConfigurationException {
		factory.setNamespaceAware(true);
		factory.setExpandEntityReferences(true);
		return factory.newDocumentBuilder().newDocument();
	}

	public static Document read(InputStream source) throws SAXException, IOException, ParserConfigurationException {
		if (source==null) return null;
		factory.setNamespaceAware(true);
		factory.setExpandEntityReferences(true);
		return factory.newDocumentBuilder().parse(new InputSource(source));
	}

	public static Document read(Reader source) throws SAXException,
			IOException, ParserConfigurationException {
		factory.setNamespaceAware(true);
		factory.setExpandEntityReferences(true);
		return factory.newDocumentBuilder().parse(new InputSource(source));
	}
	
	public static void write(Document doc, Writer writer) throws IOException {
		OutputFormat format = new OutputFormat();
		format.setIndenting(INDENTING);
		XMLSerializer serializer = new XMLSerializer(format);
		serializer.setNamespaces(true);
		serializer.setOutputCharStream(writer);
		serializer.serialize(doc);
	}

	public static void write(Element elem, Writer writer) throws IOException {
		OutputFormat format = new OutputFormat();
		format.setIndenting(INDENTING);
		XMLSerializer serializer = new XMLSerializer(format);
		serializer.setNamespaces(true);
		serializer.setOutputCharStream(writer);
		serializer.serialize(elem);
	}
		
	public static String getValue(Element element) {
		NodeList l = element.getChildNodes();
		StringBuffer s = new StringBuffer();
		for (int i = 0; i < l.getLength(); i++) {
			switch (l.item(i).getNodeType()) {
			case Node.TEXT_NODE:
			case Node.CDATA_SECTION_NODE:
				s.append(l.item(i).getNodeValue());
			}
		}
		return s.toString();
	}

}
