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
 *
 * @author steven.a.battle@googlemail.com
 */

package com.hp.gloze.www_w3_org_2001_XMLSchema;


import java.beans.IntrospectionException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.gloze.Context;
import com.hp.gloze.Gloze;
import com.hp.gloze.Restrictions;
import com.hp.gloze.XMLBean;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

/*! \page any any
 The use of xs:any makes it necessary to map content for which we have no schema.
 
 The example below includes two marked-up 'parts', and the same content represented as an HTML table.
 \see http://www.w3.org/MarkUp/Group/
 
 \include any.xml
 
 The HTML content is here represented by an xs:any element from the 'xhtml' namespace, which may include attributes and
 arbitrarily nested elements.
 
 \include any.xsd
 
 Not having the schema to hand, Gloze regards any content as ambiguously ordered so adds all
 content to an rdf:Seq. This helps to distinguish attributes from elements; any properties not
 added to this sequence are therefore attributes. Elements with a single value (and no attributes) are added as
 literal properties (see 'tr').
 
 The example also includes an xml:id defined to be of type xs:ID, regardless of the schema.
 This identifies the html resource, and is translated as a fragment identifier relative to the document base.
 
 \include any.n3
 
 If a schema is available for the wild-card element, then it may be referenced from within the XML instance document using
 an xsi:schemaLocation or xsi:noNamespaceSchemaLocation attribute. This can appear in the document element or on the
 wild-card element itself.

*/

public class any extends XMLBean {
	
	private String id, namespace = "##any", minOccurs = "1", maxOccurs ="1", processContents = "strict";	

	public any() throws IntrospectionException {
	}

	public boolean needSeq(Set names, Context ctx) {
		// it's too easy to create non-deterministic content models with wild-cards
		return true;
	}

	public boolean toRDF(Resource subject, Element element, Seq seq, Context ctx)
		throws Exception {
		schema xs = (schema) this.get_owner();
		String ns = element.getNamespaceURI();
		StringTokenizer t = new StringTokenizer(namespace);
		while (t.hasMoreTokens()) {
			String n = t.nextToken();
			String tns = xs.getTargetNamespace();
			if (n.equals("##any") // anything
			|| (n.equals("##other") && (tns==null || !tns.equals(ns))) // other than the TNS
			|| (n.equals("##targetNamespace") && (tns==null || tns.equals(ns))) // in the TNS
			|| (n.equals("##local") && ns==null) // unqualified element (no namespace)
			|| n.equals(ns) ) { // else n is anyURI
				// is this a known element?
				element e = ctx.getElement(expandQName(element, ctx.getModel(), ctx));
				if (e!=null) return e.toRDF(subject, element, seq,ctx);
				
				// any schema information on this element?
				schema s = Gloze.initSchemaXSI(element,xs.get_location(),ctx.getDefaultNS(),ctx.getSchemaMap());
				if (s!=null) {
					// yes there is - gather global info
					Set<URL> done = new HashSet<URL>();
					s.gatherGlobals(ctx.getModel(),ctx,done);
					e = ctx.getElement(expandQName(element, ctx.getModel(), ctx));
					if (e!=null) return e.toRDF(subject, element, seq,ctx);
				}
			
				// otherwise use the default no-schema mapping
				if (ctx.isVerbose())
				Gloze.logger.info("using no-schema mapping for element: "+element.getNodeName());
				return noSchemaToRDF(subject, element,seq,ctx);
			}
		}
		return false;
	}
		
	public int toRDF(Resource subject, Node node, int index, Seq seq, boolean mixed, Context ctx) 
	throws Exception {
		// the element may be defined locally or globally
		NodeList l = node.getChildNodes();		
		// index children of this XML node
		// occurs counts occurrences of this XSD element
		int max = maxOccurs.equals("unbounded")? Integer.MAX_VALUE : Integer.parseInt(maxOccurs); 		
		for (int occurs=0; index < l.getLength() && occurs<max; index++) {
			Node n = l.item(index);
			switch (n.getNodeType()) {
			// mixed content
			case Node.TEXT_NODE:
				if (mixed) schema.textToRDF(subject, seq, n, ctx);
			case Node.COMMENT_NODE:
				break;
			case Node.ELEMENT_NODE:
				// map a single child element
				if (toRDF(subject, (Element) n, seq, ctx)) occurs++;
				else return index;
				break;
			}
		}
		return index;
	}
	
