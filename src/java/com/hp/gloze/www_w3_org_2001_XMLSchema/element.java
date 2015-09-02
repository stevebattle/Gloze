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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.xml.datatype.Duration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.gloze.Context;
import com.hp.gloze.DTDElement;
import com.hp.gloze.Gloze;
import com.hp.gloze.Restrictions;
import com.hp.gloze.XMLBean;
import com.hp.gloze.www_eclipse_org_emf_2002_Ecore.Ecore;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

/*! \page element element

 Elements map to RDF properties. An instance of an element maps to an RDF statement. 
 A qualified element is defined in the target namespace of the schema, as is the corresponding RDF property. 
 Where an element has a complex type we define a corresponding OWL ObjectProperty. 
 Where an attribute or element has an RDF recommended simple type we define a corresponding OWL DatatypeProperty. 
 Attribute and elements of non-recommended simple types form a grey zone with literal types (anySimpleType and ENTITY) still requiring a DatatypeProperty, but the URI (QName, NOTATION, IDREF) and structured types (duration, ENTITIES, IDREFS, NMTOKENS) switch; demanding an ObjectProperty.
 Where content has a named type (or if there is an explicit xsi:type) this is represented by an rdf:type statement on the node.
 	
 	e.g. for target namespace http://example.org/ and element named 'foo', the RDF property name is http://example.org/foo
 	
 	\include element1.xsd
 	
 	The OWL mapping was produced with lang=N3. This shows the property definition for 'foo'. It is a datatype property ranging over xs:string.
 	
 	\include element1.owl
 
 	Where the target namespace ends with an alphanumeric character, a fragment separator is introduced.
 	e.g. for target namespace http://example.org and element named 'foo', the RDF property name is http://example.org#foo
 	
 	\include element2.xsd
 	
 	This OWL was produced with lang=N3
 	
 	\include element2.owl
 	
 	Unqualified elements are not defined in the target namespace, but the mapping to RDF requires an absolute URI.
 	Unqualified elements occur if no target namespace is defined, or if an element is declared locally and its form is unqualified.
 	The properties corresponding to unqualified elements will be declared in the user-defined default namespace (xmlns) of the schema.
 	This is a command line parameter (not the xmlns defined on the document element).

 	e.g. for default namespace http://example.com/def/ and unqualified element named 'foo', the RDF property name is http://example.org/def/foo

 	\include element3.xsd
 	
 	The OWL mapping was produced with lang=N3 and xmlns=http://example.com/def/
 	
 	\include element3.owl
 	
 	In xml schema, elements have their own symbol space, distinct from other components such as attributes and types.
 	If there are overlaps between these symbol spaces, it is advisable to introduce a symbolic prefix to keep them distinct.
 	
 	e.g. the element named 'foo' and type named 'foo' in the target namespace http://example.org/ will clash.
 	Introducing a symbolic prefix '~' (at the command line) for elements resolves the clash giving us an RDF property name
 	http://example.org/~foo.
 	
 	The OWL mapping below was generated from the first schema above, but with element=~
 	
 	\include element4.owl
 	
	\section Type
	
	An element may have a simple or complex type. On the whole, simple typed elements map to OWL datatype properties 
	(exceptions include most of the datatypes that don't have a clean mapping into RDF); while complex typed elements
	map to object properties.
	
	The schema below defines a simple typed property 'foo' and a complex type property 'bar'. 
	
	\include element5.xsd
	
	With a corresponding OWL mapping:
	
	\include element5.owl
	
	An element may reference or directly include a simple type declaration.
	
	\section elementChildren Child components
	
	- \ref simpleType "simpleType"
	- \ref complexType "complexType"
	- \ref annotation "annotation"
	
	
 	\page elementString Datatype example xs:string
 	
 	The 'string' element contains an xs:string "foobar" as its content.
 	
 	\include elementString.xml
 	
 	This is defined in its schema.
 	
 	\include elementString.xsd
 	
 	The N3 translation declares a namespace prefix 'xs_' that allows the full URI <http://www.w3.org/2001/XMLSchema#string> to be abbreviated to xs_:string.
 	Note that the RDF mapping preserves namespaces declared in the XML (so they can be recovered in the reverse mapping), 
 	the namespace 'xs' defined as <http://www.w3.org/2001/XMLSchema> is already taken,
 	so Gloze defines an \em extended version with a trailing '#' and adds the underscore to the name.
 	The XML base is http://example.org/
 	
 	\include elementString.n3

	\page elementIdentity Example xs:ID, xs:IDREF

	The xs:ID and xs:IDREF types have document scope so are not recommended for use in RDF.
	Take a fragment of XML with an attribute 'id' of type xs:ID. 
	Instead of adding the 'id' attribute as a property of a resource,
	we use it to derive the URI of that resource. Given a base <tt>http://example.org/base</tt> the RDF mapping
	below includes a statement with property 'foo' and object <tt>http://example.org/base#foobar</tt>.
	The 'bar' element is an IDREF, and the object of this statement is the resource identified as '#foobar'.

	\include elementIdentity.xml
	
	The xml schema for this is as follows:
	
	\include elementIdentity.xsd

	This XML maps to the following RDF. 
	Note how the 'id' attribute has been dropped in the RDF.

	\include elementIdentity.n3	  

	\page elementDuration Example xs:duration
	  
	The duration "P2M26DT14H18M" is split into two separate values "P2M" and "P26DT14H18M".
	Notice the missing year and seconds components, any component is optional. Whereas the value
	space of duration is partially ordered, the spaces of these year/month and day/time types are totally ordered.

	\include elementDuration.xml
	
	The xml schema for this is as follows:
	
	\include elementDuration.xsd
	
	The object of the 'duration' statement is now a bnode with a pair of rdf:values, representing the two components of the original duration.
	These values may be distinguished by their type.
	The base is http://example.org/base.
	  
	\include elementDuration.n3	 
	 
	Mapping back to XML, we make use of the fact that these year/month and day/time types are sub-classes of duration.
	Durations are added component-wise, so we needn't be concerned with (indeterminate) carry from days to months.
	
	\page elementENTITY Example xs:ENTITY
	
	Entities allow common blocks of text to be substituted in place, reducing duplication and errors. Entities are not just any plain text, 
	but may also contain \em balanced markup. This fits the bill of the rdf:XMLLiteral datatype. Entity references are
	usually identified by surrounding them with '&' and ';', allowing the XML parser to expand them, 
	but this is not required for the schema entity type.
	
	\include elementENTITY.xsd
	
	An XML instance that defines and uses an entity 'eg' is as follows:
	
	\include elementENTITY.xml
	
	The rdf:XMLLiteral type is used in conjunction with the 'Literal' parseType of the rdf/xml serialization. 
	The base is http://example.org/base.
	
	\include elementENTITY.rdf
	
	\page elementQName Example xs:QName
	
	This example defines a qname 'eg:foobar', with a prefix 'eg' defined as http://example.com#.
	
	\include elementQName.xml
	
	The corresponding xml schema is as follows:
	
	\include elementQName.xsd
	
	The resulting RDF shows that the offending QName type has been dropped, and the expanded URI is a resource name.
	
	\include elementQName.n3
	
	\page elementIDREFS Example xs:IDREFS
	
	In this example we define an element 'foo' whose content type is an xs:ID so the foo element itself is implicated in the naming.
	The 'bar' element is of type xs:IDREFS; a list of IDREFs. The IDREF type is not used directly as a datatype (because this has document scope), 
	but becomes a global resource URI.
	
	\include elementIDREFS.xsd
	
	The 'bar' element refers twice to the same ID.
	\include elementIDREFS.xml
	
	
	In N3 the content of an RDF list is contained in brackets.
	\include elementIDREFS.n3
	
*/

