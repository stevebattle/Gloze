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
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.util.Set;

import com.hp.gloze.Context;
import com.hp.gloze.Gloze;
import com.hp.gloze.XMLBean;
import org.apache.jena.rdf.model.Model;

/*! \page import Import
	 If a schema imports a schema then that schema is automatically loaded.
	 When lifting a schema to OWL, a corresponding owl:imports is generated. The URI used in the
	 import is base relative.
	 
	 \include import.xsd
	 \include import.owl
 */

public class Import extends XMLBean {
	private static final String XMLSCHEMA_XSD = "http://www.w3.org/2001/XMLSchema.xsd";

	String id, namespace, schemaLocation;
	
	// the imported schema
	private schema schema;

	/**
	 * @throws IntrospectionException
	 */
	public Import() throws IntrospectionException {
	}
	
	/** initialise by loading referenced schema, all attributes already set */

	public void initialise() throws Exception {
		try {
			// it may be preloaded
			schema = Gloze.getCachedSchema(namespace);
			if (schemaLocation!=null && !schemaLocation.equals(XMLSCHEMA_XSD)) {
				URL url = null;
				// be more conservative for remote schema (assume namespace is unique)
				if (isValidURI(schemaLocation) && !schemaLocation.startsWith("file")) {
					url = new URL(schemaLocation);
					// load if we didn't have a cached schema
					if (schema==null) schema = Gloze.loadSchema(url);
				}
				// be more liberal for local schema (assume namespace is not unique)
				else {
					url = new URI(get_owner().get_location()+ schemaLocation).normalize().toURL();
					schema = Gloze.loadSchema(url);
				}
			}
		}
		catch (FileNotFoundException e) {
			Gloze.logger.warn(e.getMessage());
		}
	}
	
	public void gatherGlobals(Model model, Context ctx, Set<URL> done) {
		schema xs = getSchema();
		if (xs==null) 
			Gloze.logger.warn("no schema for namespace: "+namespace);
		else if (!done.contains(xs.get_url())) xs.gatherGlobals(model, ctx, done);	
	}

	/** translate the imported schema to OWL for owl:imports  */

	public void toOWL(File target, String location, Context ctx) 
	throws Exception {
		schema xs = (schema) get_owner();
		try {
			// The schema may be null if say we explicitly import the (redundant) XML schema for schema
			if (schema!=null && ctx.getBaseMap()!=null) {				
				// identify schema location
				String l = null;
				if (schemaLocation!=null) l = changeSuffix(schemaLocation, "owl");
				else {
					// if schemaLocation is missing try user defined namespace/schema pairing
					l = changeSuffix(schema.get_url().toURI().toString(), "owl");
					// assume this is in the same target folder as the parent
					int n; if ((n = l.lastIndexOf("/"))>=0) l = l.substring(n+1);			
				}
				
				// add owl:imports to the ontology
				URI o = ctx.getBaseMap().resolve(location);
				URI u = prune(o).resolve(l);
				xs.ont.getOntology(ctx.getBaseMap().toString()).addImport(xs.ont.createResource(u.toString()));
				
				URI base = prune(ctx.getBaseMap().resolve(location)).resolve(l);

				// map imported schema
				if (target!=null) {
					File b = target.getCanonicalFile();
					if (!b.isDirectory()) b = b.getParentFile();
					schema.toOWL(new File(b, l),base.toString(),false, new Context(base, ctx));
				}
				else schema.toOWL((File)null,base.toString(),false, new Context(base, ctx));
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	public String getId() {
		return id;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getSchemaLocation() {
		return schemaLocation;
	}

	public void setId(String string) {
		id = string;
	}

	public void setNamespace(String string) {
		namespace = string;
	}

	public void setSchemaLocation(String location) throws Exception {
		schemaLocation = location;	
	}

	public schema getSchema() {
		return schema;
	}
		
}