	/* drop XML element sequencing and mixing */

	public int toXML(Element element, Resource rdf, int index, Set<Statement> pending, Context ctx) {
		Document doc = element.getOwnerDocument();
		Seq seq = rdf.getModel().getSeq(rdf);
		Statement stmt = null;
		boolean qualify = !namespace.equals("##local");

		int max = maxOccurs.equals("unbounded")? Integer.MAX_VALUE : Integer.parseInt(maxOccurs); 	
		int occurs = 0;
		for (; occurs<max ; occurs++, index++) {
			// consume mixed values
			index = produceMixed(seq,index,element);
			if (index<seq.size()) {
				stmt = (Statement) asStatement((Resource) seq.getObject(index+1));
				if (!toXML(element, stmt.getPredicate(), stmt.getObject(), qualify, ctx)) break;
			} else break;
		}
		// do any pending properties match?
		Set<Statement> done = new HashSet<Statement>();
		for (Iterator ui = pending.iterator(); occurs<max && ui.hasNext(); ) {
			Statement s = (Statement) ui.next();
//			element.appendChild(doc.createTextNode(s.getString()));
			if (toXML(element, s.getPredicate(), s.getObject(), qualify, ctx)) done.add(s);
		}
		pending.removeAll(done);
		return index;
	}
	
	public boolean toXML(Element elem, Property property, RDFNode rdf, boolean qualify,Context ctx) {
		element el = ctx.getElement(property.getURI());
		if (el!=null && el.toXML(elem,rdf,ctx)) return true;
				
		if (getProcessContents().equals("strict") && !namespace.equals("##local"))
			Gloze.logger.warn("cannot find element: "+property.getLocalName());

		if (!property.getNameSpace().equals(RDF.getURI())) {
			Element child = noSchemaToElement(elem,property,ctx);
			elem.appendChild(child);
			return noSchemaToXML(child, rdf,qualify,ctx);
		}
		return false;
	}

	public void toOWL(Restrictions rest, int min, int max, Context ctx) {
		max = Restrictions.product(getMaxOccurs(),max);
		schema xs = (schema) this.get_owner();
		Set<String> added = new HashSet<String>();
		if (rest!=null) {
			StringTokenizer t = new StringTokenizer(namespace);
			while (t.hasMoreTokens()) {
				String ns = t.nextToken();
				// anything at all
				if (ns.equals("##any")) rest.addMaxAny(ns,max);
				// anything other than the target ns
				else if (ns.equals("##other")) rest.addMaxAny(ns,max);
				// anything in the target namespace
				else if (ns.equals("##targetNamespace")) rest.addMaxAny(xs.getTargetNamespace(),max);
				// unqualified element (no namespace)
				else if (ns.equals("##local")) rest.addMaxAny(ctx.getDefaultNS(),max);
				else if (!added.contains(ns)) {
					rest.addMaxAny(ns,max);
					added.add(ns);
				}
			}
		}
	}
	
	public String getId() {
		return id;
	}

	public String getMaxOccurs() {
		return maxOccurs;
	}

	public String getMinOccurs() {
		return minOccurs;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getProcessContents() {
		return processContents;
	}

	public void setId(String string) {
		id = string;
	}

	public void setMaxOccurs(String string) {
		maxOccurs = string;
	}

	public void setMinOccurs(String string) {
		minOccurs = string;
	}

	public void setNamespace(String string) {
		namespace = string;
	}

	public void setProcessContents(String string) {
		processContents = string;
	}

}