/*! \page elementID Element Identity
 	This example includes an element 'foobar' with xs:ID content. This identifies the
 	immediately containing element , 'foobar' itself.
 	
 	\include elementIdentity1.xml
 	\include elementIdentity1.xsd
 	\include elementIdentity1.n3
 */

public class element extends DTDElement {
	private String _abstract = "false", form, ref, type;
	private String substitutionGroup, _default, nillable = "false";
	private simpleType simpleType;
	private complexType complexType;
	private annotation annotation;
	
	// ecore extensions
	private Ecore ecore;
	
	public element() throws IntrospectionException {
	}

	public element(String name, String minOccurs, String maxOccurs)
			throws IntrospectionException {
		super(name, minOccurs, maxOccurs);
	}

	public element(String namespace, String localName, String minOccurs,
			String maxOccurs) throws IntrospectionException {
		this(concatName(namespace, localName), minOccurs, maxOccurs);
	}

	public element(String namespace, String localName)
			throws IntrospectionException {
		this(namespace, localName, "1", "1"); // single occurrence
	}
	
	public boolean needSeq(Set<String> names, Context ctx) {
		if (names.contains(getElementName()) && ctx.isSequenced()) return true;
		names.add(getElementName());
		return super.needSeq(names,ctx);
	}
		
	public void reportLocalElements(Model model,Set<String> report, Context ctx) {
		if (name!=null  && !(get_parent() instanceof schema)) report.add(createURI(model,ctx));
		super.reportLocalElements(model,report,ctx);
	}

