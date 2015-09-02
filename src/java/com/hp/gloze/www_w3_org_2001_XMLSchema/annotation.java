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

import com.hp.gloze.Context;
import com.hp.gloze.XMLBean;
import org.apache.jena.rdf.model.Resource;

/*! \page annotation annotation
 
 A documentation annotation on a schema, element, attribute or complex type is
 recorded as rdfs:comment. See documentation below for examples.
 
 \section annotationChildren Child components

 - \ref documentation

 */

public class annotation extends XMLBean {

	private String id;
	private documentation[] documentation;

	public void toOWL(Resource resource, Context ctx) {
		// process documentation
		for (int i=0; documentation!=null && i<documentation.length; i++)
			documentation[i].toOWL(resource,ctx);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public annotation() throws IntrospectionException {
	}

	public documentation[] getDocumentation() {
		return documentation;
	}

	public void setDocumentation(documentation[] documentation) {
		this.documentation = documentation;
	}

}
