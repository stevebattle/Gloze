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
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.hp.gloze.Context;
import com.hp.gloze.Gloze;
import com.hp.gloze.XMLBean;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import com.ibm.icu.util.StringTokenizer;

/*! \page union union
 Union datatypes merge the lexical spaces of several existing types to create another.
 Because OWL does not currently support user-defined datatypes, 
 Gloze uses only the union member types to define datatyped literals.
 
 \include union.xml
 
 This XML conforms to the schema below. The content is either an xs:int or an xs:string.
 
 \include union.xsd
 
 The XML content is validated against both xs:int and xs:string to determine its type.
 In this case it is an xs:string.
 
 \include union.n3
 
 There's little to say about the range of the property. A simple type may sometimes map to an object type (e.g. QNames),
 so a union is not necessarily a datatype property. In this case, both member types map to datatype properties
 so 'union' is a datatype property.
 
 \include union.owl

 \section unionChildren Child components

 - \ref annotation
 - \ref simpleType

 */

public class union extends XMLBean {

	private String id, memberTypes;
	private simpleType[] simpleType;

	public class MemberType {
		private String type;
		private simpleType simple;
		public MemberType(String type) {
			this.type = type;
		}
		public MemberType(simpleType simple) {
			this.simple = simple;
		}
		public simpleType getSimpleType() {
			return simple;
		}
		public String getType() {
			return type;
		}
	}

	public union() throws IntrospectionException {
	}

	public void resolve(Model model, Context ctx) {
		ctx.putMemberTypes(this,get_memberTypes(ctx));
		super.resolve(model,ctx);
	}
	
	private Vector<MemberType> get_memberTypes(Context ctx) {
		Vector<MemberType> v = ctx.getMemberTypes(this);
		if (v!=null) return v;
		
		v = new Vector<MemberType>();
		if (memberTypes!=null) {
			StringTokenizer tok = new StringTokenizer(memberTypes);
			while (tok.hasMoreTokens()) {
				String type = tok.nextToken();
				String uri = expandQName(ctx.getDefaultNS(),type,ctx.getModel());
				// add a schema type string or resolved simpleType
				MemberType m;
				if (uri.startsWith(schema.XSD_URI)) m = new MemberType(uri);
				else m = new MemberType(ctx.getSimpleType(uri));
				v.add(m);
			}
		}
		// add simple types in the body
		for (int i=0; simpleType!=null && i<simpleType.length; i++)
			v.add( new MemberType(simpleType[i]));
		
		return v;
	}
	
	// a list is modelled as a sequence of simple values
	
	public boolean toRDF(Resource subject, Property prop, Node node, String value, Seq seq, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		schema xs = (schema) this.get_owner();
		if (value !=null && ctx.isPreserved()) value = value.trim();
		
		// get (resolved) member types
		Vector<MemberType> v = get_memberTypes(ctx);
		for (int i=0; i<v.size(); i++) {
			// isolate restrictions on each member
			Set<restriction> r = null;
			if (restrictions!=null) {
				r = new HashSet<restriction>();
				r.addAll(restrictions);
			}
			MemberType m = v.elementAt(i);
			simpleType s = m.getSimpleType();
			if (s!=null && s.toRDF(subject,prop,node,value,seq,r,ctx)) return true;
			else if (s==null && xs.toRDF(node,subject,prop,value,m.getType(),seq,r,ctx)) return true;	
		}
		return false;
	}

	public RDFList toRDFList(Node node, String value, RDFList list, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		schema xs = (schema) this.get_owner();
		
		// record restrictions if not already doing so
		if (restrictions==null) restrictions = new HashSet<restriction>();
		
		// get (resolved) member types
		Vector<MemberType> v = get_memberTypes(ctx);
		for (int i=0; i<v.size(); i++) {
			// isolate restrictions on each member
			Set<restriction> r = new HashSet<restriction>();
			r.addAll(restrictions);
			MemberType m = v.elementAt(i);
			simpleType s = m.getSimpleType();
			RDFList l;
			if (s!=null && (l=s.toRDFList(node,value,list,r,ctx))!=null) return l;
			else if (s==null && (l=schema.toRDFList(node,value,m.getType(),list,r,ctx))!=null) return l;
		}
		return null;
	}

	public boolean toXML(Element e, RDFNode rdf, String pack, Context ctx) {
		schema xs = (schema) this.get_owner();
		// resolved member types
		Vector<MemberType> v = get_memberTypes(ctx);
		for (int i=0; i<v.size(); i++) {
			MemberType m = v.elementAt(i);
			simpleType s = m.getSimpleType();
			if (s!=null && s.toXML(e,rdf,pack,ctx)) return true;
			else if (s==null && xs.toXMLText(e,rdf,m.getType(),pack,ctx)) return true;
		}
		return false;
	}

	public boolean toXML(Attr attr, RDFNode rdf, Context ctx) throws Exception {
		schema xs = (schema) this.get_owner();
		// resolved member types
		Vector<MemberType> v = get_memberTypes(ctx);
		for (int i=0; i<v.size(); i++) {
			MemberType m = v.elementAt(i);
			simpleType s = m.getSimpleType();
			if (s!=null && s.toXML(attr,rdf,ctx)) return true;
			else if (s==null && xs.toXML(attr,rdf,null,ctx)) return true;
		}		
		return false;
	}

	public void defineType(Property prop, Context ctx) {
		// get (resolved) member types
		Vector<MemberType> v = get_memberTypes(ctx);
		for (int i=0; i<v.size(); i++) {
			MemberType m = v.elementAt(i);
			if (m.getSimpleType()!=null) m.getSimpleType().defineType(prop,ctx);
			else schema.defineType(prop,m.getType());
		}
	}

	Resource toOWL(OntModel ont, String uri, boolean createAnon, Context ctx) {
		schema xs = (schema) this.get_owner();
		UnionClass u = null;
		
		// get (resolved) member types
		Vector<MemberType> v = get_memberTypes(ctx);
		Set<Resource> r = new HashSet<Resource>();	
		
		for (int i=0; i<v.size(); i++) {
			MemberType m = v.elementAt(i);
			simpleType s = m.getSimpleType();
			String t = m.getType();
			// once inside a union the member types may be anonymous
			if (s!=null) {
				Resource rs = s.toOWL(ctx);
				if (rs!=null) r.add(rs);
				else {
					Gloze.logger.warn("Can't define union : "+uri);
					return null;
				}
			}
			else {
				Resource cls = schema.toOWL(ont,t);
				// a null (e.g for a QName) means we can't define the union
				if (cls==null) {
					Gloze.logger.warn("Can't define union : "+uri);
					return null;
				}
				else r.add(cls);
			}
		}
		if (uri!=null || createAnon) {
			u =  ont.createUnionClass(uri, ont.createList(r.iterator()));
			if (uri!=null) ctx.putOntClass(uri, u);		
		}
		return u;
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
	 * @return Returns the memberTypes.
	 */
	public String getMemberTypes() {
		return memberTypes;
	}

	/**
	 * @param memberTypes The memberTypes to set.
	 */
	public void setMemberTypes(String memberTypes) {
		this.memberTypes = memberTypes;
	}

	/**
	 * @return Returns the simpleType.
	 */
	public simpleType[] getSimpleType() {
		return simpleType;
	}

	/**
	 * @param simpleType The simpleType to set.
	 */
	public void setSimpleType(simpleType[] simpleType) {
		this.simpleType = simpleType;
	}

}
