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
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.hp.gloze.Content;
import com.hp.gloze.Context;
import com.hp.gloze.Gloze;
import com.hp.gloze.Restrictions;
import com.hp.gloze.XMLBean;
import com.hp.gloze.www_eclipse_org_emf_2002_Ecore.Ecore;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.impl.UnionClassImpl;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;

/*! \page attribute attribute
 
	Attributes map to RDF properties. An instance of an attribute maps to an RDF statement. 
 	A qualified attribute is defined in the target namespace of the schema. The target namespace is the
 	namespace of the corresponding RDF property.
 	
 	e.g. The following schema declares a global and therefore qualified attribute 'foo'  defined in the
 	target namespace.
 	
 	\include attribute1.xsd
 	
 	By default, we take the closure over global attributes, assuming that \em all uses of the property conform to
 	this global declaration. This means that 'foo' globally ranges over rdfs:Literal (the datatype corresponding to xs:anySimpleType).
 	If this assumption is false, attribute closure may be disabled (closed=false), and global ranges are not asserted.
 	
 	\include attribute1.owl
 	
 	Like elements, if the target namespace ends with an alpha-numeric a fragment separator '#' is introduced.
 	
 	Unqualified attributes are not defined in the target namespace. Unqualified attributes occur if no target namespace
 	is defined, or where the attribute is defined locally and its form is unqualified.
 	The example below defines the attribute 'foo' locally within an attributeGroup; it's form is unqualified by default.
 	
 	\include attribute2.xsd
 	
 	All we can say about the attribute is that it is a property, for the same name may be re-used where
 	its range is a different datatype or even an object type. Closure doesn't apply to local attributes.
 	
 	The following OWL was produced with lang=N3. The unqualified attribute is defined in the user-supplied default namespace,
 	with xmlns=http://example.org/def/ .
 	
 	\include attribute2.owl
 	
 	In xml schema, attributes have their own symbol space, distinct from other components such as elements and types.
 	If there are overlaps between these symbol spaces, it is advisable to introduce a symbolic prefix to keep them distinct.
 	
 	e.g. the attribute named 'foo' and type named 'foo' in the target namespace http://example.org/ will clash.
 	Introducing a symbolic prefix '@' (at the command line) for attributes resolves the clash giving us an RDF property name
 	http://example.org/@foo.
 	
 	The OWL mapping below was generated from the first schema above, but with attribute=@
 	
 	\include attribute3.owl

	\section attributeChildren Child components
	
	- \ref simpleType
	- \ref annotation
 
    \page attributeAnySimpleType Example: xs:anySimpleType
  
 	This example shows how an untyped attribute 'bar' has a default type of xs:anySimpleType. All simple types are
 	derived from this, so it occupies a similar place to rdfs:Literal.

	\include attributeAnySimpleType.xsd
	
	An XMl instance of this schema is as follows:
 	
 	\include attributeAnySimpleType.xml
 	 		
	The value 'foobar' is mapped to a literal of type rdfs:Literal.
	
	\include attributeAnySimpleType.n3
	
	\page attributeNotation Example: xs:NOTATION
	
	This example demonstrates the use of notations. These are essentially QNames which must be expanded to give them global scope.
	The only restriction is that any notation should be predefined in the schema. The example includes a base64Binary embedded image 
	which is notated as belonging to a particular predefined mime type. 
	
    \include attributeNOTATION.xml
    
    The xml schema defines the mime options is as follows. 
	The only mime type options available are 'jpeg', 'gif' and 'png'. 

    \include attributeNOTATION.xsd
    
    In the resulting RDF mapping, the xs:NOTATION datatype does not appear, but the expanded QName names the global mime type resource.
   
    \include attributeNOTATION.n3
    
    \section attributeChildren Child components

	- \ref annotation
	- \ref simpleType

- \ref simpleContent
 */

public class attribute extends Content {

	private String _default, fixed, form, name, ref, type, use="optional";
	private simpleType simpleType;
	private String uri;
	private annotation annotation;
	
	// ecore attributes
	private Ecore ecore;
	
	public attribute() throws IntrospectionException {
	}
	
	public attribute(String uri) throws IntrospectionException {
		this.uri = uri;
	}
	
	/** create a URI for a reference to a global attribute **/

	public static String createURI(Attr attr, String tns, Context ctx) {
		if (attr.getNamespaceURI()!=null)
			return concatName(attr.getNamespaceURI(), ctx.getAttributeSymbol(), attr.getLocalName());
		else if (tns!=null)
			return concatName(tns,ctx.getAttributeSymbol(), attr.getNodeName());
		else
			return concatName(ctx.getDefaultNS(), ctx.getAttributeSymbol(),attr.getNodeName());
	}
	
