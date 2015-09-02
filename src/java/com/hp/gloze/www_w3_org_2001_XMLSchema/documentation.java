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

import org.w3c.dom.Element;

import com.hp.gloze.Context;
import com.hp.gloze.XMLBean;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

/*! \page documentation Documentation

	Documentation on a schema, element, attribute or complex type is
	recorded as rdfs:comment. Documentation for the schema is added to the owl:Ontology header.
	Documentation for a complex type is added to the corresponding class.
	Documentation for an element or attribute is added to the corresponding property.
	
	\include documentation.xsd
	
	The translation into OWL is as follows:
	
	\include documentation.owl

*/

public class documentation extends XMLBean {

	private String source ;

	public documentation() throws IntrospectionException {
	}

	public void toOWL(Resource resource, Context ctx) {
		if (resource==null) return;
		String v = getValue((Element)this._node);
		v = schema.collapseWhitespace(schema.replaceWhitespace(v));
		if (v==null || v.equals("")) return;

		if (getLang()!=null) resource.addProperty(RDFS.comment, v,getLang());
		else resource.addProperty(RDFS.comment, v);
	}

	public String getSource() {
		return source;
	}


	public void setSource(String source) {
		this.source = source;
	}

}
