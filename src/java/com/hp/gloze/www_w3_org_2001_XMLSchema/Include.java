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
import org.apache.jena.rdf.model.Model;

/*! \page include Include
If a schema includes a schema then that schema is automatically loaded.
When lifting a schema to OWL, a corresponding owl:imports is generated. The URI used in the
import is base relative.

\include include.xsd
\include include.owl
*/

public class Include extends XMLBean {
	String id, schemaLocation;
	
	// the included schema
	private schema schema;

	/**
	 * @throws IntrospectionException
	 */
	public Include() throws IntrospectionException {
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
			
			schema = Gloze.loadSchema(url);
			schema.set_owner(this.get_owner());
		}
		catch (Exception e) {
			Gloze.logger.error("can't include "+schemaLocation);
			throw(e);
		}
	}

	public void gatherGlobals(Model model, Context ctx, Set<URL> done) {
		schema xs = getSchema();
		if (!done.contains(xs.get_url())) xs.gatherGlobals(model, ctx, done);	
	}
	
	/** translate the included schema to OWL and include it  */
	// location - of the parent relative to its base

	public void toOWL(File target, String location, Context ctx) throws Exception {
		schema xs = (schema) get_owner();
		try {
			// add owl:imports to the ontology
			if (ctx.getBaseMap()!=null) {
				URI o = ctx.getBaseMap().resolve(location);
				String loc = changeSuffix(schemaLocation, "owl");
				URI uri = prune(o).resolve(loc);
				xs.ont.getOntology(ctx.getBaseMap().toString()).addImport(xs.ont.createResource(uri.toString()));
				
				// map included schema
				URI base = prune(ctx.getBaseMap().resolve(location)).resolve(loc);
				Context c = ctx.copy();
				c.setBase(base);
				
				if (target!=null) {
					File t = target.getCanonicalFile();
					if (!t.isDirectory()) t = t.getParentFile();
					schema.toOWL(new File(t,loc),base.toString(),false, c);
				}
				else schema.toOWL((File)null,base.toString(),false, c);
			}
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

	public schema getSchema() {
		return schema;
	}
	
}
