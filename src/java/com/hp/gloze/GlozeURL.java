/*! \page license License
 *  (c) Copyright Hewlett-Packard Company 2001 - 200
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
 * @author jeanmarc.vanel@gmail.com
 */

package com.hp.gloze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hp.gloze.www_w3_org_2001_XMLSchema.element;
import com.hp.gloze.www_w3_org_2001_XMLSchema.schema;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.vocabulary.RDF;

/**
 * The programmatic interface for Gloze.
 */

public class GlozeURL extends Gloze {

	public String closed = System.getProperty("gloze.closed");

	private static final int SCHEMA_CACHE_MAX = 100;
	public static Logger logger = Logger.getLogger("com.hp.gloze");
	public static Map<URL,schema> schemaCache = new Hashtable<URL,schema>(SCHEMA_CACHE_MAX);
	private static final String RULES = "owl.rules";
	static String OK = "http://www.hp.com/gloze#OK";
	
	// optionally define a base URI to identify the document element
	// the default document URI is the filename, this can be overridden
	String base = System.getProperty("gloze.base");
	
	// define a namespace for unqualified elements and attributes
	String xmlns = System.getProperty("gloze.xmlns");
	
	// output options
	String roundtrip = System.getProperty("gloze.roundtrip");
	String verbose = System.getProperty("gloze.verbose");
	String target = System.getProperty("gloze.target");
	// lang options are RDF/XML RDF/XML-ABBREV N-TRIPLE N3
	String lang = System.getProperty("gloze.lang");
	String schemaLocation = System.getProperty("gloze.schemaLocation");
	String order = System.getProperty("gloze.order");	
	String space = System.getProperty("gloze.space");	
	// optional symbolic prefix for attributes (eg. @) and elements (eg. ~)
	// to distinguish them from types (unprefixed)
	String _element = System.getProperty("gloze.element");
	String _attribute = System.getProperty("gloze.attribute");
	String report = System.getProperty("gloze.report");
	// class declaration style (subClassOf or intersectionOf)
	String _class = System.getProperty("gloze.class");
	String trace = System.getProperty("gloze.trace");
	String overwrite = System.getProperty("gloze.overwrite");
	public String fixed = System.getProperty("gloze.fixed");
	
	boolean silent = false;

	// schema associated with this Gloze instance <schema location URL to schema instance>
	public Map<URL,schema> schemaMap = new HashMap<URL,schema>();
	
	/**
	 * Create an instacen of Gloze.
	 */
	
	public GlozeURL() {
		if (lang==null) lang = "RDF/XML-ABBREV";
		if (_class==null) _class = "subClassOf";
	}
	
	/**
	 * Construct an instance of Gloze. If no target is provided then the output is normally written to the console.
	 * This constructor provides the option to silence this output, and is useful for test purposes.
	 * @param silent : boolean disables output where no target is supplied
	 */

	public GlozeURL(boolean silent) {
		this();
		this.silent = silent;
	}

	/** Construct an instance of Gloze for a schema with the given target namespace.
	 * 
	 * @param schemaLocation : URL 
	 * @param targetNS : URI
	 * @throws Exception
	 */

	public GlozeURL(URL schemaLocation, URI targetNS) throws Exception {
		this();
		initSchema(targetNS, schemaLocation, schemaMap);
	}
	
	/** Construct an instance of Gloze given multiple schema locations and target namespaces.
	 * 
	 * @param schemaLocation : array of URLs
	 * @param targetNS : array of URIs
	 * @throws Exception
	 */

	public GlozeURL(URL[] schemaLocation, URI[] targetNS) throws Exception {
		this();
		for (int i = 0; i < schemaLocation.length; i++) initSchema(targetNS[i], schemaLocation[i], schemaMap);
	}
	

	/**
	 * Load schema with URL into a static cache.
	 * The schema is not associated with a gloze instance.
	 * @param url : schema URL
	 * @return schema instance
	 * @throws Exception
	 */
	
	public static schema loadSchema(URL url) throws Exception {
		// look up a schema by its URL
		// dump it if it gets too large
		if (schemaCache.size()>SCHEMA_CACHE_MAX) clearCache();
		schema xs = GlozeURL.schemaCache.get(url);
		if (!GlozeURL.schemaCache.containsKey(url)) {
			// otherwise parse the input
			if ("true".equals(System.getProperty("gloze.verbose"))) logger.info("reading " + url);
			Document doc = XMLUtility.read(input(url, null));
			if (doc!=null) {
				xs = (schema) XMLBean.newShallowInstance(doc.getDocumentElement());
				// add to the cache before continuing with imports
				GlozeURL.schemaCache.put(url, xs);
				// required to break loops in mutually imported schema
				xs.set_url(url);
				xs.set_location(parent(url));
				xs.populate();
			}
		}
		return xs;
	}

	/**
	 * Clear the static cache.
	 */
	public static void clearCache() {
		schemaCache = new Hashtable<URL,schema>(SCHEMA_CACHE_MAX);
	}
	
