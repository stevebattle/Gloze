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
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Set;

import com.hp.gloze.Context;
import com.hp.gloze.Gloze;
import com.hp.gloze.XMLBean;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;

/*! \page redefine redefine
 
 This component is similar to include but also allows types and groups to be completely redefined.
 There is no comparable feature in OWL, so we simply include the new definitions as they appear
 in the redefinition. Because we can't selectively import things that are not redefined, anything
 unaffected by the redefinition is defined directly within the redefining ontology, rather than being imported. 
 In effect, the redefinitions shadow only the affected schema components.
 
 \include redefine.xsd
 
 This schema includes a type 'Foo' that is redefined, becoming a string; and a type 'Bar' that is
 unchanged.
 
 \include redefined.xsd
 \include redefine.owl
 
    \section redefineChildren Child components

	- \ref simpleType
	- \ref complexType
	- \ref group
	- \ref attributeGroup

 */

public class redefine extends XMLBean {
	String id, schemaLocation;
	
	simpleType[] simpleType;
	complexType[] complexType;
	group[] group;
	attributeGroup[] attributeGroup;
	
	// the included schema
	private schema _schema;

	/**
	 * @throws IntrospectionException
	 */
	public redefine() throws IntrospectionException {
	}

	/** invoked when (optional) namespace and schemaLocation attributes have been set */

	public void initialise() throws Exception {
		try {
			// trigger loading
			schema xs = (schema) get_owner();
			URL url = null;		
			if (isValidURI(schemaLocation) && !schemaLocation.startsWith("file"))
				url = new URL(schemaLocation);
			else url = new URI(xs.get_location()+ schemaLocation).normalize().toURL();

			_schema = Gloze.loadSchema(url);
			_schema.set_owner(this.get_owner());
		}
		catch (Exception e) {
			Gloze.logger.error("can't include "+schemaLocation);
			throw(e);
		}
	}

	/** translate the included schema to OWL and include it  */

	public void toOWL(OntModel ont, File target, String location, Context ctx) throws Exception {
		try {
			// redefined simple types
			for (int i=0; simpleType!=null && i<simpleType.length; i++)
				simpleType[i].toOWL(ctx);
			// redefined complex types
			for (int i=0; complexType!=null && i<complexType.length; i++)
				complexType[i].toOWL(null,false,ctx);
			// groups
			for (int i=0; group!=null && i<group.length; i++)
				group[i].toOWL(null,1,1,ctx);
			// attribute groups
			for (int i=0; attributeGroup!=null && i<attributeGroup.length; i++)
				attributeGroup[i].toOWL(null,ctx);
			
			// map included schema
			String suffix = ctx.getLang().toLowerCase().equals("n3")?"n3":"owl";
			String loc = changeSuffix(schemaLocation, suffix);
			URI base = prune(ctx.getBaseMap().resolve(location)).resolve(loc);
			Context c = ctx.copy();
			c.setBase(base);
			
			if (target!=null) {
				File t = target.getCanonicalFile();
				if (!t.isDirectory()) t = t.getParentFile();
				ont.add(_schema.toOWL(new File(t,loc),base.toString(),true, c).getBaseModel());
			}
			else ont.add(_schema.toOWL((File)null,base.toString(),true, c).getBaseModel());
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	public String getId() {
		return id;
	}

	public String getSchemaLocation() {
		return schemaLocation;
	}

	public void setId(String string) {
		id = string;
	}

	public void setSchemaLocation(String location) throws Exception {
		schemaLocation = location;		
	}

	public schema get_schema() {
		return _schema;
	}
	
	public attributeGroup[] getAttributeGroup() {
		return attributeGroup;
	}

	public void setAttributeGroup(attributeGroup[] attributeGroup) {
		this.attributeGroup = attributeGroup;
	}

	public complexType[] getComplexType() {
		return complexType;
	}

	public void setComplexType(complexType[] complexType) {
		this.complexType = complexType;
	}

	public group[] getGroup() {
		return group;
	}

	public void setGroup(group[] group) {
		this.group = group;
	}

	public simpleType[] getSimpleType() {
		return simpleType;
	}

	public void setSimpleType(simpleType[] simpleType) {
		this.simpleType = simpleType;
	}

	public void gatherGlobals(Model m, Context ctx, Set<URL> done) {
		schema xs = get_schema();
		if (!done.contains(xs.get_url())) xs.gatherGlobals(m, ctx, done);	
		
		// redefine simple types
		for (int i = 0; simpleType!=null && i < simpleType.length; i++) {
			simpleType[i].resolve(m,ctx);
			ctx.putSimpleType(simpleType[i].createURI(m,ctx),simpleType[i]);
		}
		// redefine complex types
		for (int i = 0; complexType!=null && i < complexType.length; i++) {
			complexType[i].resolve(m,ctx);
			ctx.putComplexType(complexType[i].createURI(m,ctx),complexType[i]);
		}
		// redefine groups
		for (int i = 0; group!=null && i < group.length; i++) {
			group[i].resolve(m,ctx);
			ctx.putGroup(group[i].createURI(m,ctx),group[i]);
		}
		// redefine attribute groups
		for (int i = 0; attributeGroup!=null && i < attributeGroup.length; i++) {
			attributeGroup[i].resolve(m,ctx);
			ctx.putAttributeGroup(attributeGroup[i].createURI(m,ctx),attributeGroup[i]);
		}
	}

}
