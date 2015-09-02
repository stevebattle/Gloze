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

package com.hp.gloze;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.w3c.dom.Document;

import com.hp.gloze.www_w3_org_2001_XMLSchema.attribute;
import com.hp.gloze.www_w3_org_2001_XMLSchema.attributeGroup;
import com.hp.gloze.www_w3_org_2001_XMLSchema.complexType;
import com.hp.gloze.www_w3_org_2001_XMLSchema.element;
import com.hp.gloze.www_w3_org_2001_XMLSchema.group;
import com.hp.gloze.www_w3_org_2001_XMLSchema.schema;
import com.hp.gloze.www_w3_org_2001_XMLSchema.simpleType;
import com.hp.gloze.www_w3_org_2001_XMLSchema.union.MemberType;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

public class Context {
	
	static public final String DEFAULT_XMLNS = "http://example.org";
	
	// schema
	private Map<URL,schema> schemaMap;
		
	// globals
	private Map<String,element> globalElements = new HashMap<String,element>();
	private Map<String,attribute> globalAttributes = new HashMap<String,attribute>();
	private Map<String,simpleType> globalSimpleTypes = new HashMap<String,simpleType>();
	private Map<String,complexType> globalComplexTypes = new HashMap<String,complexType>();
	private Map<String,group> globalGroups = new HashMap<String,group>();
	private Map<String,attributeGroup> globalAttributeGroups = new HashMap<String,attributeGroup>();
	private Map<element, String> substitutionGroups = new HashMap<element, String>();
	
	// resolved global maps
	private Map<XMLBean,XMLBean> baseMap = new HashMap<XMLBean,XMLBean>();
	private Map<XMLBean,XMLBean> typeMap = new HashMap<XMLBean,XMLBean>();
	private Map<XMLBean,XMLBean> refMap = new HashMap<XMLBean,XMLBean>();
	private HashMap<XMLBean, Vector<MemberType>> memberTypes = new HashMap<XMLBean,Vector<MemberType>>();
	
	// cached ontologies associated with schema
	private Map<schema,OntModel> ontModel = new HashMap<schema, OntModel>();

	// the model also provides a prefix mapping context
	private Model model;
	// for performing inference over the model
	private InfModel infModel;
	
	private String defaultNS=DEFAULT_XMLNS, elementSymbol, attributeSymbol, lang="RDF/XML-ABBREV", _class="subClassOf";
	private URI base;
	
	// The rule for XML processors is that in the absence of a declaration that identifies the content model of an element, all white space is significant.

	private boolean verbose=false, sequenced=false, preserved=true, closed=true, report=false, silent=false, overwrite=true, fixed=false;
	
	private Reasoner reasoner;
	private Map<String,Resource> ontClass = new HashMap<String, Resource>();

	private int _nsPrefixIndex = 1;	

	private void copyMaps(Context ctx) {
		schemaMap = ctx.schemaMap;
		
		globalElements = ctx.globalElements;
		globalAttributes = ctx.globalAttributes;
		globalSimpleTypes = ctx.globalSimpleTypes;
		globalComplexTypes = ctx.globalComplexTypes;
		globalGroups = ctx.globalGroups;
		globalAttributeGroups = ctx.globalAttributeGroups;
		substitutionGroups = ctx.substitutionGroups;
		
		// the baseMap identifies the bases of restrictions/extensions
		// this is independent of renaming due to redefinition
		baseMap = ctx.baseMap;
		typeMap = ctx.typeMap;
		refMap = ctx.refMap;
		memberTypes = ctx.memberTypes;
		
		// reuse caches
		ontModel = ctx.ontModel;
		ontClass = ctx.ontClass;
	}
	
	public Context() {	
	}
	
	public Context(URI base, Context ctx) {
		copyParameters(ctx);
		copyMaps(ctx);
		setBase(base);
	}
	
	public Context copy()  {
		Context c = new Context();
		c.copyParameters(this);
		c.copyMaps(this);
		return c;
	}