	/**
	 * Get a schema from the static cache.
	 * @param namespace
	 * @return schema instance
	 */
	
	public static schema getCachedSchema(String namespace) {
		for (Iterator<schema> i = GlozeURL.schemaCache.values().iterator(); i.hasNext(); ) {
			schema xs = i.next();
			if (namespace.equals(xs.getTargetNamespace())) return xs;
		}
		return null;
	}

	/**
	 * Initialise a gloze instance with a schema given its URL and target namespace.
	 * The schemaMap is updated with the new mapping.
	 * @param namespace : URI
	 * @param url : schema location URL
	 * @param schemaMap : Maps schema location to schema instance
	 * @throws Exception 
	 */
	
	public static schema initSchema(URI namespace, URL url, Map<URL,schema> schemaMap) throws Exception {
		schema xs = null;
		if (!url.getProtocol().equals("file")) {
			xs = getCachedSchema(namespace.toString());
			if (xs!=null) return xs;
		}
		xs = loadSchema(url);
		if (xs!=null) {
			String tns = xs.getTargetNamespace();
			if (namespace!=null && tns!=null && !tns.equals(namespace.toString()))
				logger.warn("target namespace mismatch: " + namespace + " " + tns);
			schemaMap.put(url,xs);
		}
		return xs;
	}
	
	/** Initialise schema from schema location(s) defined in an xml element 
	 * (typically the document element or an element corresponding to an xs:any wild-card).
	 * @param element : potentially containing a schemaLocation
	 * @param location : URL of this XML document.
	 * @param defaultNS used instead of the schema URL.
	 * @param schemaMap maps schema location to schema instance.
	 * @throws Exception
	 */
	
	public static schema initSchemaXSI
	(Element element, URL location, String defaultNS, Map<URL,schema> schemaMap) 
	throws Exception {
		// get schema from xml schema location
		String l = element.getAttributeNS(schema.XSI, "schemaLocation");
		if (l!=null) {
			// l is a string of namespace filename pairs
			StringTokenizer t = new StringTokenizer(l);
			while (t.hasMoreTokens()) {
				String first = t.nextToken();
				String hint = null;
				try {
					if (t.hasMoreTokens()) {
						// the first token represents the tns
						// the following token is the file location
						hint = t.nextToken();
						return initSchemaLocation(new URI(first), location, hint, defaultNS, schemaMap);
					} else { // treat this as a sole filename
						hint = first;
						return initSchemaLocation(null, location, hint, defaultNS, schemaMap);
					}
				} catch (Exception x) {
					logger.warn("ignored schema location hint " + hint);
				}
			}
		}
		// a schema can have both schemaLocation and noNamespaceSchemaLocation defined
		l = element.getAttributeNS(schema.XSI, "noNamespaceSchemaLocation");
		if (!l.equals("")) {
			return initSchemaLocation(null, location, l, defaultNS, schemaMap);
		}
		return null;
	}

	private static schema initSchemaLocation
	(URI ns, URL location, String hint, String defaultNS, Map<URL,schema> schemaMap) 
	throws Exception {
		URL url;
		// absolute URL
		if (hint.indexOf(":")>=0 || location==null) url = new URL(hint);
		// relative URL
		else url = new URL(location.toString() + hint);
		return initSchemaLocation(ns, url, defaultNS, schemaMap);
	}
	
	/** Add the schema, with given namespace and location, to the schema map.
	 * @param namespace : the preferred namespace
	 * @param schemaLocation
	 * @param defaultNS : used if no namespace is supplied
	 * @param schemaMap : updated with the new schema mapping
	 * @return schema instance
	 * @throws Exception
	 * @throws URISyntaxException
	 */

	public static schema initSchemaLocation
	(URI namespace, URL schemaLocation, String defaultNS, Map<URL,schema> schemaMap) 
	throws Exception, URISyntaxException {
		if (namespace!=null) return initSchema(namespace, schemaLocation, schemaMap);
		// user defined namespace
		else if (defaultNS!=null) return initSchema(new URI(defaultNS), schemaLocation, schemaMap);
		// fallback to using the schema URL
		else return initSchema(schemaLocation.toURI().normalize(), schemaLocation, schemaMap);
	}
	
	/* Adds xsi:schemaLocation to an output XML document */
	