	public boolean isDefault(String attribute, String value) {
		if (attribute.equals("minOccurs"))
			return value.equals("1");
		if (attribute.equals("maxOccurs"))
			return value.equals("1");
		return false;
	}
		
	/** return this element or a substitution matching the DOM element */
	
	public element substitute(Element element, Context ctx) {
		populate();
		return ctx.substitute(createURI(element, ctx),this,ctx.getModel());
	}
	
	/** return this element or a substitution matching the statement */

	public element substitute(Statement stmt, Context ctx) {
		populate();
		if (stmt==null) return null;
		return ctx.substitute(stmt.getPredicate().getURI(),this,ctx.getModel());
	}
	
	/** create an element URI directly from an XML instance */
	
	public String createURI(Model model, String qname, Context ctx) {
		return expandQName(ctx.getDefaultNS(),ctx.getElementSymbol(),qname,get_node(),model);		
	}
	
	public static String createURI(Element elem, Context ctx) {
		if (elem.getNamespaceURI()!=null)
			return concatName(elem.getNamespaceURI(), ctx.getElementSymbol(), elem.getLocalName());
		else
			return concatName(ctx.getDefaultNS(), ctx.getElementSymbol(),elem.getNodeName());
	}
	
	public String createURI(Model model, Context ctx) {
		String uri;
		schema xs = (schema) this.get_owner();
		
		if (isQualified() && getName()!=null)
			uri = xs.qualifiedName(getName(),ctx.getElementSymbol(),model);

		// unqualified elements
		else if (!isQualified() && getName()!=null)
			uri = xs.unqualifiedName(getName(),ctx.getElementSymbol(),model,ctx);

		// QName references (to global elements) are resolved wrt the xmlns
		else uri = expandQName(ctx.getDefaultNS(),ctx.getElementSymbol(),getRef(),get_node(),model);
		
		return uri;
	}
	
	public void resolve(Model model, Context ctx) {
		ctx.putType(this,get_type(ctx));
		super.resolve(model, ctx);
	}
	