	String createURI(Model model, Context ctx) {
		if (uri != null) return uri;
		schema xs = (schema) this.get_owner();
		
		if (isQualified() && getName()!=null)
			uri = xs.qualifiedName(getName(),ctx.getAttributeSymbol(),model);
		
		// unqualified definitions
		else if (getRef()==null)
			uri = xs.unqualifiedName(getName(),ctx.getAttributeSymbol(),model,ctx);

		// QName references use the xmlns (also see xmlns parameter) as the default
		else uri = expandQName(ctx.getDefaultNS(),ctx.getAttributeSymbol(),getRef(),get_node(),model);
		
		return uri;
	}

	public void resolve(Model model, Context ctx) {
		ctx.putType(this,get_type(ctx));
		super.resolve(model,ctx);
	}
	
	public void reportLocalAttributes(Model model,Set<String> report, Context ctx) {
		if (name!=null && !(get_parent() instanceof schema)) {
			String t = expandQName(ctx.getDefaultNS(),getType(),ctx.getModel());
			if (t!=null && t.equals(schema.XSD_URI+"#ID")) return;
			report.add(createURI(model,ctx));
		}
	}

	private XMLBean get_type(Context ctx) {
		if (simpleType != null) return simpleType;
		XMLBean b = ctx.getType(this);
		if (b!=null) return b;
		if (type!=null) {
			String uri = expandQName(ctx.getDefaultNS(),type,ctx.getModel());
			if (!uri.startsWith(schema.XSD_URI)) {
				b = ctx.getComplexType(uri);
				if (b==null) b = ctx.getSimpleType(uri);
				if (b!=null) ctx.putType(this,b);
				else Gloze.logger.warn("no such type: "+type);
			}
		}
		return b;
	}
	
	/*! \page attributeID Attribute Identity
	 
	 The following example demonstrates the use of xs:ID, xs:IDREF, and xs:IDREFS.
	 The element 'foobar' includes an 'id' attribute that identifies it.
	 The bar element references it via its 'href' or 'hrefs' attributes,
	 of type xs:IDREF or xs:IDREFS, respectively.
	 
	 \include attributeID.xml
	 \include attributeID.xsd
	 
	 Observe that no resources of types xs:ID, xs:IDREF, or xs:IDREFS appear in the mapped RDF.
	 These are all translated into resource URIs, and in the case of xs:IDREFS to an RDF:List of URIs.
	 
	 \include attributeID.n3
	 
	 */
	
	/*! \page lang xml:lang
	 The appearance of an xml:lang attribute states that the element content is
	 expressed in the given language. The same lang value can be set on an RDF literal
	 but not an RDF datatype. Language settings take precedence over datatyping.
	 
	 \include attributeLang.xml
	 
	 This conforms to the following schema. Note that xml:lang is defined in a standard schema
	 obtainable from http://www.w3.org/XML/1998/namespace. The attribute group 'specialAttrs' includes
	 xml:base, xml:lang and xml:space.
	 
	 \include attributeLang.xsd
	 
	 The resulting RDF below includes the literal 'language' in the english language.
	 
	 \include attributeLang.n3
	 
	 */
				
	public void toRDF(Resource subject, Attr attribute, Context ctx)
		throws Exception {
		Model model = ctx.getModel();
		schema xs = (schema) this.get_owner();
		if (attribute==null && getFixed()==null) return;
		
		String value = attribute!=null?attribute.getValue():getFixed();
		if (value.equals("") && _default!=null) value = _default;
		Property prop = model.createProperty(createURI(model,ctx));
		
		// avoid adding xml namespace attributes
		if (attribute!=null && 
				attribute.getNamespaceURI()!=null && 
				attribute.getNamespaceURI().equals(XML)) return;
		
		// the attribute may have been added already in a restriction
		if (subject.hasProperty(prop)) return; 

		// the attribute may be defined locally or globally by ref
		attribute def = getDefinition(model,ctx);
		if (def==null) return; // may be eg. xml:lang
		
		String t = expandQName(ctx.getDefaultNS(),def.getType(),ctx.getModel());
		if (t==null && simpleType==null) t = schema.XSD_URI+"#anySimpleType";
		if (t!=null && t.startsWith(schema.XSD_URI)) {
			// ID is used to name the owning element - don't generate RDF property
			if (t.endsWith("#ID")) return;
			// IDREF to globally named element
			else if (t.endsWith("#IDREF")) {
				Resource rez = model.createResource(addFragment(ctx.getBaseMap(), value).toString());
				subject.addProperty(prop, rez);
			}
			// IDREFS to global entities
			else if (t.endsWith("#IDREFS"))
				subject.addProperty(prop, schema.toRDFList(attribute,value,XSD.IDREF.getURI(),null,ctx));
			else {
				value = schema.processWhitespace(attribute,value,t,ctx);
				xs.toRDF(attribute, subject, prop, value, t, null, null, ctx);
			}
			return;
		}
		else {
			XMLBean type = def.get_type(ctx);
			// simple type?
			if (type instanceof simpleType)
				((simpleType) type).toRDF(subject, prop, attribute, value, null, null, ctx);
			// else add plain literal
			else subject.addProperty(prop, model.createLiteral(schema.processWhitespace(attribute,value,null,ctx)));
		}
	}
	