	private Document addSchemaLocation(Document doc, URI base) throws Exception {
		Element e = doc.getDocumentElement();
		StringBuffer loc = null;
		StringBuffer noNSloc = null;
		Set<String> added = new HashSet<String>();
		for (URL l : schemaMap.keySet()) {
			schema xsd = schemaMap.get(l);
			for (URL url : schemaCache.keySet()) {
				if (schemaCache.get(url).equals(xsd)) {
					String uri = XMLBean.relativize(base, url.toURI()).toString();
					if (uri.startsWith(base.toString())) 
						uri = uri.substring(base.toString().length());
					if (xsd.getTargetNamespace()==null && !added.contains(uri)) {
						if (noNSloc==null) noNSloc = new StringBuffer();
						else noNSloc = noNSloc.append(" ");
						noNSloc.append(uri);
						added.add(uri);
					}
					else if (!added.contains(uri)) {
						if (loc==null) loc = new StringBuffer();
						else loc.append(" ");
						loc.append(xsd.getTargetNamespace());
						loc.append(" ");
						loc.append(uri);
						added.add(uri);
					}
				}
			}
		}
		if (loc!=null)
			e.setAttributeNS(schema.XSI,"xsi:schemaLocation",loc.toString());
		if (noNSloc!=null)
			e.setAttributeNS(schema.XSI,"xsi:noNamespaceSchemaLocation",noNSloc.toString());
		return doc;
	}
	