	public Context(Model mod, Map<URL,schema> schemaMap, Gloze gloze) {
		Set<URL> done = new HashSet<URL>();
		model = mod;
		initialise(gloze);
		// gather global schema components
		this.schemaMap = schemaMap;
		for (URL url: schemaMap.keySet()) {
			schema xs = schemaMap.get(url);
			if (!done.contains(xs.get_url())) xs.gatherGlobals(model, this,done);
		}
	}

	public Context(URI base, String xmlns, Model mod, schema xs, Gloze gloze) {
		setBase(base);
		setDefaultNS(xmlns);
		model = mod;
		initialise(gloze);
		// gather global schema components
		xs.gatherGlobals(model,this,new HashSet<URL>());
	}

	public Context(URI base, String xmlns, Model mod, Map<URL,schema> schemaMap, Gloze gloze) {
		setBase(base);
		setDefaultNS(xmlns);
		model = mod;
		Set<URL> done = new HashSet<URL>();
		initialise(gloze);
		// gather global schema components		
		this.schemaMap = schemaMap;
		for (URL url: schemaMap.keySet()) {
			schema xs = schemaMap.get(url);
			if (!done.contains(xs.get_url())) xs.gatherGlobals(model,this,done);
		}
	}
	
	private void initialise(Gloze gloze) {
		if (gloze.verbose!=null) setVerbose(gloze.verbose.equals("true"));
		if (gloze.verbose==null && gloze.target==null) setVerbose(true);
		if (gloze.order!=null) setSequenced(gloze.order.equals("seq"));
		if (gloze.space!=null) setPreserved(gloze.space.equals("preserve"));
		if (gloze.closed!=null) setClosed(gloze.closed.equals("true"));
		if (gloze.fixed!=null) setFixed(gloze.fixed.equals("true"));		
		setElementSymbol(gloze._element);
		setAttributeSymbol(gloze._attribute);
		setLang(gloze.lang);
		set_class(gloze._class);
		setSilent(gloze.silent);
		if (gloze.overwrite!=null) setOverwrite(gloze.overwrite.equals("true"));
		setReport(gloze.report!=null && gloze.report.equals("true"));
	}
	
	private void copyParameters(Context ctx) {
		setDefaultNS(ctx.getDefaultNS());
		setAttributeSymbol(ctx.getAttributeSymbol());
		setElementSymbol(ctx.getElementSymbol());
		setVerbose(ctx.isVerbose());
		setSequenced(ctx.isSequenced());
		setPreserved(ctx.isPreserved());
		setClosed(ctx.isClosed());
		setModel(ctx.getModel());
		setReasoner(ctx.getReasoner());
		setLang(ctx.getLang());
		set_class(ctx.get_class());
		setSilent(ctx.isSilent());
		setOverwrite(ctx.isOverwrite());
	}

	public boolean toXML(Document doc, Resource rez) throws Exception {
		StmtIterator i = rez.listProperties();
		while (i.hasNext()) {
			Statement s = i.nextStatement();
			Property p = s.getPredicate();
			element e = getElement(p.getURI());
			if (e!=null && e.toXML(doc, s.getObject(),this)) return true;		
		}
		return false;
	}
	
	public Model getModel() {
		return model;
	}
		
	public String createNSPrefix() {
		return "ns"+_nsPrefixIndex++;	
	}

	public void putSubstitution(element elem, String uri) {
		substitutionGroups.put(elem, uri);
	}

	public String getSubstitution(element elem) {
		return (String) substitutionGroups.get(elem);
	}
	
	public Vector<element> getSubstitutionFor(Model mod, element elem) {
		String uri = elem.createURI(mod,this);
		if (substitutionGroups.containsValue(uri)) {
			Vector<element> v = new Vector<element>();
			for (element e: substitutionGroups.keySet()) {
				if (getSubstitution(e).equals(uri)) v.add(e);
			}
			return v;
		}
		return null;
	}
	
	public element substitute(String uri, element elem, Model model) {
		String elemURI = elem.createURI(model,this);
		if (uri.equals(elemURI)) return elem;
		// is there a substitute for this element?
		if (substitutionGroups.containsValue(elemURI)) {
			for (Iterator i = substitutionGroups.keySet().iterator(); i.hasNext();) {
				element k = (element) i.next();
				if (substitutionGroups.get(k).equals(elemURI)) {
					element e;
					// recursively check the substitution tree
					if ((e = substitute(uri,k,model)) != null) return e;
				}
			}
		}
		return null;
	}
	
