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

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.hp.gloze.Context;
import com.hp.gloze.XMLBean;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;

/*! \page simpleType simpleType

	New simple types can be derived by restriction. OWL can define new sub-classes of datatypes but 
	is not able to define constraints on the new value space other than by enumeration. Simple types are
	therefore declared in OWL, but not defined to the same extent as in XML schema.
	
	\include simpleType1.xsd
	\include simpleType1.owl
	
	Gloze avoids using simple types in typed literals by instead using the datatype it is derived from.
	In the XML instance and below the lifted value 'bar' has type xs:string rather than the user-defined simple type 'mySimpleType'.
	
	\include simpleType1.xml
	\include simpleType1.n3
		
	\section simpleTypeChildren Child components
	
	- \ref restriction
	- \ref list
	- \ref union

 */

public class simpleType extends XMLBean {

	private String name;

	private restriction restriction;
	private list _list;
	private union union;

	/**
	 * @return Returns the union.
	 */
	public union getUnion() {
		return union;
	}

	/**
	 * @param union The union to set.
	 */
	public void setUnion(union union) {
		this.union = union;
	}

	public simpleType() throws IntrospectionException {
	}

	public String createURI(String qname, Model model, Context ctx) {
		return expandQName(ctx.getDefaultNS(),null,qname,get_node(),model);		
	}
	
	String createURI(Model model,Context ctx) {
		schema xs = (schema) this.get_owner();
		return xs.expandName(getName(),model,ctx);
	}

	/** return the ID if defined, may be attribute, simpleContent */
	
	public String getID(Element element, Context ctx) {
		if (restriction!=null) return restriction.getID(element,ctx);
		return null;
	}

	public boolean toRDF(Resource subject, Property prop, Node node, String value, Seq seq, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		schema xs = (schema) this.get_owner();
		String type = null;
		if (name!=null) type = createURI(name,subject.getModel(),ctx);
		
		if (_list!=null) 
			return _list.toRDF(subject, prop, seq, node, value, restrictions,ctx);
		else if (restriction != null) 
			return restriction.toRDF(subject, node, value, prop, seq, restrictions,ctx);
		else if (union!=null)
			return union.toRDF(subject,prop,node,value,seq, restrictions,ctx);

		if (value !=null && ctx.isPreserved()) value = value.trim();	
		return xs.toRDF(node,subject,prop,value,type,seq,restrictions,ctx);
	}

	public RDFList toRDFList(Node node, String value, RDFList list, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		schema xs = (schema) this.get_owner();
		String type = null;
		if (name!=null) type = createURI(name,ctx.getModel(),ctx);
		
		if (restriction != null) 
			return restriction.toRDFList(node, value, list, restrictions,ctx);
		else if (_list!=null)
			return _list.toRDFList(node,value, list, restrictions,ctx);
		else if (union!=null)
			return union.toRDFList(node,value, list, restrictions,ctx);
		
		else return schema.toRDFList(node,value,type,list,restrictions,ctx);
	}
	
	// simple type (element)

	public boolean toXML(Element e, RDFNode rdf, Context ctx) {
		return toXML(e,rdf,null,ctx);
	}
	
	public boolean toXML(Element e, RDFNode rdf, String pack, Context ctx) {
		try {
			schema xs = (schema) this.get_owner();
			if (_list!=null) return _list.toXML(e, rdf,ctx);
			else if (restriction!=null) return restriction.toXML(e,rdf,pack,ctx);
			else if (union!=null) return union.toXML(e,rdf,pack,ctx);
			else return xs.toXMLText(e,rdf,null,null,ctx);
		} catch (Exception ex) { // non-fatal
			return false;
		}
	}
		
	// simple type attribute

	public boolean toXML(Attr attr, RDFNode rdf, Context ctx) {
		try {
			schema xs = (schema) this.get_owner();
			if (_list!=null) return _list.toXML(attr, rdf,ctx);
			else if (restriction!=null) return restriction.toXML(attr,rdf,ctx);
			else if (union!=null) return union.toXML(attr,rdf,ctx);
			else return xs.toXML(attr,rdf,null,ctx);
		} catch (Exception e) { // non-fatal
			return false;
		}
	}

	public boolean isValid(String value, Context ctx) {
		if (restriction!=null) return restriction.isValid(value, ctx);
		return true;
	}
	
	/* return a class resource to represent a simple type
	 * embedded simple types may be anonymous
	 */
	
	public Resource toOWL(Context ctx) {
		schema xs = (schema) this.get_owner();
		String uri = createURI(xs.ont,ctx);
		
		// have we seen this type before
		Resource cls = ctx.getOntClass(uri) ;
		if (cls!=null) return cls;	

		// delegate creation of the class to the children
		if (restriction!=null) cls = restriction.toOWLSimpleType(uri, ctx);
		else if (_list!=null) cls = _list.toOWL(xs.ont, uri, true, ctx);
		else if (union!=null) cls = union.toOWL(xs.ont, uri, true, ctx);
		else cls = xs.ont.createClass(uri);

		if (cls!=null) ctx.putOntClass(uri, cls);
		return cls;
	}
	
	public void defineType(Property prop, Context ctx) {
		if (restriction!=null) restriction.defineType(prop,ctx);
		else if (_list!=null) _list.defineType(prop, ctx);
		else if (union!=null) union.defineType(prop,ctx);		
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public restriction getRestriction() {
		return restriction;
	}

	public void setRestriction(restriction restriction) {
		this.restriction = restriction;
	}

	public list getList() {
		return _list;
	}

	public void setList(list list) {
		this._list = list;
	}

}