	protected void xml_to_rdf( URL source, URI base, Model model) throws Exception {
		Document doc = null;
		try {
			doc = XMLUtility.read( source.openStream() );
		} catch (FileNotFoundException e) {
			logger.warn("File not found: "+source.getPath());
			return;
		}
		try {
			lift(doc, source, base, model);			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** Lift XML to RDF metadata (given source and target files). 
	 * The Gloze instance should be pre-initialised with the relevant XML schema.
	 * @param source document
	 * @param target file
	 * @param base of the XML document
	 * @param model for the RDF
	 */

	public void xml_to_rdf( URL source, File target, URI base, Model model) throws Exception {
		xml_to_rdf(source, base, model);
		
		// set base (see http://jena.sourceforge.net/IO/iohowto.html)
		RDFWriter writer = model.getWriter(lang);	
		//writer.setProperty("xmlbase", base.toString());
		writer.setProperty("showXmlDeclaration", "true");

		// output options
		if (target != null) {
			if ("true".equals(verbose)) logger.info("writing "+target.getName());
			//model.write(new FileWriter(target), lang);
			writer.write(model,new FileWriter(target), base.toString());
		}
		else if (!silent) 
			//model.write(System.out, lang);
			writer.write(model,System.out, base.toString());

		
		// roundtrip xml
		if (roundtrip!=null && roundtrip.equals("true")) {
			Document doc = drop(model, base);
			if (schemaLocation!=null) addSchemaLocation(doc, new URI(schemaLocation));
			XMLUtility.write(doc, new PrintWriter(System.out));
		}
	}

	private void xml_to_rdf( URL source, String name) throws Exception {
		Model m = ModelFactory.createDefaultModel();
		String suffix = lang.toLowerCase().equals("n3") ? "n3" : "rdf" ;
		
		// derive target
		File t = this.target!=null?new File(this.target):null, target = t;
		if (t!=null && (t.isDirectory() || t.getName().indexOf('.')<0)) {
			if (!t.exists()) t.mkdirs();
			target = new File(t, XMLBean.changeSuffix(source.getFile(), suffix));
		}
		
		// derive base
		URI base = null;
		// base is (in order of preference) user assigned, derived from the target, or the source
		if (this.base!=null && this.base.endsWith("/") && name!=null) base = new URI(this.base + name);
		else if (this.base!=null) base = new URI(this.base);
		else if (target!=null) base = target.toURI();
		else base = source.toURI();
		
		xml_to_rdf(source, target, base, m);
	}

	/** Drop RDF metadata into XML (given source and target files). 
	 * The Gloze instance should be pre-initialised with the relevant XML schema.
	 * The model should be initialised with a prefix map used to define define xml namespaces.
	 * @param source RDF
	 * @param target file
	 * @param base URI for XML
	 * @param model containing a prefix map.
	 * @return XML document
	 */

	public Document rdf_to_xml( URL source, File target, URI base, Model model) throws Exception {
		// read model and drop into XML
		if (lang.equals("N3")
				|| source.getFile().endsWith(".n3")
				|| source.getFile().endsWith(".ttl")
			)
			model.read( source.openStream(),
					base.toString(), "N3" );
		else {
			model.read( source.openStream(), base.toString()); }
		
		// check that jena has initialised the namespace prefix map
		if (source.getFile().endsWith(".rdf")) {
			Document rdf = XMLUtility.read( source.openStream() );
			NamedNodeMap amap = rdf.getDocumentElement().getAttributes();
			Map<String, String> pmap = model.getNsPrefixMap();
			for (int i=0 ;i<amap.getLength(); i++) {
				Node a = amap.item(i);
				String name = a.getNodeName();
				String lname = a.getLocalName();
				if (name.startsWith("xmlns:") && !pmap.containsKey(lname)) 
					model.setNsPrefix(lname,a.getNodeValue());
			}
		}
		
		Document doc = drop(model, base);
		if (schemaLocation!=null) addSchemaLocation(doc,new URI(schemaLocation));

		if (this.target != null) XMLUtility.write(doc, new FileWriter(target));
		else if (!silent) XMLUtility.write(doc, new PrintWriter(System.out));
		return doc;
	}
	
	private void rdf_to_xml( URL source, String name) throws Exception {
		Model m = ModelFactory.createDefaultModel();
		
		// derive target
		File t = this.target!=null?new File(this.target):null, target = t;
		if (t!=null && (t.isDirectory() || t.getName().indexOf('.')<0)) {
			if (!t.exists()) t.mkdirs();
			target = new File(t, XMLBean.changeSuffix(source.getFile(), "xml"));
		}
		
		// derive base
		URI base = null;
		// base is (in order of preference) user assigned, derived from the target, or the source
		if (this.base!=null && this.base.endsWith("/") && name!=null) base = new URI(this.base + name);
		else if (this.base!=null) base = new URI(this.base);
		else if (target!=null) base = target.toURI();
		else base = source.toURI();
		
		rdf_to_xml(source, target, base, m);
	}
	
	/** Lift XML schema to OWL ontology.
	 * @param source schema
	 * @param base of the ontology
	 * @return ontology
	 */
	
	public OntModel xsd_to_owl( URL source, String base) throws Exception {
		schema xs = loadSchema(source);
		if (xs==null) return null;
		
		schemaMap.put(source, xs);
		String suffix = "owl";
		String name = XMLBean.changeSuffix(source.getFile(),suffix);

		// derive target
		File t = this.target!=null?new File(this.target):null, target = t;
		if (t!=null && (t.isDirectory() || t.getName().indexOf('.')<0)) {
			if (!t.exists()) t.mkdirs();
			target = new File(t, name);
		}

		// derive base
		if (base!=null && base.endsWith("/") && name!=null) base += name;
		URI b = null;
		if (base!=null) b = new URI(base);
		else if (target!=null) b = target.toURI();
		else b = source.toURI();

		// create a context gathering global definitions
		Context ctx = new Context(b,xmlns,ModelFactory.createDefaultModel(),schemaMap,this);
		String ns = xs.getTargetNamespace();
		if (ns==null) ns = ctx.getDefaultNS();
		initSchema(new URI(ns),source, schemaMap);

		// load rules and initialise reasoner
		InputStream rules = GlozeURL.class.getResourceAsStream(RULES);
		BufferedReader br = new BufferedReader(new InputStreamReader(rules));
		List<Rule> l = Rule.parseRules(Rule.rulesParserFromReader(br));
		GenericRuleReasoner r = new GenericRuleReasoner(l);
		r.setTraceOn(trace!=null && trace.equals("true"));
		ctx.setReasoner(r);
		
		OntModel m = xs.toOWL(target, name, false, ctx);
//		ctx.getModel().write(System.out,"RDF/XML-ABBREV");
		return m;
	}
	
	private static InputStream input(URL source, URI hint) throws Exception {
		try {
			if (source.toString().startsWith("file:")) {
				return new FileInputStream(source.getFile());
			}
			if (source.getProtocol().equals("http")) {
				return source.openConnection().getInputStream();
			}
			if (hint != null && hint.toString().startsWith("file:")) {
				return new FileInputStream(new File(hint));
			}
			if (hint != null && hint.toString().startsWith("http:")) {
				return hint.toURL().openConnection().getInputStream();
			}
			if (source != null && source.toString().startsWith("http:")) {
				return source.openConnection().getInputStream();
			}
		}
		catch (FileNotFoundException e) {
			GlozeURL.logger.warn("file not found: "+source);
		}
		catch (ConnectException e) {
			GlozeURL.logger.warn("connection refused: "+source);
		}
		return null;
	}

	private static URL parent(URL uri) {
		try {
			String s = uri.toString();
			return new URL(s.substring(0, s.lastIndexOf('/') + 1));
		} catch (Exception e) {
			return null;
		}
	}
	
	URI getBase(Document xml) throws Exception {
		URI base = null;
		Element e = xml.getDocumentElement();
		if (e.hasAttributeNS(XMLBean.XML,"base"))
			base = new URI(e.getAttributeNS(XMLBean.XML,"base"));
		return base;
	}

	/** recommended programmatic interfaces */
	
	/** Lift XML (document) into RDF metadata, creating a new model.
	 * The Gloze instance should be pre-initialised with the relevant XML schema.
	 * @param xml input document to lift
	 * @param url location of the input document (required for relative schema location)
	 * @param base : URI of the input document base
	 * @return model
	 */
	
	public Model lift(Document xml, URL url, URI base) throws Exception {
		Model model = ModelFactory.createDefaultModel();
		lift(xml, url, base, model);
		return model;
	}
	
	/** Lift XML (document) into RDF metadata, adding to existing model.
	 * The Gloze instance should be pre-initialised with the relevant XML schema. 
	 * @param xml input document
	 * @param url location of the input document (required for resolving relative schema location)
	 * @param base : URI of input document base
	 * @param model output model
	 * @return boolean indicating success or failure.
	 * @throws Exception
	 */

	public boolean lift(Document xml, URL url, URI base, Model model) throws Exception {
		// load additional schema identified in the XML instance
		Element d = xml.getDocumentElement();
		initSchemaXSI(d, parent(url),xmlns,schemaMap);
		// last ditch attempt to look for schema with same name
		if (schemaMap.isEmpty()) 
			initSchema(null,new URL(XMLBean.changeSuffix(url.toString(),"xsd")),schemaMap);
		
		// initialise base from the XML source if available
		URI b = getBase(xml);
		if (b!=null) base = b;
		
		Context ctx = new Context(base,xmlns,model,schemaMap,this);
		
		for (URL u: schemaMap.keySet()) schemaMap.get(u).declareGlobalNS(model,ctx);

		//	create a named or anonymous resource
		Resource rez;
		if (base != null) rez = model.createResource(base.toString());
		else rez = model.createResource();
		
		element e = ctx.getElement(element.createURI(d,ctx));
		if (e!=null) return e.toRDF(rez, d, null,ctx);
		
		logger.warn("using no schema mapping for document element: "+d.getLocalName());
		// use default no-schema mapping
		XMLBean.noSchemaToRDF(d,base,ctx);
		return true;
	}

	/** Drop RDF model into XML (starting with named resource).
	 * The Gloze instance should be pre-initialised with the relevant XML schema.
	 * @param model containing the RDF meta-data
	 * @param uri the (named) root of the XML output
	 * @return XML Document
	 */

	public Document drop(Model model, URI uri) throws Exception {
		Resource rez = model.getResource(uri.toString());
		return drop(rez);
	}
	
	/** Drop RDF resource into XML.
	 * The Gloze instance should be pre-initialised with the relevant XML schema.
	 * @param resource resource representing the root of the XML document
	 * @return XML Document
	 */

	public Document drop(Resource resource) throws Exception {
		Document doc = XMLUtility.newDocument();
		Context ctx = new Context(new URI(resource.getURI()), xmlns, resource.getModel(),schemaMap,this);
		
		if (ctx.toXML(doc, resource)) return doc;

		GlozeURL.logger.warn("drop: no schema mapping for resource: "+resource);
		StmtIterator si = resource.listProperties();
		if (!si.hasNext()) 
			GlozeURL.logger.warn("because no properties, is gloze.uri correctly defined?");
		else while (si.hasNext()) {
			Property p = si.nextStatement().getPredicate();
			// skip eg. RDF:type
			if (p.getNameSpace().equals(RDF.getURI())) continue;
			GlozeURL.logger.warn("because no matching property: "+p.getURI());
		}
		// try no-schema mapping
		GlozeURL.logger.info("using no-schema mapping");
		XMLBean.noSchemaToXML(doc,resource,ctx);
		return doc;
	}
	
	/** Gloze main program. The main input parameter is XML to lift or RDF to drop.
	 * This is followed by an (optional) sequence of targetNamespace schemaLocation pairs, 
	 * followed by a single (optional) no-namespace schema location.
	 */

	public static void main(String args[]) {
		GlozeURL gloze = new GlozeURL();
		try {
			if (args.length==0) help();
			else {
				URL source = null;
		
				// the input comprises schema target/schema-location pairs
				for (int i = 1; i < args.length; i++) {
					// a targetNamespace/schemaLocation pair (or noNamespaceSchemaLocation)
					String x = args[i];
					if (x.endsWith(".xsd") || XMLBean.isValidURI(x)) {
						String location;
						URI ns = null;
						if (++i < args.length) {
							ns = new URI(x);
							location = args[i];
						}
						else location  = x;
						
						URL url = XMLBean.isValidURI(location) ? new URL(location)
									: new File(location).toURI().toURL();
						
						GlozeURL.initSchemaLocation(ns,url,gloze.xmlns,gloze.schemaMap);
					}
				}
	
				// the first arg is the source
				String givenSource = args[0];				
				source = XMLBean.isValidURI(givenSource)
						? new  URL(givenSource)
						: new File(givenSource).toURI().toURL();
				File sourceFile = new File(givenSource);
						
				if (givenSource.endsWith(".rdf")
						|| givenSource.toLowerCase().endsWith(".n3")
						|| givenSource.toLowerCase().endsWith(".ttl")
						|| givenSource.toLowerCase().endsWith(".nt")
					) {
					gloze.rdf_to_xml(source,null);

				} else if (givenSource.endsWith(".xsd")) { 
					gloze.xsd_to_owl(source, gloze.base);

				} else if (sourceFile.isDirectory()) {
					// iterate over xml, and xsd files in the directory
					File[] f = sourceFile.listFiles();
					for (int i = 0; i < f.length; i++) {
						gloze = new GlozeURL();
						// distinguish each f[i] by appending the name to the base
						String name = f[i].getName();
						if (name.endsWith(".xml")) {
							if ("true".equals(System.getProperty("gloze.verbose"))) logger.info("reading "+name);
							gloze.xml_to_rdf( f[i].toURI().toURL(), name);
						}
						else if (name.endsWith(".xsd")) {
							if ("true".equals(System.getProperty("gloze.verbose"))) logger.info("reading "+name);
							gloze.xsd_to_owl(f[i], gloze.base);
						}
					}
				} 
				// else assume this is an xml file to be lifted (use base as-is)
				else gloze.xml_to_rdf(source,null);
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}
	
	/*! \page gettingStarted Getting Started
	 Gloze may be invoked from the command line using one of the following incantations:
	 
		-# <tt>java [-options] com.hp.gloze.Gloze xmlfile (namespaceURI schemaURL)* [nonamespaceschemaURL]</tt>
		-# <tt>java [-options] com.hp.gloze.Gloze rdffile (namespaceURI schemaURL)* [nonamespaceschemaURL]</tt>
		-# <tt>java [-options] com.hp.gloze.Gloze xsdfile (namespaceURI schemaURL)*</tt>
		-# <tt>java [-options] com.hp.gloze.Gloze directory</tt>
		
		In all cases the java classpath must include the Jena libraries (typically under JENA_HOME/lib), gloze.jar, and of course the java runtime.
		
		The first option takes an \c xmlfile and maps it to RDF. To produce a good mapping, Gloze requires the XML schema
		that describes the XML document. The schema can be referenced explicitly from within the XML using the XML Schema Instance (XSI)
		schemaLocation or noNamespaceSchemaLocation attributes on the document element. Any schema not already associated with the
		instance in this way can be added to the command line prefixed by their namespace URI. The final schema may be (optionally) a no-namespace schema
		in which case no namespace need be supplied. The order of the schema is significant; schema are loaded left to right, so later schema may depend on earlier schema
		but not vice-versa.
		
		The second form takes an \c rdffile and maps it to XML. The RDF will have no XML schema associated with it so the schema must be
		explicitly supplied as above. Note that in either case, if the XML schema is not supplied, Gloze will make a best attempt to perform
		a schema-less mapping.
		
		The third form takes an XML schema \c xsdfile and maps it to OWL, the Web Ontology Language. Any included, imported or redefined schema
		are recursively mapped.
		
		The final form allows you to lift the contents of an entire directory at once. This may contain both XML and xml schema documents.
		
		By default, the output is written to the console. By supplying a target file or directory (see options below), the output can be saved.

		\section options Options
		
		<tt>-Dgloze.attribute=SYMBOL</tt>
		
		All attributes, elements and types are assigned a URI combining their local name and the target (or default) namespace.
		However, XML schema also states that attributes, elements and types define separate symbol spaces such that if their
		names were the same they would not become confused. While it is good practice to assign unique names to attributes,
		elements and types, this may be unavoidable when using an existing schema. If this occurs Gloze will warn you of the \e potential URI clash, 
		and the remedy is to insert a special symbolic prefix ahead of attribute or element names to disambiguate them. 
		This option defines a symbolic prefix for attributes, which by default is empty. A recommended attribute prefix is the '\@' character.

		<tt>-Dgloze.base=URI</tt>
				
		The base URI defines the root of the XML document which by default is the URL of the XML source document.
		When mapping XML to RDF, the base is the URI of a resource that forms the root of an RDF 'tree'. 
		When mapping from RDF to XML, there must be a resource with this base URI in the RDF model.
		The base is also required to expand relative URIs appearing as QNames in the XML, and for XML IDs which represent fragment identifiers relative to this base.
		This option may be used to supply a different base URI; typically an improvement over the 'file:' scheme of 
		most input files. In either case, an explicit xml:base declaration in the document element will take precedence.
		
		When lifting a whole directory in one batch, the bases can be differentiated by supplying a base terminated by a stroke '/'; gloze appends the relevant filename. This will be *.xml for a lifted XML file,
		or *.owl for a lifted schema.
		
		<tt>-Dgloze.class=subClassOf|intersectionOf</tt>
		
		OWL classes may be defined either as sub-classes or intersections of other classes.
		The intersection style is stronger, in that \e any individual consistent with the class description is a member by definition.
		This style is required for reasoning about schema extensions, but is much more expensive to compute.
		The default for this option is 'subClassOf'. In this case, extensions are not mapped to sub-class relationships (restrictions are unaffected). 

		<tt>-Dgloze.closed=true|false</tt>
		
		Where an XML schema uses a 'russian doll' style with each type embedded in the definition of its parent element, and so on,
		there are no global, named complex types to map into classes. Furthermore, because elements define their types locally, 
		different occurrences of the element may have different types. It is not even correct to take the union of these different types,
		as the schema may be included in another, where the same element is re-used with additional types. One solution to this problem,
		which may not be valid in all cases, is to take the closure over the globally defined attributes and elements, using this as their definition.
		Other local uses of these attributes and elements must be consistent with their global definition.
		This option is true by default, but may be disabled if it results in an invalid OWL mapping.

		<tt>-Dgloze.element=SYMBOL</tt>	
		
		Just as attributes may be assigned a prefix to distinguish their symbol space, elements may also be assigned a symbolic prefix.
		The default is empty. Recommended values include '~'. Note that types may not be assigned a prefix.

		<tt>-Dgloze.fixed=true|false</tt>
		
		Add fixed values when dropping into XML. Fixed values must either be undefined or must match the fixed value
		declared in the XML schema. When lifting into RDF, the fixed value is always added.
		
		<tt>-Dgloze.lang=N3|RDF/XML|RDF/XML-ABBREV</tt>
		
		This option defines the RDF output format, the default is 'RDF/XML-ABBREV' which is about as pretty as it gets while still using XML.
		This option applies to both lifted RDF and to OWL.
		Many people prefer the sleek simplicity of N3, though user beware the lack of xml:base in the generation of ontologies in N3.

		<tt>-Dgloze.order=no|seq</tt>
		
		An XML tree is ordered in that the lexical ordering of children is significant. An RDF model is a graph, so a naive mapping will lose this ordering.
		Gloze can record this additional ordering information by adding the reified statements to an RDF sequence. 
		Gloze will automatically avoid adding this overhead if the ordering can be reconstructed unambiguously from the schema.
		However, even where the ordering is ambiguous, it may not matter at an application level. In this case,
		sequencing can be globally disabled.
		
		<tt>-Dgloze.overwrite=true|false</tt>
		
		When true, (the default) Gloze will overwrite existing output files which is required if the source has changed. 
		However, if it is necessary to interrupt a long run with multiple nested inclusions and imports, the user may opt to recycle the earlier output.
		In this case Gloze can be more or less restarted where it was interrupted.

		<tt>-Dgloze.report=true|false</tt>
		
		When working with large schema, it is often hard to find where a particular attribute, element or type is defined. By opting to generate a report, 
		Gloze will list all the generated attribute, element, and type URIs and their sources.

		<tt>-Dgloze.roundtrip=true|false</tt>
		
		Used mainly for testing, it is sometimes useful to lift an XML document into RDF and then immediately drop this back into XML so the original and final versions
		can be compared. By default this is disabled.

		<tt>-Dgloze.schemaLocation=URI|dir</tt>
		
		This option allows the schemaLocation to be inserted into the dropped XML.

		<tt>-Dgloze.space=default|preserve</tt>
		
		Whitespace processing, modelled after xml:space;
		using this parameter is equivalent to setting xml:space on the document element.
		It has two settings, 'default' and 'preserve', with the latter preserving whitespace.
		The default setting is 'default', performing whitespace collapse and removal.
		It is not possible to \em relax whitespace processing of datatypes \em other than xs:string and xs:normalizedString
		which are already fully whitespace replaced and collapsed. The space setting will therefore only effect
		string types and other mixed text content.
		
		<tt>-Dgloze.target=file</tt>
		
		By default output is written to the console. By defining a target file or directory the output will be saved.

		<tt>-Dgloze.trace=true|false</tt>
		
		This option is only useful for low-level debugging of inference. When enabled it produces a trace of rule firings.

		<tt>-Dgloze.verbose=true|false</tt>
		
		When enabled, information (disabled by default) and warnings are logged to the console.

		<tt>-Dgloze.xmlns=URI</tt>
		
		Unqualified references to schema components are resolved against the default xml namespace.
		Adding this option is equivalent to defining an xmlns on the document element of the schema.
		It also provides a substitute target namespace for unqualified components, or more generally for no-namespace schema. 
		The default value for this is the URL of the schema.
		
		\section examples Examples
		
		The following examples demonstrate a number of Gloze invocations using different combinations
		of these options. All examples assume the classpath has been initialised to point to the java runtime;
		gloze.jar; and the libraries in JENA-HOME/lib.
		
		This example lifts 'example.xml' into RDF using a schema 'schema.xsd' with base "http://example.org/". 
		The output is written to 'example.rdf' and the target is the current directory.
		The base URI of the XML is "http://example.org/example.xml" and this named resource is the root of the RDF mapping. 

		<tt>java -Dgloze.target=. -Dgloze.base=http://example.org/example.xml com.hp.gloze.Gloze example.xml http://example.org/ schema.xsd</tt>
		
		This example lifts example.xml using a pair of schema with namespaces.
		No base is provided as we assume the instance defines its own xml:base.
		No target is defined, so the output is written to the console.
		
		<tt>java com.hp.gloze.Gloze example.xml http://www.example.org/ schema1.xsd http://www.example.com/ schema2.xsd</tt>

		This example lifts example.xml using a no-namespace schema.
		Additionally, the resulting RDF is ordered. 
		
		<tt>java -Dgloze.order=seq  -Dgloze.xmlns=http://example.org/ -Dgloze.base=http://example.org/example.xml com.hp.gloze.Gloze example.xml schema.xsd</tt>

		The example lifts 'example.xml', but supplies no schema because this is defined in the XML instance using xsi:schemaLocation.
		The target language is N3, so the output file is 'example.n3' in the current directory.
		Finally, the RDF is round-tripped back into XML so it may be compared with the XML input.
		
		<tt>java -Dgloze.target=. -Dgloze.roundtrip=true -Dgloze.base=http://example.org/example.xml -Dgloze.lang=N3 com.hp.gloze.Gloze example.xml</tt>

		The example below drops example.rdf into XML, using "http://example.org/example.xml" as the root resource, and the schema 'schema.xsd'.

		<tt>java -Dgloze.base=http://example.org/example.xml com.hp.gloze.Gloze example.rdf http://www.example.org/ schema.xsd</tt>
		
		This example lifts the schema 'schema.xsd' into OWL, using xml:base "http://example.org/schema.xsd".
		The output is written to 'schema.owl' in the current target directory.

		<tt>java -Dgloze.target=. -Dgloze.base=http://example.org/schema.xsd com.hp.gloze.Gloze schema.xsd</tt>

		This example lifts the same schema into OWL but uses N3 as the target language.
		The schema may import or include other schema which are also lifted into OWL.
		The output is written to the console.
		
		<tt>java -Dgloze.lang=N3 com.hp.gloze.Gloze schema.xsd</tt>
		
		The following example lifts a pair of schema into OWL. the first 'schema1.xsd' imports 'schema2.xsd'
		but the schemalocation is missing, hence the need to supply it as a user defined parameter.
		
		<tt>java -Dgloze.target=. -Dgloze.base=http://example.org/schema1.owl com.hp.gloze.Gloze schema1.xsd http://www.example.com/ schema2.xsd</tt>

		The following invocation is used to generate all the examples used in this documentation. They are contained in a single \em examples directory.
		Note that few of the examples contain an explicit reference to their schema. In the absence of a schema reference on the command line or in the XML instance,
		Gloze looks for a schema in the same directory with the same name (with an 'xsd' extsnsion).
		Because the supplied base is terminated by a stroke '/', the relevant file name is appended for each lifted file.

		<tt>java -Dgloze.xmlns=http://example.org/def/ -Dgloze.base=http://example.org/ -Dgloze.target=examples -Dgloze.lang=N3 -Dgloze.verbose=true com.hp.gloze.Gloze examples</tt>

		The following invocation lifts example.xml using a combination of schema obtained via a web proxy, and one locally defined schema (xml.xsd - without a DTD).
		A schemaLocation would be defined within example.xml
		
		<tt>java -Dgloze.base=http://example.org/ -Dhttp.proxyHost=myproxy.com -Dhttp.proxyPort=8080 com.hp.gloze.Gloze example.xml http://www.w3.org/XML/1998/namespace xml.xsd</tt>
	 */

	private static void help() {
		System.out.println("Usage:	java [-options] com.hp.gloze.Gloze xmlfile (namespaceURI schemaURL)* [nonamespaceschemaURL]");
		System.out.println("or	java [-options] com.hp.gloze.Gloze rdffile (namespaceURI schemaURL)* [nonamespaceschemaURL]");
		System.out.println("or	java [-options] com.hp.gloze.Gloze xsdfile");
		System.out.println();
		System.out.println("options:");
		System.out.println("-Dgloze.order=no|seq			disable/enables ordering (default=no)");
		System.out.println("-Dgloze.space=default|preserve		whitespace handling (default=default)");
		System.out.println("-Dgloze.base=URI			base URI (default=target or source URL)");
		System.out.println("-Dgloze.xmlns=URI			default namespace for unqualified components (default=schema URL)");
		System.out.println("-Dgloze.element=SYMBOL			symbolic prefix for elements (default='')");
		System.out.println("-Dgloze.fixed=true|false		add fixed attributes in drop (default=false)");
		System.out.println("-Dgloze.attribute=SYMBOL		symbolic prefix for attributes (default='')");
		System.out.println("-Dgloze.lang=N3|RDF/XML|RDF/XML-ABBREV	RDF format (default='RDF/XML-ABBREV')");
		System.out.println("-Dgloze.target=file			output directory or file (default=none)");
		System.out.println("-Dgloze.verbose=true|false		direct output to console (default=false)");
		System.out.println("-Dgloze.roundtrip=true|false		combined lift/drop for testing (default=false)");
		System.out.println("-Dgloze.schemaLocation=URI|dir		schema location attribution (default=none)");
		System.out.println("-Dgloze.closed=true|false		close global definitions for OWL mapping (default=true)");
		System.out.println("-Dgloze.class=subClassOf|intersectionOf	class definition style (default=subClassOf)");
		System.out.println("-Dgloze.report=true|false		report defined URIs (default=false)");
		System.out.println("-Dgloze.trace=true|false		trace rules (default=false)");
		System.out.println("-Dgloze.overwrite=true|false		overwrite existing owl files (default=true)");
	}

}
