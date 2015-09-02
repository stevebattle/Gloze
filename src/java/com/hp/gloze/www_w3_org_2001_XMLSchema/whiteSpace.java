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

import org.w3c.dom.Node;

import com.hp.gloze.Context;
import com.hp.gloze.XMLBean;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;

public class whiteSpace extends XMLBean {

	private String fixed, id, value = "";

	public whiteSpace() throws IntrospectionException {
	}
	
	/*! \page whiteSpace whiteSpace
	 WhiteSpace processing is a contentious area of XML and is one of the main reasons why round-tripping XML
	 produces an output that is (semantically) equivalent rather than (lexically) identical to the original.
	 The following example demonstrates three ways to control whitespace processing.
	 It inlcudes an element 'foobar' with liberally spaced mixed content interspersed with sub-elements 'foo'
	 and 'bar' both with content containing leading (tabbed) indentation.
	 
	 The whitespace processing of element 'foo' is determined by the xml:space attribute which indicates that whitespace
	 should be collapsed; trimming the leading whitespace. The whitespace processing of element 'bar' is
	 determined by the 'whiteSpace' restriction in the schema, which is also set to collapse whitespace.
	 
	 \include space.xml
	 \include space.xsd
	 
	 Finally, the gloze parameter space may be set to 'preserve' or 'default', equivalent to setting
	 xml:space in the document element. Mapping to RDF with space=default, 
	 then round-tripping back into XML we get the following (equivalenmt but not identical to the original).

	 \include space1.xml
	 
	 */

	public boolean toRDF(Resource subject, Node node, String value,
		Property prop, String type, Seq seq, Set<restriction> restrictions, Context ctx) throws Exception {
		schema xs = (schema) this.get_owner();
		if (this.value.equals("collapse")) value = schema.collapseWhitespace(value);
		else if (this.value.equals("replace")) value = schema.replaceWhitespace(value);
		return xs.toRDF(node,subject,prop,value,type,seq,restrictions,ctx);
	}

	public RDFList toRDFList(Node node, String value, String type, RDFList list, Context ctx) 
	throws Exception {
		schema xs = (schema) this.get_owner();
		if (this.value.equals("collapse")) value = schema.collapseWhitespace(value);
		else if (this.value.equals("replace")) value = schema.replaceWhitespace(value);
		return schema.toRDFList(node,value,type,list,null,ctx);
	}

	public String getFixed() {
		return fixed;
	}

	public void setFixed(String fixed) {
		this.fixed = fixed;
	}

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

}