	public void putComplexType(String uri, complexType elem) {
		globalComplexTypes.put(uri,elem);
	}
	
	public complexType getComplexType(String uri) {
		return (complexType) globalComplexTypes.get(uri);
	}

	public void putSimpleType(String uri, simpleType elem) {
		globalSimpleTypes.put(uri,elem);
	}
	
	public simpleType getSimpleType(String uri) {
		return (simpleType) globalSimpleTypes.get(uri);
	}
	
	public void putElement(String uri, element elem) {
		globalElements.put(uri,elem);
	}
	
	public element getElement(String uri) {
		return (element) globalElements.get(uri);
	}
	
	public boolean isGlobalElement(element elem) {
		return globalElements.containsValue(elem);
	}

	public boolean isGlobalAttribute(attribute attr) {
		return globalAttributes.containsValue(attr);
	}

	public void putAttribute(String uri, attribute attr) {
		globalAttributes.put(uri,attr);
	}
	
	public attribute getAttribute(String uri) {
		return (attribute) globalAttributes.get(uri);
	}
	
	public void putGroup(String uri, group grp) {
		globalGroups.put(uri,grp);
	}
	
	public group getGroup(String uri) {
		return (group) globalGroups.get(uri);
	}
	
	public void putAttributeGroup(String uri, attributeGroup attGrp) {
		globalAttributeGroups.put(uri,attGrp);
	}
	
	public attributeGroup getAttributeGroup(String uri) {
		return (attributeGroup) globalAttributeGroups.get(uri);
	}
	
	public void putBase(XMLBean bean, XMLBean base) {
		if (base==null) return;
		this.baseMap.put(bean,base);
	}
	
	public XMLBean getBase(XMLBean bean) {
		return (XMLBean) baseMap.get(bean);
	}

	public void putType(XMLBean bean, XMLBean type) {
		if (type==null) return;
		this.typeMap.put(bean,type);
	}
	
	public XMLBean getType(XMLBean bean) {
		return (XMLBean) typeMap.get(bean);
	}

	public void putRef(XMLBean bean, XMLBean ref) {
		if (ref==null) return;
		this.refMap.put(bean,ref);
	}
	
	public XMLBean getRef(XMLBean bean) {
		return (XMLBean) refMap.get(bean);
	}

	public void putMemberTypes(XMLBean bean, Vector<MemberType> types) {
		if (types==null) return;
		memberTypes.put(bean,types);
	}
	
	public Vector<MemberType> getMemberTypes(XMLBean bean) {
		return (Vector<MemberType>) memberTypes.get(bean);
	}

	public String getDefaultNS() {
		return defaultNS;
	}

