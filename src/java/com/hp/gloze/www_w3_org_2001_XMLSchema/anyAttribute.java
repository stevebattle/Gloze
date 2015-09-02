package com.hp.gloze.www_w3_org_2001_XMLSchema; 

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

import java.beans.IntrospectionException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hp.gloze.Content;
import com.hp.gloze.Context;
import com.hp.gloze.Gloze;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

/*! \page anyAttribute anyAttribute

Sometimes it is useful to create a slot that would match a range of attributes. One use of this is to allow
predefined xml attributes on an element, such as xml:lang; xml:id; xml:space without having to include the XML schema.
XML schema is not savvy to these new features, so they must be explicitly added to the schema.

\see http://www.w3.org/XML/1998/namespace

The example below includes two marked-up 'parts'. We use xml:id to add an identifier to 'foo', and xml:lang to define the language in which
'bar' is expressed.

\include anyAttribute.xml

The schema for the XML namespace, and hence the xml:id attribute, is not imported, so the interpretation of anyAttribute is non-strict.

\include anyAttribute.xsd

Note that even without the XML namespace schema to hand, Gloze treats xml:id and xml:lang correctly.

\include anyAttribute.n3

*/

/*! \page id xml:id
 Gloze interprets xml:id attributes as being of type xs:ID, regardless of whether or not a schema definition is available.
 The schema author must still design the schema so that the xml:id is a valid attribute. This may be done by adding it
 explicitly as a reference to the (imported) XML namespace schema; as xs:anyAttribute; or within xs:any.
 
 \ref anyAttribute "xml:id example"
 
 \see http://www.w3.org/XML/1998/namespace

 
 */

public class anyAttribute extends Content {

	private String namespace="##any", processContents="strict";

	public anyAttribute() throws IntrospectionException {
	}

	public void designate(String path) {
	}

	public void toRDF(Resource subject, Node node, Set<restriction> restrictions, Context ctx)
		throws Exception {
		// anyAttribute is not inherited by restrictions
		if (restrictions!=null) return;
		
		schema xs = (schema) this.get_owner();
		NamedNodeMap atts = node.getAttributes();
		for (int i=0; i<atts.getLength(); i++) {
			Attr attr = (Attr) atts.item(i);
			if (attr.getNodeName().startsWith("xmlns")) continue;
			String ns = attr.getNamespaceURI();
			StringTokenizer t = new StringTokenizer(namespace);
			while (t.hasMoreTokens()) {
				String n = t.nextToken();
				String tns = xs.getTargetNamespace();
				if (n.equals("##any") // anything at all
				|| (n.equals("##other") && (tns==null || !tns.equals(ns))) // other than the TNS
				|| (n.equals("##targetNamespace") && (tns==null || tns.equals(ns))) // in the target namespace
				|| (n.equals("##local") && ns==null) // local to this schema
				|| n.equals(ns) ) { // else the ns is anyURI
					// is this a known attribute?
					String uri = attribute.createURI(attr,null,ctx);
					attribute a = ctx.getAttribute(uri);
					if (a!=null) {
						a.toRDF(subject, attr, ctx);
						if (attr.getPrefix()!=null) 
							addPrefixes(attr.getPrefix(), attr.getNamespaceURI(), ctx.getModel());
					}
					else { // otherwise use the default no-schema mapping
						if (getProcessContents().equals("strict") 
								&& !n.equals("##any") && !n.equals("##other") && !n.equals("##local"))
							Gloze.logger.warn("cannot find attribute: "+attr.getName());
						noSchemaToRDF(subject,attr,ctx);
					}
				}
			}
		}
	}

	public void toXML(Element e, Resource subject, Set<Statement> pending, Context ctx) {
		Document doc = e.getOwnerDocument();
		schema xs = (schema) this.get_owner();
		Set<Statement> done = new HashSet<Statement>();
		boolean qualify = !(namespace.equals("##local") && xs.getTargetNamespace()==null);
		for (Statement s: pending) {
			Property p = s.getPredicate();
			// ignore e.g. iterated statements
			if (p.getURI().startsWith(RDF.getURI())) continue;
			
			// find an attribute that corresponds to this property
			attribute a = ctx.getAttribute(p.getURI());
			if (a!=null) a.toXML(e,s.getObject(),ctx);
			else if (!getProcessContents().equals("strict")) { 
				// construct a new attibute for the property
				String ns = p.getNameSpace(), nsAlt=null;
				if (ns.charAt(ns.length()-1)=='#') nsAlt = ns.substring(0,ns.length()-1);
				String pref = ctx.getModel().getNsURIPrefix(nsAlt);
				if (pref!=null) ns = nsAlt;
				else pref = ctx.getModel().getNsURIPrefix(ns);
				
				// check this new property is permitted by the namespace constraint
				if (namespace.contains("##any") ||
					    (namespace.contains("##other") && !ns.equals(xs.getTargetNamespace())) ||
						(namespace.contains("##local") || 
						namespace.contains(ns) || namespace.contains(nsAlt))) {
					
					Attr attr;
					if (qualify && !(ns.equals(ctx.getDefaultNS()) || ctx.getDefaultNS().equals(nsAlt))) {
						attr = doc.createAttributeNS(ns, p.getLocalName());
						attr.setPrefix(pref);	
					}
					else attr = doc.createAttribute(p.getLocalName());
					attr.setNodeValue(s.getString());
					if (qualify) e.setAttributeNodeNS(attr);
					else e.setAttributeNode(attr);
				}
				done.add(s);
			}
		}
		pending.removeAll(done);
		
		if (!subject.isAnon() && namespace.contains(XML) && !getProcessContents().equals("strict")) {
			e.setAttributeNS(XML,"id",subject.getLocalName());
		}
		else if (subject.hasProperty(RDF.value) && namespace.contains(XML) && !getProcessContents().equals("strict")) {
			// if there are multiple values we assume they are all the same language
			String lang = subject.getProperty(RDF.value).getLanguage();		
			if (lang!=null) e.setAttributeNS(XML,"lang",lang);
		}
	}

	public String getID(Attr a) {
		if (a==null) return null;
		return null;
	}

	public String getNamespace() {
		return namespace;
	}
	
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	public String getProcessContents() {
		return processContents;
	}
	
	public void setProcessContents(String processContents) {
		this.processContents = processContents;
	}

}
