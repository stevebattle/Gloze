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
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.hp.gloze.Context;
import com.hp.gloze.Gloze;
import com.hp.gloze.XMLBean;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import com.ibm.icu.util.StringTokenizer;

/*! \page list list
 An xs:list is mapped into an rdf:List. Lists assume whitespace separated content in the XML instance. 
 In the case of lists of xs:string, the string value is separated into separate string tokens.
 
 \include list.xml
 \include list.n3
 
 The mapping into OWL does not currently define lists for specific datatypes.
 
 \include list.xsd
 \include list.owl

 \section annotationChildren Child components

 - \ref annotation
 - \ref simpleType

*/
public class list extends XMLBean {

	private String id, itemType;
	private simpleType simpleType;

	public list() throws IntrospectionException {
	}

	public void resolve(Model model, Context ctx) {
		ctx.putType(this,get_type(ctx));
		super.resolve(model, ctx);
	}
	
	private simpleType get_type(Context ctx) {
		if (simpleType != null) return simpleType;
		simpleType s = (simpleType) ctx.getType(this);
		if (s!=null) return s;
		if (itemType!=null) {
			String uri = expandQName(ctx.getDefaultNS(),itemType,ctx.getModel());
			if (!uri.startsWith(schema.XSD_URI)) {
				s = ctx.getSimpleType(uri);
				if (s!=null) ctx.putType(this,s);
				else Gloze.logger.warn("no such type: "+itemType);
			}
		}
		return s;
	}
	
	// a schema list is modelled as an rdf:list of simple values
	
	public boolean toRDF(Resource subject, Property prop, Seq seq, Node node, String value, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		Model m = ctx.getModel();
		schema xs = (schema) this.get_owner();
		RDFList l= m.createList();

		simpleType t = get_type(ctx);
		String type = expandQName(ctx.getDefaultNS(),itemType, m);
		
		StringTokenizer tok = new StringTokenizer(value);
		while (tok.hasMoreTokens()) {
			String v = tok.nextToken();
			if (t!=null) l = t.toRDFList(node,v,l,null,ctx);
			else if ((l=schema.toRDFList(node,v,type,l,null,ctx))==null) return false;
		}		
		if (l!=null) {
			// are all restrictions observed
			if (restrictions!=null)
				for (restriction r: restrictions) 
					if (!r.isValid(value, null, l, ctx)) return false;

			Statement stmt = m.createStatement(subject, prop, l);
			m.add(stmt);
			if (seq != null) seq.add(stmt.createReifiedStatement());
			return true;
		}
		else return false;
	}
	
	// this is a degenerate case of a list within a list
	
	public RDFList toRDFList(Node node, String value, RDFList list, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		Gloze.logger.error("sublists not allowed");
		return null;
	}
	
	public boolean toXML(Element e, RDFNode rdf, Context ctx) {
		boolean ok =true;
		try {
			schema xsd = (schema) this.get_owner();
			simpleType t = get_type(ctx);
			String type = null;
			if (t==null) type = expandQName(ctx.getDefaultNS(),itemType, ctx.getModel());

			RDFList list = (RDFList) rdf.as(RDFList.class);
			String pack = null;
			for (ExtendedIterator i = list.iterator(); ok && i.hasNext(); ) {
				RDFNode n = (RDFNode) i.next();	
				if (t!=null) ok=t.toXML(e,n,pack,ctx);
				else ok=xsd.toXMLText(e,n,type,pack,ctx);
				pack = " "; // whitespace separator
			}
		} catch (Exception ex) { // non-fatal
			Gloze.logger.warn("failed to list value");
		}
		return ok;
	}

	public boolean toXML(Attr attr, RDFNode rdf, Context ctx) {
		boolean ok=true;
		Model m = ctx.getModel();
		try {
			schema xsd = (schema) this.get_owner();
			simpleType simple = ctx.getSimpleType(xsd.expandQName(ctx.getDefaultNS(),itemType,m));
			String type = null;
			if (simple==null) type = expandQName(ctx.getDefaultNS(),itemType, m);

			RDFList list = (RDFList) rdf.as(RDFList.class);
			for (ExtendedIterator i = list.iterator(); ok && i.hasNext(); ) {
				RDFNode n = (RDFNode) i.next();
				if (simple!=null) ok=simple.toXML(attr,n,ctx);
				else ok=xsd.toXML(attr,n,type,ctx);
			}
		} catch (Exception e) { // non-fatal
			Gloze.logger.warn("failed to list value");
		}
		return ok;
	}
	
	/** a list type */
	
	public Resource toOWL(OntModel ont, String uri, boolean createAnon, Context ctx) {
		OntClass cls = null;
		
		// try to establish the list member type (may be anonymous)
		String type = expandQName(ctx.getDefaultNS(),itemType, ont);
		
		// we don't like anonymous lists
		if (uri==null || (type!=null && type.startsWith(schema.XSD_URI) && !schema.isValidDatatype(type))) {
			// pass over anonymous simple types
			if (uri!=null) {
				cls = ont.createClass(uri);
				cls.addSuperClass(RDF.List);		
				return cls;
			} else return RDF.List;
		}
		
		// the type may be null for embedded simpleType
		Resource first = null;
		if (type==null) {
			simpleType t = get_type(ctx);
			first = t.toOWL(ctx);
		}
		else first = schema.toOWL(ont,type);
		
		if (uri!=null || createAnon) {
			cls = ont.createClass(uri);
			if (uri!=null) ctx.putOntClass(uri, cls);
			cls.addSuperClass(RDF.List);		
			if (first!=null) {
				// add the item type as range of rdf:first
				cls.addSuperClass(ont.createAllValuesFromRestriction(null,RDF.first,first));
				// add the class itself as the range of rdf:rest
				cls.addSuperClass(ont.createAllValuesFromRestriction(null,RDF.rest,cls));
			}
		}
		return cls;
	}
	
	public void defineType(Property prop, Context ctx) {
		prop.addProperty(RDF.type,OWL.ObjectProperty);
	}

	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id The id to set.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return Returns the itemType.
	 */
	public String getItemType() {
		return itemType;
	}

	/**
	 * @param itemType The itemType to set.
	 */
	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	/**
	 * @return Returns the simpleType.
	 */
	public simpleType getSimpleType() {
		return simpleType;
	}

	/**
	 * @param simpleType The simpleType to set.
	 */
	public void setSimpleType(simpleType simpleType) {
		this.simpleType = simpleType;
	}


}
