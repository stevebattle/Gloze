package com.hp.gloze.www_eclipse_org_emf_2002_Ecore;

import org.w3c.dom.Node;

import com.hp.gloze.Context;
import com.hp.gloze.Restrictions;
import com.hp.gloze.XMLBean;
import com.hp.gloze.www_w3_org_2001_XMLSchema.schema;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.vocabulary.RDF;

public class Ecore {
	public static final String URI="http://www.eclipse.org/emf/2002/Ecore";
	public static final String REFERENCE=URI+"#reference";
	
	String instanceClass, name, documentRoot, _package, nsPrefix ;
	String reference, opposite, mixed, featureMap, ignore;
	
	public String get_package() {
		return _package;
	}
	public void set_package(String _package) {
		this._package = _package;
	}
	public String getDocumentRoot() {
		return documentRoot;
	}
	public void setDocumentRoot(String documentRoot) {
		this.documentRoot = documentRoot;
	}
	public String getFeatureMap() {
		return featureMap;
	}
	public void setFeatureMap(String featureMap) {
		this.featureMap = featureMap;
	}
	public String getIgnore() {
		return ignore;
	}
	public void setIgnore(String ignore) {
		this.ignore = ignore;
	}
	public String getInstanceClass() {
		return instanceClass;
	}
	public void setInstanceClass(String instanceClass) {
		this.instanceClass = instanceClass;
	}
	public String getMixed() {
		return mixed;
	}
	public void setMixed(String mixed) {
		this.mixed = mixed;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNsPrefix() {
		return nsPrefix;
	}
	public void setNsPrefix(String nsPrefix) {
		this.nsPrefix = nsPrefix;
	}
	public String getOpposite() {
		return opposite;
	}
	public void setOpposite(String opposite) {
		this.opposite = opposite;
	}
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}
	
	public void toOWL(OntModel ont, String uri, Restrictions rest, Node node, String type, Context ctx) {
		if (type==null) return;
		String ref = XMLBean.expandQName(ctx.getDefaultNS(),getReference(),node,ont);
		if (ref==null) return;
		
		if (type.equals(schema.XSD_URI+"#IDREF")) {
			rest.addRange(ont,uri,ref);
		}
		else if (type.equals(schema.XSD_URI+"#IDREFS")) {
			rest.addRange(ont,uri,RDF.List.getURI());
		}
	}
	
	
}