	/* preliminaries before matching on property name */

	public void toXML(Element e, Resource subject, Set<Statement> pending, Context ctx) {
		String uri = createURI(ctx.getModel(),ctx);
		try {
			// the attribute may be defined locally or globally by ref
			attribute def = getDefinition(ctx.getModel(),ctx);
			
			if (!ctx.isFixed() && getFixed()!=null) return;
			
			String type = def!=null?expandQName(ctx.getDefaultNS(),def.getType(),ctx.getModel()):null;
			
			if (type==null && simpleType==null) type = schema.XSD_URI+"#anySimpleType";
			
			if (ref!=null && ref.equals("xml:lang")) {
				String l = subject.getProperty(RDF.value).getLiteral().getLanguage();
				if (l!=null && !l.equals("")) e.setAttribute("xml:lang", l);				
			}
			if (ref!=null && ref.equals("xml:id")) {
				if (!subject.isAnon()) 
					e.setAttribute("xml:id", subject.getLocalName());				
			}
			else if (type!=null && type.startsWith(schema.XSD_URI)) {
				if (type.endsWith("#ID")) {
					if (!subject.isAnon() && !getUse().equals("prohibited")) {
						// derive attribute from resource URI
						e.setAttribute(def.getName(), new URI(subject.getURI()).getFragment());
					}
					else if (getUse().equals("required"))
						Gloze.logger.warn("missing required ID "+getName());
					return;
				}
			}
	
			// look for attributes that correspond to RDF properties
			Set<Statement> done = new HashSet<Statement>();
			for (Statement stmt: pending) {
				Property p = stmt.getPredicate();
				if (uri.equals(p.getURI())) { // && !done.contains(s)) {
					toXML(e, ctx, def, type, stmt.getObject());
					done.add(stmt);
				}
			}
			pending.removeAll(done);
		} catch (Exception x) {}
	}
	
	public void toXML(Element elem, RDFNode object, Context ctx) {
		try {
			attribute def = getDefinition(ctx.getModel(),ctx);
			String type = def!=null?expandQName(ctx.getDefaultNS(),def.getType(),ctx.getModel()):null;
			toXML(elem, ctx, def, type, object);	
		} catch (Exception e) {
		}
	}

	private void toXML(Element e, Context ctx, attribute def, String type, RDFNode object) 
		throws Exception {
			Document doc = e.getOwnerDocument();
			schema xs = (schema) this.get_owner();
			Attr a;
			if (isQualified()) {
				String ns = xs.getTargetNamespace();
				a = doc.createAttributeNS(ns, def.getName());
				//if (Character.isLetterOrDigit(ns.charAt(ns.length()-1))) ns += "#";
				a.setPrefix(ctx.getModel().getNsURIPrefix(ns));
				e.setAttributeNodeNS(a);
			}
			else e.setAttributeNode(a = doc.createAttribute(def.getName()));

			XMLBean t = get_type(ctx);
			if (t instanceof simpleType) ((simpleType) t).toXML(a,object,ctx);
			else if (type != null && type.endsWith("IDREFS"))
				xs.listToXML(a,(RDFList) object.as(RDFList.class),XSD.IDREF.getURI(),ctx);
			else a.setValue(xs.toXMLValue(e,object,type,ctx));
		}

	/** check validity of simple content */
	
	public boolean isValid(Resource resource, Context ctx) {
		Model model = ctx.getModel();
		Property prop = model.createProperty(createURI(model,ctx));
		Statement stmt = resource.getProperty(prop);
		String value = stmt!=null?stmt.getString():null;
		attribute def = getDefinition(model,ctx);
		XMLBean t = def.get_type(ctx);
		if (t instanceof simpleType) return ((simpleType)t).isValid(value, ctx);
		return true;
	}

	public String getID(Attr a, Context ctx) {
		if (a==null) return null;
		// the attribute may be defined locally or globally by ref
		attribute def = getDefinition(ctx.getModel(),ctx);
		String t = expandQName(ctx.getDefaultNS(),def.getType(),ctx.getModel());
		// schema ID?
		if (t != null && t.startsWith(schema.XSD_URI) && t.endsWith("ID"))
			return a.getValue();
		return null;
	}

	/** return the attribute definition **/

