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

package com.hp.gloze.www_w3_org_2001_XMLSchema;

import java.beans.IntrospectionException;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.hp.gloze.Content;
import com.hp.gloze.Context;
import com.hp.gloze.Restrictions;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

/*! \page simpleContent simpleContent
 Simple content lets us add attributes to an element that otherwise has content representing a single value.
 
 \include simpleContent.xml
 \include simpleContent.xsd
 
 Note how the content and named properties derived from attributes share the same subject. The content is
 differentiated from attributes by the use of the rdf:value property.
 
 \include simpleContent.n3
 
 \section simpleContentChildren Child components

 - \ref annotation
 - \ref restriction
 - \ref extension

 */

public class simpleContent extends Content {
	
	private String id;
	private restriction restriction;
	private extension extension;
	
	public simpleContent() throws IntrospectionException {
	}

	// simple content only appears as a direct component of complexType so is not sequenced
	public boolean toRDF(Resource subject, Node node, Context ctx)
		throws Exception {
		Model model = subject.getModel();
		Element e = (Element) node;
		if (restriction != null)
			return restriction.toRDF(subject, node, getValue(e), RDF.value, null, null, ctx);
		else if (extension != null) 
			return extension.toRDF(subject, e, RDF.value, null, ctx);
		else {// otherwise just add plain literal
			Literal l = model.createLiteral(schema.processWhitespace(node,getValue(e),null,ctx));
			subject.addProperty(RDF.value, l);
			return true;
		}			
	}

	public boolean toRDF(Resource subject, Property prop, Node node, String value, Seq seq, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		schema xs = (schema) this.get_owner();
		if (restriction != null) 
			return restriction.toRDF(subject, node, value, prop, seq, restrictions,ctx);
		else if (extension != null) 
			return extension.toRDF(subject, (Element)node, prop, restrictions, ctx);

		if (value !=null && ctx.isPreserved()) value = value.trim();	
		return xs.toRDF(node,subject,prop,value,null,seq,restrictions,ctx);
	}

	public boolean toXML(Element e, RDFNode rdf, Set<Statement> pending, Context ctx) {
		if (rdf instanceof Resource) {
			Resource r = ((Resource)rdf);
			// add simple content encoded as rdf:value
			Statement s = r.getProperty(RDF.value);
			if (extension!=null) extension.toXML(e,r,pending,ctx);
			else if (restriction!=null) restriction.toXML(e,r,pending,ctx);
			else if (s!=null && s.getObject() instanceof Literal) {
				Literal l = (Literal) s.getObject();
				e.appendChild(e.getOwnerDocument().createTextNode(l.getString()));
			}
			return true;
		}
		return false;
	}

	public void toOWL(OntModel ont, Resource cls, Restrictions rest, Context ctx) {
		// extension/restriction in the context of a complex type
		if (extension!=null) extension.toOWLSimpleContent(cls, rest, ctx);
		else if (restriction!=null) restriction.toOWLSimpleContent(cls, rest, ctx);
	}

	public String getID(Element element, Context ctx) {
		if (extension!=null) return extension.getID(element,ctx);
		if (restriction!=null) return restriction.getID(element,ctx);
		return null;
	}

	public extension getExtension() {
		return extension;
	}

	public String getId() {
		return id;
	}

	public restriction getRestriction() {
		return restriction;
	}

	public void setExtension(extension extension) {
		this.extension = extension;
	}

	public void setId(String string) {
		id = string;
	}

	public void setRestriction(restriction restriction) {
		this.restriction = restriction;
	}

}
