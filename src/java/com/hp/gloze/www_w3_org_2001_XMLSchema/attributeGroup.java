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
import com.hp.gloze.Gloze;
import com.hp.gloze.Restrictions;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/*! \page attributeGroup attributeGroup

Attribute groups allow the schema designer to collect together common groups of attributes.
The example below is based on the XML Linking standard \em XLink which defines groups of attributes
that define a number of link properties including xlink:href and xlink:type that we will see here.

\include link.xml

The xlink:href attribute is of type xs:anyURI, and this is resolved against the document base to provide
an absolute URI that retains its meaning in RDF. However, it remains a literal of type xs:anyURI.

\include link.n3

For brevity only the attributeGroup reference is included here. 
The simpleLink attribute group also defines a \em fixed xlink:type attribute that has an implied value
of 'simple' which is added to the RDF model even though it is not explicit in the XML.

\include link.xsd

\see http://www.w3.org/1999/xlink

\section annotationChildren Child components

- \ref attribute
- \ref attributeGroup
- \ref anyAttribute

*/

public class attributeGroup extends Content {

	private String id, name, ref;
	private attribute[] _attribute;
	private attributeGroup[] attributeGroup;
	private anyAttribute anyAttribute;

	public attributeGroup() throws IntrospectionException {
	}

	String createURI(Model model, Context ctx) {
		schema xs = (schema) this.get_owner();
		if (getName()!=null) return xs.expandName(getName(),model,ctx);
		else return expandQName(ctx.getDefaultNS(),null,getRef(),get_node(),model);
	}

	/** resolve references to simple and complex types, groups and attribute groups */
	
	public void resolve(Model model, Context ctx) {
		ctx.putRef(this,get_ref(ctx));
		super.resolve(model,ctx);
	}
	
	attributeGroup get_ref(Context ctx) {
		attributeGroup g = (attributeGroup) ctx.getRef(this);
		if (g!=null) return g;
		if (ref!=null) {
			String uri = expandQName(ctx.getDefaultNS(),ref,ctx.getModel());
			g = ctx.getAttributeGroup(uri);
			if (g!=null) ctx.putRef(this,g);
			else Gloze.logger.warn("no such attribute group: "+ref);
		}
		return g;
	}
	
	public void toRDF(Resource subject, Node node, Set<restriction> restrictions, Context ctx)
		throws Exception {
		schema xs = (schema) this.get_owner();
		// follow up ref if supplied
		if (ref!=null) {
			attributeGroup g = get_ref(ctx);
			if (g!=null) g.toRDF(subject,node,restrictions,ctx);
		}
		else {
			xs.toRDF(subject, node, _attribute, ctx);
		
			for (int i=0; attributeGroup!=null && i<attributeGroup.length; i++)
				attributeGroup[i].toRDF(subject,node,restrictions,ctx);
			
			if (restrictions!=null) return; // don't inherit beyond this point
			
			if (anyAttribute!=null) anyAttribute.toRDF(subject,node,restrictions,ctx);
		}
	}

	public void toXML(Element e, Resource rdf, Set<Statement> pending, Context ctx) {
		// follow up ref if supplied
		if (ref!=null) {
			attributeGroup g = get_ref(ctx);
			if (g!=null) g.toXML(e,rdf,pending, ctx);
		}
		else {
			for (int i = 0; _attribute != null && i < _attribute.length; i++)
				_attribute[i].toXML(e, rdf, pending, ctx);
			
			for (int i=0; attributeGroup!=null && i<attributeGroup.length; i++)
				attributeGroup[i].toXML(e,rdf,pending,ctx);
			
			if (anyAttribute!=null) anyAttribute.toXML(e,rdf,pending,ctx);
		}
	}
	
	public void toOWL(Restrictions rest, Context ctx) {
		schema xs = (schema) this.get_owner();
		attributeGroup g = get_ref(xs.ont,ctx);
		if (g!=null) g.toOWL(rest, ctx);

		for (int i = 0; _attribute != null && i < _attribute.length; i++)
			_attribute[i].toOWL(rest, ctx);
		
		for (int i=0; attributeGroup!=null && i<attributeGroup.length; i++)
			attributeGroup[i].toOWL(rest, ctx);
		
	}

	private attributeGroup get_ref(Model model, Context ctx) {
		attributeGroup g = (attributeGroup) ctx.getRef(this);
		if (g!=null) return g;
		if (ref!=null) {
			g = ctx.getAttributeGroup(createURI(model,ctx));
			if (g!=null) ctx.putRef(this,g);
			else Gloze.logger.warn("no such attribute group: "+ref);
		}
		return g;
	}

	public attribute[] getAttribute() {
		return _attribute;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setAttribute(attribute[] attributes) {
		_attribute = attributes;
	}

	public void setId(String string) {
		id = string;
	}

	public void setName(String string) {
		name = string;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String string) {
		ref = string;
	}

	public anyAttribute getAnyAttribute() {
		return anyAttribute;
	}

	public void setAnyAttribute(anyAttribute anyAttribute) {
		this.anyAttribute = anyAttribute;
	}

	public attributeGroup[] getAttributeGroup() {
		return attributeGroup;
	}

	public void setAttributeGroup(attributeGroup[] attributeGroup) {
		this.attributeGroup = attributeGroup;
	}

}
