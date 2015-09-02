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

import com.hp.gloze.Content;
import com.hp.gloze.Context;
import com.hp.gloze.Restrictions;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;

/*! \page complexContent

	\section elementChildren Child components
	
	- \ref restriction "restriction"
	- \ref extension "extension"
*/


public class complexContent extends Content {

	private String id, mixed;
	private restriction restriction;
	private extension extension;
	
	public complexContent() throws IntrospectionException {
	}
	
	public boolean needSeq(Set<String> names, Context ctx) {
		if (mixed!=null && mixed.equals("true")) return true;
		if (extension!=null) return extension.needSeq(names, ctx);
		return false;
	}

	public int toRDF(Resource subject, Element elem, int index, Seq seq, boolean mixed, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		if (extension != null) 
			return extension.toRDF(isMixed(mixed), subject, elem, index, seq, restrictions, ctx);
		else if (restriction!=null) 
			return restriction.toRDF(isMixed(mixed), subject, elem, index, seq, this, restrictions,ctx);
		return index;		
	}

	public int toXML(Element e, Resource rdf, int index, Set<Statement> pending, Context ctx) {
		if (extension!=null) return extension.toXML(e,rdf,index,pending,ctx);
		else if (restriction!=null) return restriction.toXML(e,rdf,index,pending,this,ctx);
		return index;
	}

	public boolean subtype(String type, Model model, Context ctx) {
		if (extension!=null) return extension.subtype(type, model,ctx);
		if (restriction!=null) return restriction.subtype(type, model,ctx);
		return false;
	}

	public void assertType(Resource subject, Context ctx) {
		if (extension != null) extension.assertType(subject,ctx);
		else if (restriction!=null) restriction.assertType(subject,ctx);
	}
	
	public void toOWL(OntModel ont, Resource cls, Restrictions rest, Context ctx) {
		if (extension!=null) extension.toOWLComplexContent(cls, rest, ctx);
		else if (restriction!=null) restriction.toOWLComplexContent(ont,cls,rest,ctx);
	}
	
	public void subClass(Resource c, Context ctx) {
		if (extension!=null) extension.subClass(c, schema.type.complexType, ctx);		
		if (restriction!=null) restriction.subClassComplexContent(c, ctx);		
	}
	
	public String getId() {
		return id;
	}
	
	public boolean isMixed(boolean parentMixed) {
		if (mixed!=null) return mixed.equals("true");
		// otherwise inherit mixed from parent complexType
		return parentMixed;
	}

	public String getMixed() {
		return mixed;
	}

	public restriction getRestriction() {
		return restriction;
	}

	public void setId(String string) {
		id = string;
	}

	public void setMixed(String string) {
		mixed = string;
	}

	public void setRestriction(restriction restriction) {
		this.restriction = restriction;
	}

	public extension getExtension() {
		return extension;
	}

	public void setExtension(extension extension) {
		this.extension = extension;
	}

}