	public void setDefaultNS(String namespace) {
		if (namespace!=null) this.defaultNS = namespace;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public boolean isSequenced() {
		return sequenced;
	}

	public void setSequenced(boolean sequence) {
		this.sequenced = sequence;
	}

	public boolean isPreserved() {
		return preserved;
	}

	public void setPreserved(boolean trimmed) {
		this.preserved = trimmed;
	}

	public URI getBaseMap() {
		return base;
	}

	public void setBase(URI base) {
		if (base!=null) base = base.normalize();
		this.base = base;
	}

	public String getElementSymbol() {
		return elementSymbol;
	}

	public void setElementSymbol(String symbol) {
		if (symbol!=null) elementSymbol = symbol;
	}
	
	public String elementName(String localName) {
		return elementSymbol==null?localName:elementSymbol+localName;
	}

	public String getAttributeSymbol() {
		return attributeSymbol;
	}

	public void setAttributeSymbol(String symbol) {
		if (symbol!=null) attributeSymbol = symbol;
	}
	
	public String attributeName(String localName) {
		return attributeSymbol==null?localName:attributeSymbol+localName;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		if (lang!=null) this.lang = lang;
	}

	public Reasoner getReasoner() {
		return reasoner;
	}

	public void setReasoner(Reasoner reasoner) {
		this.reasoner = reasoner;
		infModel = ModelFactory.createInfModel(reasoner,this.model);	}
	
	/* enable incremental addition of inferences */
	
	public boolean checkConsistency(Resource cls, Model model) {
		Model temp = ModelFactory.createDefaultModel();
		temp.add(this.model);
		if (infModel==null) infModel = ModelFactory.createInfModel(reasoner,this.model);
		
		// merge the models eg. optimization of infModel.add(model);
		// if the class is anonymous add it separately
		Map<RDFNode,RDFNode> visited = new HashMap<RDFNode,RDFNode>();
		if (cls!=null && cls.isAnon()) mergeDescription(infModel.createResource(), cls, false, visited);
		mergeModel(model,false,visited);
		
		boolean valid = infModel.validate().isValid();
		if (!valid) {
			// restore the old state
			this.model.removeAll().add(temp);
			infModel.rebind();
		}
		else {
			// mark known classes as OK
			Resource ok = infModel.getResource(Gloze.OK);
			for (ResIterator ri = this.model.listSubjectsWithProperty(RDF.type, OWL.Class); ri.hasNext(); )
				ri.nextResource().addProperty(RDF.type, ok);
		}
		return valid;
	}
		
	public void mergeModel(Model mod, boolean flagOK, Map<RDFNode,RDFNode> visited) {
		for (ResIterator ri = mod.listSubjects(); ri.hasNext(); ) {
			Resource r = ri.nextResource(), ir;
			if (!r.isAnon()) ir = infModel.getResource(r.getURI());
			else continue;
			
			// does ir have any defining properties (other than its type)
			boolean defined = false;
			for (StmtIterator si = ir.listProperties(); !defined && si.hasNext(); ) 
				defined |= !si.nextStatement().getPredicate().equals(RDF.type);
			if (defined) continue;
			// add definition to the infModel
			mergeDescription(ir, r, flagOK, visited);
		}
	}
	
	private void mergeDescription(Resource ir, Resource r, boolean flagOK, Map<RDFNode,RDFNode> visited) {
		for (StmtIterator si = r.listProperties(); si.hasNext(); ) {
			Statement stmt = si.nextStatement();
			Property p = stmt.getPredicate();
			RDFNode o = stmt.getObject();
			
			if (flagOK && r.hasProperty(RDF.type, OWL.Class))
				ir.addProperty(RDF.type, infModel.getResource(Gloze.OK));
			
			if (o.isLiteral()) 
				ir.addProperty(p,o);
			
			else if (!o.isAnon())
				ir.addProperty(p,infModel.createResource(((Resource)o).getURI()));
			
			else if (o.isAnon() && visited.containsKey(o))
				ir.addProperty(p,visited.get(o));

			else { // recursively merge anonymous objects
				Resource a = infModel.createResource();
				visited.put(o,a);
				ir.addProperty(p,a);
				mergeDescription(a, (Resource)o, false, visited);
			}
		}
	}
	
	public void assertOK(Resource rez) {
		if (rez!=null && !rez.isAnon()) {
			String uri = rez.getURI();
			infModel.getResource(uri).addProperty(RDF.type, infModel.getResource(Gloze.OK));
		}
	}
	
	public Resource getOntClass(String uri) {
		return ontClass.get(uri);
	}
	
	public void putOntClass(String uri, Resource c) {
		if (uri!=null && c!=null) ontClass.put(uri,c);
	}

	public boolean isClosed() {
		return closed;
	}

	void setClosed(boolean closed) {
		this.closed = closed;
	}

	public boolean isReport() {
		return report;
	}

	void setReport(boolean report) {
		this.report = report;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public String get_class() {
		return _class;
	}

	void set_class(String _class) {
		if (_class!=null) this._class = _class;
	}

	public boolean isSilent() {
		return silent;
	}

	void setSilent(boolean silent) {
		this.silent = silent;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public OntModel getOntModel(schema xs) {
		return ontModel.get(xs);
	}

	public void setOntModel(schema xs, OntModel ontModel) {
		this.ontModel.put(xs,ontModel);
	}

	public boolean isFixed() {
		return fixed;
	}

	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}

	public Map<URL, schema> getSchemaMap() {
		return schemaMap;
	}

}