	attribute getDefinition(Model model, Context ctx) {
		// The attribute may refer to its definition
		if (ref != null) return ctx.getAttribute(createURI(model,ctx));
		else return this;
	}
	
	public void toOWL(Restrictions rest, Context ctx) {
		
		// Restrictions are only used in the context of class definitions.
		// They need to be cleaned up, for example, when creating global attribute definitions
		boolean noRest = rest==null;
		
		schema xs = (schema) this.get_owner();
		String uri = createURI(xs.ont,ctx);		
		attribute def = getDefinition(xs.ont,ctx);
		if (def==null) return;
		if (rest==null) rest = new Restrictions();
		String type = expandQName(ctx.getDefaultNS(),def.getType(),xs.ont);
		
		// add cardinality restrictions
		// simple types (including QNames, IDREFs) have minCard of at most 1 because of possible duplication */
		boolean simple = type==null || (type.startsWith(schema.XSD_URI) && !type.endsWith("#anyType")) ;
		if (type==null || !type.equals(schema.ID)) {
			rest.addMin(uri, use.equals("required")?1:0,simple,def.getModel());
			rest.addMax(uri, use.equals("prohibited")?0:1,def.getModel());
		}
		
		// include no more than cardinality restrictions for referenced attributes
		if (!this.equals(def)) return;

		OntProperty property = xs.ont.createOntProperty(uri);

		Resource range = null;
		simpleType s = def.simpleType;
		if (s==null) s = ctx.getSimpleType(type);
		if (s!=null) {
			s.defineType(property,ctx);
			range = s.toOWL(ctx);
		}
		else {
			// default attribute type
			if (type==null) type = schema.anySimpleType;
			schema.defineType(property,type);
			range = schema.toOWL(xs.ont,type);
			
			// ecore extensions
			if (ecore!=null) ecore.toOWL(getModel(), uri, rest, get_node(), type, ctx);
		}
		
		// remove all evidence of an ID attribute
		if (range!=null && schema.isID(range)) property.remove();
		
		// Don't bother to add a range restriction only for it to be cleaned up afterwards (noRest)
		if (range!=null && rest!=null && !noRest && !schema.isID(range)) 
			rest.addRange(uri, range);
		
		// global attribute closure
		if (property.getRange()==null && ctx.isGlobalAttribute(def) 
				&& ctx.isClosed() && range!=null && !schema.ID.equals(range.getURI())) {
			property.setRange(range);
		}
		
		if (noRest) rest.cleanup(xs.ont,uri);
		
		// add comments
		if (annotation!=null) annotation.toOWL(property,ctx);	
	}
		
	private OntModel getModel() {
		return ((schema) this.get_owner()).ont;
	}

	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the type.
	 * @return String
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the type.
	 * @param type The type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns fixed.
	 * @return String
	 */
	public String getFixed() {
		return fixed;
	}

	/**
	 * Sets fixed.
	 * @param fixed The fixed to set
	 */
	public void setFixed(String fixed) {
		this.fixed = fixed;
	}

	/**
	 * Returns use.
	 * @return String
	 */
	public String getUse() {
		return use;
	}

	/**
	 * Sets use.
	 * @param use The use to set
	 */
	public void setUse(String use) {
		this.use = use;
	}

	/**
	 * @return global defininition for this attribute
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * set ref to global definition for this attribute
	 * @param string
	 */
	public void setRef(String string) {
		ref = string;
	}

	/**
	 * @return inline simple type definition
	 */
	public simpleType getSimpleType() {
		return simpleType;
	}

	public void setSimpleType(simpleType type) {
		simpleType = type;
	}

	public String get_default() {
		return _default;
	}

	public void set_default(String string) {
		_default = string;
	}

	public String getForm() {
		return form;
	}
	
	public boolean isQualified() {
		schema xs = (schema) get_owner();
		// if no TNS is defined, all attributes are unqualified
		if (xs.getTargetNamespace()==null) return false;
		// explicitly qualified attribute
		if (form!=null && form.equals("qualified")) return true;
		// global attributes (or references to them) are qualified
		else if (get_parent() instanceof schema || ref!=null) return true;
		else return xs.getAttributeFormDefault().equals("qualified");
	}

	public void setForm(String string) {
		form = string;
	}

	public String getRefName() {
		String n = getName();
		if (n!=null) return n;
		else return getRef();
	}

	public annotation getAnnotation() {
		return annotation;
	}

	public void setAnnotation(annotation annotation) {
		this.annotation = annotation;
	}

	/* ecore attributes */

	public String getReference() {
		if (ecore==null) return null;
		return ecore.getReference();
	}

	public void setReference(String reference) {
		if (ecore==null) ecore = new Ecore();
		ecore.setReference(reference);
	}

}
