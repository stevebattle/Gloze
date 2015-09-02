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
import java.util.Arrays;
import java.util.Vector;

import org.w3c.dom.Node;

import com.hp.gloze.Context;
import com.hp.gloze.XMLBean;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.ontology.DataRange;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Resource;

/*! \page enumeration enumeration
 XML schema also allows a simple type restriction to be defined by enumeration. 
 OWL Enumerations allow classes to be defined extensionally, in terms of their membership.
 For data-types, the class of values is specified as an OWL DataRange, with OWL oneOf listing the permitted range of values.
 
 \include enumeration.xsd
 
 Because xs:QNames are not recommended for use in RDF (they depend on locally defined prefixes)
 they are fully expanded to form absolute URIs. The QNames that appear in the schema are unprefixed
 and so are defined in the default XML namespace.
 
 \include enumeration.owl
 */
public class enumeration extends XMLBean {

	private String id, value;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public enumeration() throws IntrospectionException {
	}
	
	@SuppressWarnings("unchecked")
	public static DataRange toOWL(OntModel ont, String type, enumeration[] en) {
		Vector v = new Vector();
		RDFDatatype dt = type==null?null:getDatatype(type);
		for (int i=0; i<en.length; i++) {
			if (dt!=null) v.add(ont.createTypedLiteral(en[i].getValue(),dt));
			else v.add(ont.createLiteral(en[i].getValue()));
		}
		return ont.createDataRange(ont.createList(v.iterator()));
	}
	
	/* return a list of individuals that constitute an enumeration */
	
	public static RDFList toOWL(OntModel ont, enumeration[] en, Node node, Context ctx) {
		Vector<Resource> v = new Vector<Resource>();
		for (int i=0; i<en.length; i++) {
			String uri = expandQName(ctx.getDefaultNS(),en[i].getValue(), node, ont);
			v.add(ont.getResource(uri));
		}
		return ont.createList(v.iterator());		
	}

	public boolean isValid(String value, String type) {
		RDFDatatype dt = getDatatype(type);
		if (dt!=null && dt.isValid(value) && dt.isValid(this.value)) {
			if (type.equals(schema.XSD_URI+"#base64Binary")
			 || type.equals(schema.XSD_URI+"#hexBinary")) {
				byte[] b = (byte[]) getDatatype(type).parse(value);
				byte[] b1 = (byte[]) getDatatype(type).parse(this.value);
				return Arrays.equals(b,b1);
			}
			else return dt.parse(value).equals(dt.parse(this.value));
		}
		else return value.equals(this.value);
	}

}
