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
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
  * @author steven.a.battle@googlemail.com
  */

package com.hp.gloze;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author stebat
 *
 * defines an XML content model
 * content models are implemented as state machines
 * 
 */
public interface ContentIFace {

	class ContentNodeList implements NodeList {
		List<Node> list = new Vector<Node>();
		public ContentNodeList() {}
		public ContentNodeList(NodeList l) {
			for (int i=0; i<l.getLength(); i++)
				add(l.item(i));
		}
		public int getLength() {
			return list.size();
		}
		public Node item(int index) {
			return (Node) list.get(index);
		}
		public void add(Node node) {
			list.add(node);
		}
		public Node remove(int index) {
			return (Node) list.remove(index);
		}
	}

	class ContentNamedNodeMap implements NamedNodeMap {
		Map<String,Node> map = new HashMap<String,Node>();
		List<String> index = new Vector<String>();
		public Node getNamedItem(String name) {
			return (Node) map.get(name);
		}
		public Node setNamedItem(Node arg) throws DOMException {
			String name = arg.getNodeName();
			Node result = (Node) map.get(name);
			map.put(name, arg);
			if (result == null) index.add(name);
			return result;
		}
		public Node removeNamedItem(String name) throws DOMException {
			index.remove(name);
			return (Node) map.remove(name);
		}
		public Node item(int i) {
			return (Node) map.get(index.get(i));
		}
		public int getLength() {
			return map.size();
		}
		public Node getNamedItemNS(String namespaceURI, String localName) {
			return (Node) map.get(
				(namespaceURI == null ? "" : namespaceURI) + localName);
		}
		public Node setNamedItemNS(Node arg) throws DOMException {
			String ns = arg.getNamespaceURI();
			String fullName = (ns == null ? "" : ns) + arg.getLocalName();
			Node result = (Node) map.get(fullName);
			map.put(fullName, arg);
			if (result == null)
				index.add(fullName);
			return result;
		}
		public Node removeNamedItemNS(String namespaceURI, String localName)
			throws DOMException {
			String name =
				(namespaceURI == null ? "" : namespaceURI) + localName;
			index.remove(name);
			return (Node) map.remove(name);
		}
	}
	
	public boolean addAttributes(XMLBean bean, NamedNodeMap map) throws Exception;
	
	public boolean addChildNodes(XMLBean bean, NodeList list) throws Exception;

	public NamedNodeMap getAttributes(XMLBean bean, Document doc) throws Exception;

	public NodeList getChildNodes(XMLBean bean, Document doc) throws Exception;

	public ContentIFace addNode(XMLBean bean, Node node) throws Exception;
	
	public boolean addAttribute(XMLBean bean, Attr attribute) throws Exception;

	public ContentIFace addElement(XMLBean bean, Element element) throws Exception;

	public ContentIFace addText(XMLBean bean, CharacterData data) throws Exception;

	public ContentIFace addCData(XMLBean bean, CharacterData data) throws Exception;
	
	public boolean stop();
	
	public String needs();

}