	private XMLBean get_type(Context ctx) {
		populate();
		if (complexType != null) return complexType;
		if (simpleType != null) return simpleType;
		// is the type pre-resolved
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
	
	/*! \page nil xsi:nil
	 XML differentiates between empty and null data. Such \em nillable elements can be described in the XML schema.
	 
	 \include nil.xsd
	 
	 An example of a nil element is shown in the example below. A null value is represented in RDF using rdf:nil,
	 the empty list.
	 
	 \include nil.xml
	 \include nil.n3
	 
	 Compare this with the same element simply left empty.
	 
	 \include nil1.xml
	 \include nil1.n3
	 
	 */
	
	public boolean toRDF(Resource subject, Element elem, Seq seq, Context ctx) 
	throws Exception {
		populate();
		Model m = ctx.getModel();
		String value = getValue(elem);
		if (value==null && _default != null) value = _default;
		String uri = createURI(m,ctx);
		Property prop = null;
		if (uri != null) prop = m.createProperty(uri);
		Statement stmt=null;
		// the element may be defined locally or globally (follow ref)
		element def = getDefinition(ctx.getModel(),ctx);
		
		// explicit xsi:type
		if (elem.hasAttributeNS(schema.XSI,"type")) {
			String t = elem.getAttributeNS(schema.XSI,"type");
			String fullname = expandQName(ctx.getDefaultNS(),t,elem,ctx.getModel());
			complexType c = ctx.getComplexType(fullname);
			if (c!=null) {
				String id = c.getID(elem,ctx);
				Resource o = id==null?m.createResource():m.createResource(addFragment(ctx.getBaseMap(), id).toString());
				stmt = m.createStatement(subject,prop,o);
				// the new resource, o, becomes the subject
				
				if (is_nillable()
				 && elem.hasAttributeNS(schema.XSI,"nil")
				 && elem.getAttributeNS(schema.XSI,"nil").equals("true")) 
					o.addProperty(RDF.value,RDF.nil);
				
				Seq subSeq = null;
				if (ctx.isSequenced() && elem.hasChildNodes() && c.needSeq(new HashSet<String>(), ctx))
					subSeq = m.getSeq(o.addProperty(RDF.type, RDF.Seq));
	
				int index = c.toRDF(o, elem, 0, subSeq,null,true,ctx);
				// mop up remaining values in sequence
				produceMixed(subSeq, index, elem);

				m.add(stmt);
				if (seq != null) seq.add(stmt.createReifiedStatement());
				return true;
			}
			else Gloze.logger.warn("undefined type: "+fullname);
		}
		// predefined schema datatype
		String type = expandQName(ctx.getDefaultNS(),def.getType(),m);
		if (type!=null && type.startsWith(schema.XSD_URI)) {
			if (is_nillable() && elem.hasAttributeNS(schema.XSI,"nil") && elem.getAttributeNS(schema.XSI,"nil").equals("true"))
				stmt = m.createStatement(subject,prop,RDF.nil);
			else {
				if (type.equals(schema.XSD_URI+"#ID") && value!=null && !value.trim().equals("")) {
					Resource o = m.createResource(addFragment(ctx.getBaseMap(), value.trim()).toString());
					stmt = m.createStatement(subject,prop,o);
				}
				else stmt = schema.toRDFStatement(subject, elem, prop, value, type, ctx);
			}
		}

		// user defined simple type
		XMLBean t = def.get_type(ctx);
		if (t instanceof simpleType) {
			// an empty, as opposed to nil, element is null
			if (value==null) value = "";
			if (is_nillable() && elem.hasAttributeNS(schema.XSI,"nil") && elem.getAttributeNS(schema.XSI,"nil").equals("true"))
				stmt = m.createStatement(subject,prop,RDF.nil);
			
			// if a simple type is nil there is no content or attributes
			// initialise restriction set according to level of restriction checking required
			else {
				Boolean b = ((simpleType) t).toRDF(subject, prop, elem, value, seq, null, ctx);
				if (!b) Gloze.logger.warn("cannot map value "+value+" into simpleType "+type);
			}
		}
		else if (t instanceof complexType) {
			// look for a global ID
			String id = ((complexType) t).getID(elem,ctx);
			Resource o = id==null?m.createResource():m.createResource(addFragment(ctx.getBaseMap(), id).toString());
			stmt = m.createStatement(subject,prop,o);
			// the new resource, o, becomes the subject
			if (is_nillable() && elem.hasAttributeNS(schema.XSI,"nil") && elem.getAttributeNS(schema.XSI,"nil").equals("true"))
				o.addProperty(RDF.value,RDF.nil);

			Seq subSeq = null;
			if (ctx.isSequenced() && elem.hasChildNodes() && ((complexType) t).needSeq(new HashSet<String>(),ctx))
				subSeq = m.getSeq(o.addProperty(RDF.type, RDF.Seq));
			
			int index = ((complexType) t).toRDF(o, elem, 0, subSeq, null,true,ctx);
			// mop up remaining values in sequence
			produceMixed(subSeq, index, elem);
		}
		else if (value!=null && stmt==null){ // untyped!
			Literal l = m.createLiteral(schema.processWhitespace(elem,value,null,ctx));
			stmt = m.createStatement(subject,prop,l);			
		}
		else if (type==null){ // empty
			if (is_nillable() && elem.hasAttributeNS(schema.XSI,"nil") && elem.getAttributeNS(schema.XSI,"nil").equals("true"))
				stmt = m.createStatement(subject,prop,RDF.nil);
			// link to anonymous resource
			else stmt = m.createStatement(subject,prop,m.createResource());
		}

		// if the statement was boxed, only add the link to the box
		if (stmt !=null) {
			m.add(stmt);
			if (seq != null) seq.add(stmt.createReifiedStatement());
		}
		return true;
	}

	public int toRDF(Resource subject, Node node, int index, Seq seq, boolean mixed, Context ctx) 
	throws Exception {
		populate();
		// the element may be defined locally or globally
		element def = getDefinition(ctx.getModel(),ctx);
		NodeList l = node.getChildNodes();
		if (def==null) {
			if (ref!=null) Gloze.logger.error("no such definition: "+ref);
			return index;
		}
		// index children of this XML node
		// occurs counts occurrences of this XSD element
		int max = maxOccurs.equals("unbounded")? Integer.MAX_VALUE : Integer.parseInt(maxOccurs); 		
		for (int occurs=0; index < l.getLength() && occurs<max; index++) {
			Node n = l.item(index);
			switch (n.getNodeType()) {
			case Node.ELEMENT_NODE:
				// we can substitute this element for another that matches the XML
				element e = def.substitute((Element) n, ctx);
				// map a single child element
				if (e!=null) e.toRDF(subject, (Element) n, seq, ctx);
				else return index;
				occurs++;
				break;
			// mixed content
			case Node.TEXT_NODE:
				if (mixed) schema.textToRDF(subject, seq, n, ctx);
			}
		}
		return index;
	}
	
	public int toXML(Element element, Resource rdf, int index, Set<Statement> pending, Context ctx) {
		populate();
		Seq seq = rdf.getModel().getSeq(rdf);
		// iterated elements may occur multiple times (don't check they're done)
		int max = maxOccurs.equals("unbounded")?Integer.MAX_VALUE:Integer.parseInt(maxOccurs);		
		// consume properties in sequence
		Statement stmt = null;
		int occurs = 0;
		for (; occurs<max; occurs++, index++) {
			// consume mixed values
			index = produceMixed(seq,index,element);
			if (index<seq.size()) {
				// the object of _i is a reified statement
				stmt = (Statement) asStatement((Resource) seq.getObject(index+1));
				element e = substitute(stmt,ctx);
				if (e!=null) e.toXML(element, stmt.getObject(),ctx);
				else break;
			}
			else break;
		}
		// do any pending properties match?
		Set<Statement> done = new HashSet<Statement>();
		for (Iterator ui = pending.iterator(); occurs<max && ui.hasNext(); ) {
			stmt = (Statement) ui.next();
			element e = substitute(stmt,ctx);
			if (e!=null) {
				e.toXML(element, stmt.getObject(),ctx);
				occurs++;
				done.add(stmt);
			}
		}
		pending.removeAll(done);
		
		return index;
	}
		
	public boolean toXML(Node xml, RDFNode rdf, Context ctx) {
		populate();
		Element e;
		Model m = ctx.getModel();
		Document doc = (Document) ((xml instanceof Document)?xml:xml.getOwnerDocument());

		// the element may be defined locally or globally
		element def = getDefinition(ctx.getModel(),ctx);
		schema xs = (schema) def.get_owner();

		if (isQualified()) {
			e = doc.createElementNS(xs.getTargetNamespace(), def.getName());
			// if the default ns is undefined use this tns (unprefixed)
			// (this element may already be in the default namespace)
			String dns = expandPrefix("", xml,null,ctx.getModel());
			// use prefixes where there may be unqualified elements and this is qualified
			if ((dns==null && (xs.getElementFormDefault().equals("unqualified")))
			|| (dns!=null && !xs.getTargetNamespace().equals(dns)) // if dns defined and this isn't in it
			|| (xs.getElementFormDefault().equals("unqualified")))
				e.setPrefix(lookupPrefix(xs.getTargetNamespace(),m,ctx));
			xml.appendChild(e);
		} else {
			// don't define the element in the (target) namespace
			e = doc.createElement(getElementName());
			xml.appendChild(e);
		}
		// nil
		if (def.is_nillable()) {
			if (rdf instanceof Resource 
			 && (rdf.equals(RDF.nil) || ((Resource)rdf).hasProperty(RDF.value,RDF.nil))) {
				e.setAttributeNS(schema.XSI, "xsi:nil", "true");
			}
		}
		String type = expandQName(ctx.getDefaultNS(),def.getType(),m);
		XMLBean t = def.get_type(ctx);	
		if (type!=null && type.startsWith(schema.XSD_URI)) {
			try {
				if (type.endsWith("#ID") || 
					type.endsWith("#IDREF") || 
					type.endsWith("#IDREFS") || 
					type.endsWith("#NMTOKENS") ||
					type.endsWith("#ENTITY") ||
					type.endsWith("#QName")) {
					e.appendChild(doc.createTextNode(xs.toXMLValue(e, rdf, type, ctx)));
				}
				else if (type.endsWith("#duration") && !rdf.isLiteral()) {
					// sum of all durations
					Duration duration = null;
					Resource r = (Resource) rdf;
					for (StmtIterator si = r.listProperties(RDF.value); si.hasNext(); ) {
						Duration d = schema.getDuration(si.nextStatement().getString());
						duration = duration==null?d:duration.add(d);
					}
					if (duration!=null)
						e.appendChild(doc.createTextNode(duration.toString()));
				}
				else addXMLValue(rdf, e, doc);
			} catch (Exception e1) {
				Gloze.logger.warn("missing value for: " + getElementName());
			}
		}
		else if (t instanceof complexType) {
			Set<Statement> pending = unsequenced((Resource) rdf);
			int index = ((complexType) t).toXML(e, (Resource) rdf, 0, pending, ctx);
			// search for matching extensions
			produceMixed(ctx.getModel().getSeq((Resource) rdf),index,e);
		}
		else if (t instanceof simpleType) {
			((simpleType) t).toXML(e, rdf,ctx);
		}
		else { // untyped
			xs.toXMLText(e,rdf,null,null,ctx);
		}
		return true;
	}

	/**
	 * @param rdf
	 * @param e
	 * @param doc
	 */
	public static void addXMLValue(RDFNode rdf, Element e, Document doc) {
		String s = null;
		if (rdf instanceof Resource) {
			Resource r = (Resource) rdf;
			if (r.hasProperty(RDF.value))
				s = r.getProperty(RDF.value).getLiteral().getString();
		} else
			s = ((Literal) rdf).getString();
		e.appendChild(doc.createTextNode(s));
	}
	
	element getDefinition(Model model, Context ctx) {
		// The element may refer to an element definition
		if (ref != null) return ctx.getElement(createURI(model, ctx));
		return this;
	}
	
	/* increment cardinalities of any super-property */
	
	private void superCard(Model model, Restrictions rest, int min, int max, boolean simple, Context ctx) {
		element def = getDefinition(model,ctx);
		String uri = ctx.getSubstitution(def);
		if (uri!=null) {
			rest.addMin(uri, Restrictions.product(getMinOccurs(), min),simple, def.getModel());
			rest.addMax(uri, Restrictions.product(getMaxOccurs(), max), def.getModel());

			element e = ctx.getElement(uri);
			if (e!=null) e.superCard(model,rest, min, max, simple, ctx);
		}		
	}
	
	/* increment cardinalities of any sub-property */

	private void subCard(Model model, Restrictions rest, int min, int max, boolean simple, Context ctx) {
		Vector subs = ctx.getSubstitutionFor(model, this);
		for (int i=0; subs!=null && i<subs.size(); i++) {
			element e = (element)subs.elementAt(i);
			String uri = e.createURI(model,ctx);
			// sub-properties may potentially (but not necessarily) occur - the minimum is unchanged
			rest.addMax(uri, Restrictions.product(getMaxOccurs(), max), e.getModel());

			e.subCard(model,rest, min, max, simple, ctx);
		}		
	}
	
	/*! \page substitution SubProperty relationships (substitution groups)
	  
	 There is a hierarchy among elements defined by substitution groups.
	 These substitution groups define sub-property relationships between properties.
	 A substitution group is defined by a head element, and member elements that substitute for the head. 
	 The property corresponding to the member is a sub-property of that corresponding to the head.
	 
	 In the example below, the \em head element 'foo' defines the substitution group, of which element 'bar'
	 is a member. This means that in the XML instance, 'bar' may be subsituted for 'foo'. Logically, any RDF
	 statement of 'bar' implies a corresponding statement of 'foo'.
	 
	 \include substitution1.xsd
	 \include substitution1.owl
	 
	*/

	public void toOWL(Restrictions rest, int min, int max, Context ctx) {
		populate();
		element def = getDefinition(getModel(),ctx);

		if (def==null) {
			Gloze.logger.warn("undefined element: "+getRef());
			return;
		}
		schema xs = (schema) def.get_owner();
		String uri = createURI(xs.ont,ctx);
				
		// default element type
		String type = expandQName(ctx.getDefaultNS(),def.getType(),getModel());
		if (type==null && def.simpleType==null && def.complexType==null) 
			type = schema.XSD_URI+"#anyType";
				
		// add cardinality restrictions
		if (rest!=null) {
			// even xs:anyType is potentially simple
			boolean simple = def.simpleType!=null || (type!=null && type.startsWith(schema.XSD_URI));
			rest.addMin(uri, Restrictions.product(getMinOccurs(), min),simple,def.getModel());
			rest.addMax(uri, Restrictions.product(getMaxOccurs(), max),def.getModel());
			
			// increment cardinalities of any super/sub-property
			superCard(getModel(),rest,min,max,simple,ctx);
			subCard(getModel(),rest,min,max,simple,ctx);
		}
		
		// include no more than cardinality restrictions for referenced elements
		if (!this.equals(def)) return;
		
		OntProperty property = null;
		Resource range = null;
		complexType ct = def.complexType;
		if (ct==null) ct = ctx.getComplexType(type);		
		if (ct!=null) {
			property = getModel().createObjectProperty(uri);			
			range = ct.toOWL(null,true, ctx);			
		}
		else if (def.simpleType!=null || ctx.getSimpleType(type)!=null) {
			simpleType s = def.simpleType;
			if (s==null) s = ctx.getSimpleType(type);
			// add simpleType range restrictions
			property = getModel().createOntProperty(uri);
			s.defineType(property,ctx);
			range = s.toOWL(ctx);
		}
		// schema datatype
		else {
			property = getModel().createOntProperty(uri);
			schema.defineType(property,type);
			range = schema.toOWL(xs.ont,type);
			
			// ecore extension
			if (ecore!=null) ecore.toOWL(getModel(), uri, rest, get_node(), type, ctx);
		}
		
		// add the type to the restrictions and property range
		if (ctx.isGlobalElement(def) && ctx.isClosed() && range!=null && !schema.isID(range) 
				&& !Restrictions.voidClass(range)) 
			property.addRange(range);
		
		if (rest!=null) rest.addRange(uri,range);
	
		// a nillable element may refer to rdf:nil (an object)
		if (def.is_nillable()) nullify(getModel(), rest, ctx, uri, def, property);
		
		// add comments
		if (annotation!=null) annotation.toOWL(property,ctx);
		
		// substitution groups map to subProperty relationships
		if (substitutionGroup!=null) {
			String sub = createURI(getModel(),getSubstitutionGroup(),ctx);
			property.addSuperProperty(getModel().createProperty(sub));
		}
		
	}

	private OntModel getModel() {
		return ((schema) this.get_owner()).ont;
	}


	private void nullify(OntModel ont, Restrictions rest, Context ctx, String uri, element def, OntProperty p) {
		p.addProperty(RDF.type,OWL.ObjectProperty);
		if (rest!=null || ctx.isGlobalElement(def)) {
			OntResource r = p.getRange();
			p.removeRange(r);
			Vector<Resource> v = new Vector<Resource>();
			v.add(RDF.nil);
			RDFList nil = ont.createList(v.iterator());
			OntClass c = ont.createEnumeratedClass(null,nil);
			if (rest!=null) rest.addRange(uri,c);
			Vector<Resource> u = new Vector<Resource>();
			u.add(r); u.add(c);
			p.addRange(ont.createUnionClass(null,ont.createList(u.iterator())));
		}
	}
	
	/** return the element name, where used, ref is identical * */

	public String getElementName() {
		return getName() != null ? getName() : ref;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public complexType getComplexType() {
		populate();
		return complexType;
	}

	public simpleType getSimpleType() {
		populate();
		return simpleType;
	}

	public void setComplexType(complexType type) {
		complexType = type;
	}

	public void setSimpleType(simpleType type) {
		simpleType = type;
	}

	public String getForm() {
		return form;
	}

	public void setForm(String string) {
		form = string;
	}

	public boolean isQualified() {
		schema xs = (schema) get_owner();
		// if no TNS is defined, all elements are unqualified
		if (xs.getTargetNamespace()==null) return false;
		// explicitly qualified
		if (form!=null && form.equals("qualified")) return true;
		// global elements (or references to them) are qualified
		else if (get_parent() instanceof schema || ref!=null) return true;
		else return xs.getElementFormDefault().equals("qualified");
	}

	public String getSubstitutionGroup() {
		return substitutionGroup;
	}

	public void setSubstitutionGroup(String string) {
		substitutionGroup = string;
	}

	public String get_abstract() {
		return _abstract;
	}

	public void set_abstract(String string) {
		_abstract = string;
	}

	public String get_default() {
		return _default;
	}

	public void set_default(String _default) {
		this._default = _default;
	}

	public String getNillable() {
		return nillable;
	}
	
	public boolean is_nillable() {
		return nillable.equals("true");
	}

	public void setNillable(String nillable) {
		this.nillable = nillable;
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
