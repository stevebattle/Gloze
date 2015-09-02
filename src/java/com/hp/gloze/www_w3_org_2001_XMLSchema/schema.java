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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.hp.gloze.Context;
import com.hp.gloze.Gloze;
import com.hp.gloze.XMLBean;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import com.ibm.icu.util.StringTokenizer;

/*! \page components Schema Components
 	
 	Gloze will process the following schema components. Excluded components are xs:field, xs:key, xs:keyref, xs:unique 
 	which enable XML content to be identified by XPath expressions. 
 	
 	- \subpage all "all"
 	- \subpage annotation "annotation"
 	- \subpage any "any"
	- \subpage anyAttribute "anyAttribute"
 	- \subpage attribute "attribute"
 	- \subpage attributeGroup "attributeGroup"
 	
 	- \subpage choice "choice"
 	- \subpage complexContent "complexContent"
 	- \subpage complexType "complexType"
 	
 	- \subpage documentation "documentation"	
 	
 	- \subpage element "element"	
 	- \subpage enumeration "enumeration"	
 	- \subpage extension "extension"

 	- \subpage fractionDigits "fractionDigits"

 	- \subpage group "group"

 	- \subpage import "import"
 	- \subpage include "include"

 	- \subpage length "length"
 	- \subpage list "list"
 	
 	- \subpage maxExclusive "maxExclusive"
 	- \subpage maxInclusive "maxInclusive"
 	- \subpage maxLength "maxLength"
 	- \subpage minExclusive "minExclusive"
 	- \subpage minInclusive "minInclusive"
 	- \subpage maxLength "maxLength"

 	- \subpage pattern "pattern"

 	- \subpage redefine "redefine"
 	- \subpage restriction "restriction"
 		
 	- \subpage sequence "sequence"
 	- \subpage simpleContent "simpleContent"
 	- \subpage simpleType "simpleType"
 	
 	- \subpage totalDigits "totalDigits"
 	
 	- \subpage union "union"
 	
 	- \subpage whiteSpace "whiteSpace"

 */

/*! \page schemaLocation xsi:schemaLocation

The XML Schema Instance namespace defines two attributes that declare schema location hints
that can be used by an XML processor to locate the schema.

Gloze may be be supplied with user-defined namespace/schema-location hints (from the command line
or through the API), or it looks for xsi:schemaLocation or xsi:noNamespaceSchemaLocation on the
document element.

The following simple XML may be lifted into RDF either by supplying the schema location
on the command line (eg. "schemaLocation.xml http://example.org/ mySchema.xsd"), or as shown in this case,
using an explicit xsi:schemaLocation. Note that the xsi namespace must be declared.

\include schemaLocation.xml

When dropping a document into XML, a schema location can be added by supplying the full (base) path of the
schema using the schemaLocation parameter (e.g. "-Dgloze.schemaLocation=file:/C:/myExamples/mySchema.xsd" ).
The schema used in the mapping are relativized against this base and added to the xsi:schemaLocation or
xsi:noNamespaceSchemaLocation attribute on the document element.
 
*/


public class schema extends XMLBean {

	public static final String XSD_URI = "http://www.w3.org/2001/XMLSchema";
	public static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";
		
	public static final String anySimpleType = XSD_URI+"#anySimpleType";
	public static final String anyType = XSD_URI+"#anyType";
	public static final String ENTITY = XSD_URI+"#ENTITY";
	public static final String ID = XSD_URI+"#ID";
	public static final String NOTATION = XSD_URI+"#NOTATION";
	
	// targetNamespace is reserved for the actual value set in the schema
	// namespace is the working copy and may be set to a default if no tns is provided
	
	private String version, targetNamespace, //namespace,
		elementFormDefault = "unqualified",
		attributeFormDefault = "unqualified",
		blockDefault = "", finalDefault = "";

	private Import[] _import;
	private Include[] include;
	private element[] element;
	private complexType[] complexType;
	private simpleType[] simpleType;
	private attribute[] attribute;
	private attributeGroup[] attributeGroup;
	private group[] group;
	private redefine[] redefine;
	private annotation[] annotation;
	
	// the schema location
	public URL _url;
	public OntModel ont; // the ontology for this schema
	private boolean processed = false;
	
	public enum type {
		complexType, simpleType
	}
	
	public schema() throws IntrospectionException {
		ont = ModelFactory.createOntologyModel();
	}

	public boolean isDefault(String attribute, String value) {
		if (attribute.equals("elementFormDefault"))
			return value.equals("unqualified");
		if (attribute.equals("attributeFormDefault"))
			return value.equals("unqualified");
		if (attribute.equals("blockDefault"))
			return value.equals("");
		if (attribute.equals("finalDefault"))
			return value.equals("");
		return false;
	}
	
	/** unqualified names are defined in the target namespace (or supplied namespace if undefined)
	 * qualified names require prefix expansion using schema xmlns declarations */
	
	public String expandName(String name, Model model, Context ctx) {
		if (getTargetNamespace()!=null)
			return qualifiedName(name,null,model);
		else return unqualifiedName(name,null,model,ctx);
	}
	
	/** name an qualified component in the target namespace */

	protected String qualifiedName(String name, String symbol, Model model) {
		return concatName(getTargetNamespace(), symbol, name);
	}
	
	/** name an unqualified component in the given namespace */

	protected String unqualifiedName(String name, String symbol, Model model, Context ctx) {
		String ns = ctx.getDefaultNS();
		if (ns==null) ns=Context.DEFAULT_XMLNS; // use xs URL where unqualified is undefined
		return concatName(ns, symbol, name);
	}

	/** add globals to the context, maintaining a set of explored schema */
	
	public void gatherGlobals(Model model, Context ctx, Set<URL> done) {
		Set<String> globalElements = new HashSet<String>();
		Set<String> globalAttributes = new HashSet<String>();
		Set<String> globalTypes = new HashSet<String>();
		Set<String> localElements = new HashSet<String>();
		Set<String> localAttributes = new HashSet<String>();
		
		// we're done with this URL (don't explore it again)
		done.add(get_url());
		
		// consult included schema
		for (int i = 0; include != null && i < include.length; i++)
			include[i].gatherGlobals(model,ctx, done);
		
		// consult imported schema
		for (int i = 0; _import != null && i < _import.length; i++)
			_import[i].gatherGlobals(model,ctx, done);

		// consult redefined schema
		for (int i = 0; redefine != null && i < redefine.length; i++)
			redefine[i].gatherGlobals(model, ctx, done);		

		if (ctx.isReport()) System.out.println("REPORT "+get_url());

		// global elements
		if (ctx.isReport()) System.out.println("GLOBAL ELEMENTS");
		for (int i = 0; element!=null && i < element.length; i++) {
			String uri = element[i].createURI(model,ctx);
			ctx.putElement(uri,element[i]);
			globalElements.add(uri);
			if (ctx.isReport()) System.out.println("\t"+uri);
			
			// ..and substitution groups
			String sub = element[i].getSubstitutionGroup();
			if (sub!=null) ctx.putSubstitution(element[i], element[i].createURI(model,sub,ctx));
		}

		// global attributes
		if (ctx.isReport()) System.out.println("GLOBAL ATTRIBUTES");		
		for (int i = 0; attribute!=null && i < attribute.length; i++) {
			String uri = attribute[i].createURI(model,ctx);
			ctx.putAttribute(uri,attribute[i]);
			globalAttributes.add(uri);
			if (ctx.isReport()) System.out.println("\t"+uri);
		}
		// global groups
		for (int i = 0; group!=null && i < group.length; i++) {
			ctx.putGroup(group[i].createURI(model,ctx),group[i]);
		}		
		// global attribute groups
		for (int i = 0; attributeGroup!=null && i < attributeGroup.length; i++) {
			ctx.putAttributeGroup(attributeGroup[i].createURI(model,ctx),attributeGroup[i]);
		}		
		// global simple type
		for (int i = 0; simpleType!=null && i < simpleType.length; i++) {
			ctx.putSimpleType(simpleType[i].createURI(model,ctx),simpleType[i]);
		}		
		// global complex type
		if (ctx.isReport()) System.out.println("GLOBAL TYPES");		
		for (int i = 0; complexType!=null && i < complexType.length; i++) {
			String uri = complexType[i].createURI(model,ctx);
			globalTypes.add(uri);
			ctx.putComplexType(uri,complexType[i]);
			if (ctx.isReport()) System.out.println("\t"+uri);
		}
		
		reportLocalElements(model,localElements,ctx);
		reportLocalAttributes(model,localAttributes,ctx);
		if (ctx.isReport()) {
			System.out.println("LOCAL ELEMENTS");
			for (String uri: localElements) System.out.println("\t"+uri);
			System.out.println("LOCAL ATTRIBUTES");
			for (String uri: localAttributes) System.out.println("\t"+uri);
			System.out.println();
		}
		
		// check for name clashes
		clash(globalElements, globalAttributes);
		clash(globalElements, globalTypes);
		clash(globalElements, localAttributes);
		clash(globalElements, localElements);
		
		clash(globalAttributes, globalTypes);
		clash(globalAttributes, localElements);
		clash(globalAttributes, localAttributes);

		clash(globalTypes, localElements);
		clash(globalTypes, localAttributes);
		
		clash(localAttributes, localElements);
}
	
	void clash(Set<String> s, Set<String> t) {
		for (String m: s) if (t.contains(m)) Gloze.logger.warn("uri clash: "+m);
	}
	
	static int textToRDF(Resource subject, Seq seq, Node node, int index, Context ctx) {
		NodeList l = node.getChildNodes();
		next: while (index < l.getLength()) {
			Node n = l.item(index);
			switch (n.getNodeType()) {
			case Node.TEXT_NODE:
				textToRDF(subject, seq, n, ctx);
			case Node.COMMENT_NODE:
				index++;
				break;
			case Node.ELEMENT_NODE:
				break next;
			}
		}
		return index;
	}
	
	public static void textToRDF(Resource subject, Seq seq, Node n, Context ctx) {
		String value = ((Text) n).getNodeValue();
		value = processWhitespace(n,value,null,ctx);
		if (!value.equals("")) {
			Model m = ctx.getModel();
			Literal lit = m.createLiteral(value);
			Statement stmt = m.createStatement(subject, RDF.value, lit);
			m.add(stmt);
			if (seq!=null) seq.add(stmt.createReifiedStatement());
		}
	}
	
	/*! \page noSchemaMapping No-Schema Mapping
	 The use of xs:any makes it necessary to map content for which we have no schema.
	 
	 - \ref any "any example"
	 
	 The schema-less mapping can be used even where we have no schema at all
	 (this is the default mode if no match can be found for the document element).
	 
	 See also the use of xsi:type for returning from a No-Schema mapping back into a schema.
	 - \ref type
	 */
	
	public boolean toRDF(Resource subject, Property prop, String value, String type, Element elem, Context ctx) 
	throws Exception {
		if (elem.hasAttribute("xml:lang")) {
			// no datatype required for a lang qualified string
			Literal l = ctx.getModel().createLiteral(value,elem.getAttribute("xml:lang"));
			subject.addProperty(prop,l);
			return true;
		}
		return toRDF(elem, subject, prop, processWhitespace(elem,value,type,ctx), type, null, null, ctx);
	}
	
	/*! \page datatypes Datatypes
	 
	 The RDF semantics recommendation identifies a subset of XML schema datatypes that are suitable for use in RDF.
	 The following XML datatypes may be used in RDF typed literals. 
	 For example, an xs:string "foobar", would be represented in RDF (N3)
	 as "foobar"^^<http://www.w3.org/2001/XMLSchema#string>.
	  
     - xs:string
     - xs:boolean
     - xs:decimal
     - xs:float
     - xs:double
     - xs:dateTime
     - xs:time
     - xs:date
     - xs:gYearMonth
     - xs:gYear
     - xs:gMonthDay
     - xs:gDay
     - xs:gMonth
     - xs:hexBinary
     - xs:base64Binary
     - xs:anyURI
     - xs:normalizedString 
     - xs:token
     - xs:language
     - xs:NMTOKEN
     - xs:Name
     - xs:NCName
     - xs:integer
     - xs:nonPositiveInteger
     - xs:negativeInteger
     - xs:long
     - xs:int
     - xs:short
     - xs:byte
     - xs:nonNegativeInteger
     - xs:unsignedLong
     - xs:unsignedInt
     - xs:unsignedShort
     - xs:unsignedByte
     - xs:positiveInteger  
	
	\subpage elementString "datatype example"
	 
	 The exceptions include:
	 - xs:anySimpleType
	 - xs:duration
	 - xs:ENTITY
	 - xs:ENTITIES
	 - xs:ID
	 - xs:IDREF
	 - xs:IDREFS
	 - xs:NMTOKENS
	 - xs:NOTATION
	 - xs:QName
	 
	 \see 
	 http://www.w3.org/TR/2004/REC-rdf-mt-20040210/
	 http://www.w3.org/TR/xpath-functions/ 
	 http://www.w3.org/TR/swbp-xsch-datatypes/
	 
	 The following sections explore work-arounds for all of these datatypes.
	 
	 \section anySimpleType The mother of all simple types (xs:anySimpleType)
	 
	 This type is the base of all simple types with an unconstrained lexical space. User defined restrictions of
	 xs:anySimpleType are not allowed. Indeed, users are generally advised to steer clear of it altogether.
	 
	 Yet, both elements and attributes may be defined to be of type xs:anySimpleType (it's also the default type for attributes).
	 Also, anySimpleType may be used as the base of a simpleContent extension.
	 
	 Thinking about an RDF savvy mapping, it occupies a similar place in the pantheon of classes
	 as rdfs:Literal, the superclass of all literals including datatypes. 
	 Thus any XML content of type xs:anySimpleType is mapped to an rdfs:Literal. 

	 \subpage attributeAnySimpleType "anySimpleType example"

	 \see  
	 http://www.w3.org/2001/05/xmlschema-rec-comments#pfiS4SanySimpleType 
	 http://lists.w3.org/Archives/Member/w3c-xml-schema-ig/2002Jan/0065.html

	 \section duration Duration (xs:duration)
	 
	 The problem with xs:duration is that there's no well-defined total ordering over it's value space 
	 (durations are partially ordered). 
	 The problem stems from there being an indeterminate number of days in a month. 
	 The recommended solution used by Gloze is to
	 distill a single period such as "P7Y2M26DT14H18M10S" (years, months, days, hours, minutes, seconds) 
	 into separate xs:yearMonthDuration "P7Y2M" (years, months) and xs:dayTimeDuration "P26DT14H18M10S" (days, hours, minutes, seconds) datatypes.
	 The reverse process, is equivalent to adding the these values, both of which are subclasses of duration.
	 When adding two durations, each component is added independently, ignoring - in particular - any carry from days to months.
	 
	 \subpage elementDuration "duration example"
	 
	 \section entity Entities (xs:ENTITY)
	 
	 An XML schema ENTITY allows the substitution of common text values or balanced mark-up defined as XML entities. 
	 ENTITY values must match an entity name declared in the DTD of the instance document. 
	 The value space of unexpanded entities is scoped to the instance document it appears in. 
	 For the XML to RDF mapping, internally defined entities are therefore expanded. 
	 As they may include balanced mark-up, an expanded entity can be described as an RDF XMLLiteral.
	 There is currently no reverse mapping due to technical issues in editing document type declarations in level 2 DOM.
	 
	 \subpage elementENTITY "entity example"
	 	 
	 \see http://jena.sourceforge.net/how-to/typedLiterals.html#xsd
	 
	 \section xsid Identity datatypes (xs:ID, xs:IDREF)
	 
	  An element is considered to have an ID if it has an attribute of type ID, or if the type of the element itself is an ID.
	  
	  IDs have no distinguishing features looking at the XML alone, they look like ordinary content.
	  We look to the XML schema which will identify the datatype as xml schema ID. 
	  The ID is associated with the enclosing element, and that element can have at most one ID.
	  
	  XML IDs are defined to have document scope, such that a given ID must be unique within a single document 
	  and that each ID reference should have a corresponding ID within the same document.
	  One advantage of the mapping into RDF is that a single RDF model may contain descriptions
	  of multiple documents. We have ensure that we preserve the global uniqueness of identifiers, 
	  and do not lose the correlation between IDs and their references when moving to this global context.
	  An identifier of type ID can be transformed into a URI by treating it as a fragment identifier relative to the document base. 
	  
	  For example, a base <tt>http://example.org/base</tt> an XML ID "foobar" combine to give the URI,
	  <tt>http://example.org/base#foobar</tt> .
	  
	  Properties of type ID will disappear, as these simply define the URI of the identified resource.
	  A corresponding reference to this resource with an IDREF is similarly expanded into a URI reference. 
	  
	 \subpage elementIdentity "identity example"
	 
	 \section notation Notation (xs:NOTATION)
	 
	 NOTATIONs are restricted to QNames declared in the schema. For the purposes of RDF mapping they are subject to the same rules as QNames.
	 The target namespace and notation name are expanded to give an absolute URI for the notation resource.
	 
	 \subpage attributeNotation "notation example"
	  	  
	 \section qNames Qualified Names (xs:QName)
	 
	 QNames define the space of (optionally) qualified local names. The scope of an XML namespace prefix includes the
	 element it is defined in and its children (subject to shadowing). This lexical scoping doesn't translate directly into RDF where everything has global scope.
	 However, the expanded QName is a URI, so it may be translated into an object reference, though typically we have no knowledge of the type of object referred to. 
	 This URI becomes associated with a resource.
	 
	 For example, given a namespace prefix 'eg' defined as "http://example.org" the QName "eg:foobar" would be
	 expanded to give the URI, <tt>http://example.org#foobar</tt> .
	 
	 \subpage elementQName "QName example"
	 
	 \section listTypes List types (xs:IDREFS, xs:ENTITIES, xs:NMTOKENS)
	 
	 Although list types are treated as simple in XML schema, they are not recommended for use in RDF. Instead,
	 we construct an rdf:list of the corresponding non-list type (xs:IDREF, xs:ENTITY, xs:NMTOKEN).
	 
	 \subpage elementIDREFS "IDREFS example"

	*/

	/*! \page subclass SubClass Relationships (complex type derivation)
	 
	 Sub-class relationships may be derived from extensions and restrictions of complex content.
	 
	 - \ref restrictionComplexContent "restriction of complex content"
	 - \ref extensionComplexContent "extension of complex content"
	 
	 */

	public boolean toRDF(Node node, Resource subject, Property prop, String value, String type, Seq seq, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		
		if (type!=null && type.equals(XSD_URI+"#anySimpleType")) type = RDFS.Literal.getURI();
			
		RDFDatatype dt = getDatatype(type);
		Model m = ctx.getModel();
		Statement stmt = null;
		RDFNode object = null;
		if (value==null) return true;

		// are all restrictions observed
		if (restrictions!=null)
			for (restriction r: restrictions) 
				if (!r.isValid(value,type,ctx)) return false;
		
		simpleType s = ctx.getSimpleType(type);
		if (s!=null) return s.toRDF(subject,prop,node,value,seq,restrictions,ctx);
		
		// this may be an ID embedded in element content
		else if (type!=null && type.equals(XSD_URI+"#ID")) {
			String uri = subject.isAnon()?null:subject.getURI();
			String frag = addFragment(ctx.getBaseMap(), value).toString();
			if (frag.equals(uri)) return true;
			else object = m.createResource(frag);	
		}		
		else if (type!=null && type.equals(XSD_URI+"#IDREF"))
			object = m.createResource(addFragment(ctx.getBaseMap(), value).toString());		
		
		else if (type!=null && type.equals(XSD_URI+"#IDREFS"))
			object = toRDFList(node,value,XSD.IDREF.getURI(),null,ctx);		

		else if (type!=null && type.equals(XSD_URI+"#ENTITIES"))
			object = toRDFList(node,value,XSD.ENTITY.getURI(),null,ctx);		

		else if (type!=null && (type.equals(XSD_URI+"#QName") || type.equals(XSD_URI+"#NOTATION"))) 
			object = m.createResource(expandQName(ctx.getDefaultNS(),value,node,ctx.getModel()));
		
		else if (type!=null && type.equals(XSD_URI+"#anyURI")) {
			URI uri = null;
			if (isValidURI(value)) uri = new URI(value);
			// it may be a relative URI
			// to avoid confusion it should begin with an initial name character
			// ensure it isn't a QName
			else if (value.indexOf(":")<0 && Character.isJavaIdentifierStart(value.charAt(0)))
				uri = resolveBase(node, new URI(value), ctx);		
			if (uri!=null) object = m.createTypedLiteral(uri.toString(),dt);
			else return false;
		}
		else if (type!=null && type.equals(XSD_URI+"#anySimpleType")) {
			object = m.createTypedLiteral(value==null?"":value,dt);
		}
		else if (RDFS.Literal.getURI().equals(type)) {
			object = m.createLiteral(value==null?"":value);
		}
		else if (value!=null && dt!=null && dt.isValid(value)) object = m.createTypedLiteral(value, dt);
		else object = m.createLiteral(value==null?"":value);
		
		if (object==null) return false;
		// if no property is supplied just add the value to the sequence/list
		if (prop==null && seq!=null) seq.add(object);
		else  {
			stmt = m.createStatement(subject, prop, object);
			m.add(stmt);
			// this can only be a sequence
			if (seq != null) seq.add(stmt.createReifiedStatement());			
		}		
		return true;		
	}
	
	/*! \page order Ordered Content
	 
	 An XML document has a tree structure where the children of each element are lexically ordered.
	 Sometimes this ordering is significant, sometimes it is not. Gloze is designed from the point of
	 view of retaining sufficient information to reconstruct the original document from the RDF, making it
	 possible to round-trip from XML to RDF and back again. This doesn't mean recording the
	 order in all cases. In many cases the XML schema contains enough ordering information to
	 reconstruct the original sequence. Ambiguity is created by multiple occurrences of the same element,
	 by mixed content, or by the appearance of the xs:any wild-card. Sequences of singly occurring
	 xs:sequence are entirely unambigous, as are choices where only one of the choices can occur
	 (so long as this occurs once). The compositor 'all', where all orderings are valid typically require
	 sequencing (unless in the degenerate case where they only have a single element).
	 The ordering of attributes is not significant.
	 
	 All the examples in this section were generated with order=seq (-Dgloze.order=seq).
	 
	 The following schema describes an unambiguous sequence containing an unambiguous choice. 
	 
	 \include ordering.xsd
	 
	 The XML below requires no additional ordering information.
	 
	 \include ordering.xml
	 \include ordering.n3
	 
	 Where there is ambiguity in the schema for a given element content this is captured as an RDF sequence.
	 We need to record the order in which particular statements appear. This is achieved by
	 reifying the statement, giving us an object that can be added to an rdf:Seq.
	 
	 The schema below permits multiple occurrences of the element 'bar', so ordering information needs
	 to be recorded if it is significant.
	 
	 \include ordering1.xsd
	 \include ordering1.xml
	 
	 The RDF mapping shows how ordering information is recorded alongside the standard RDF mapping.
	 The standard mapping involves adding the property 'foo' with value "foobar". 
	 The subject of this statement is also an RDF sequence, and the first member of this
	 sequence is the reification with rdf:predicate 'foo' and rdf:object "foobar".
	 The two occurrences of 'bar'with identical values, "foobar" result in the assertion
	 of a single property/value. Despite this, two reified statements are added to the sequence,
	 one for each occurrence. 
	 
	 \include ordering1.n3
	 
	 Ordering is transparent from an ontological perspective. One of the design goals was
	 to allow users of the RDF mapping to ignore the sequence if it is not relevant.
	 Ordering is treated as a data-structuring issue rather than an ontological one.
	 The addition of a property to a resource is treated independently of adding it to the sequence;
	 sequencing meta-data is overlaid on top of the existing unordered information model.
	 
	 */
	 
	/*! \page mixed Mixed Content
	 
	 Mixed content, that is text interleaved with markup, also requires ordering. The following example
	 includes combined text and markup. Note that the text content of an element appears as an rdf:value.
	 
	 \include mix.xml
	 \include mix.xsd
	 \include mix.n3
 
	*/

	/*! \page identity Identity
	 XML is not just a tree, but a tree with pointers. 
	 An xs:IDREF points to an element with an xs:ID within the same document.
	 An xs:ID identifies the immediately containing element. Typically this is an
	 xs:ID attribute on the element, though it may also be xs:ID simple content.
	 
	 In RDF we identify the element \em content, by assigning it a URI.
	 This URI is the object of the statement representing the element occurrence.
	 
	 - \subpage attributeID "attribute identity"
	 - \subpage elementID "element identity"
	 
	 */
	
	/*! \page type xsi:type
	 The XML schema instance namespace defines xsi:type allowing elements be be explicitly annotated with
	 type information. This can be used as an alternative mechanism to substitution groups, but also provides
	 a way to jump back into a schema from within xs:any content. An example of this is shown below using an XML
	 document that has no schema.
	 
	 The document element 'myLink' is not defined in the schema, so is processed as if it were subject to xs:any
	 and a default no-schema mapping is employed.
	 This unidentified element is treated as unqualified and is defined in the default namespace set by the xmlns parameter.
	 
	 The link schema doesn't define the 'myLink' element, but it does define the 'SimpleLink' content. The XML
	 instance refers to this using an xsi:type attribute. The RDF mapping for this generates a corresponding rdf:type
	 statement. The content of 'myLink' is then processed according to the content model of 'SimpleLink', so we have
	 escaped from the xs:any no-schema mapping.

	 \include linkType.xml	 
	 \include linkType.xsd
	 
	 The use of the 'SimpleLink' content model is evidenced by the correct datatyping of the xlink:href and the
	 insertion of the xlink:type implied by the xlink attribute group.
	 
	 \include linkType.n3
	 
	 \see http://www.w3.org/2001/XMLSchema-instance
	 */
	
	static Statement toRDFStatement(Resource subject, Element elem, Property prop, String value, String type, Context ctx) 
	throws Exception {
		Model m = ctx.getModel();

		if (value==null) { 
			if (prop==RDF.value || type.endsWith("#ID")) return null;
			return m.createStatement(subject,prop,"");
		}
		
		Statement stmt = null;
		if (type.endsWith("#ID")) ;
		// ID is used to name the owning element - don't generate RDF
		// IDREF to globally named element
		else if (type.endsWith("#IDREF")) {
			Resource r = m.createResource(addFragment(ctx.getBaseMap(), value).toString());
			stmt = m.createStatement(subject,prop,r);
		}
		else if (type.endsWith("#IDREFS")) {
			Resource r = toRDFList(elem,value,XSD.IDREF.getURI(),null,ctx);	
			stmt = m.createStatement(subject,prop,r);
		}
		else if (type.endsWith("#NMTOKENS")) {
			Resource r = toRDFList(elem,value,XSD.NMTOKEN.getURI(),null,ctx);	
			stmt = m.createStatement(subject,prop,r);
		}
		else if (type.endsWith("#QName")) {
			RDFNode object = null;
			String name = XMLBean.expandQName(ctx.getDefaultNS(),null,value,elem,ctx.getModel());
			if (name!=null) {
				object = m.createResource(name);
				stmt = m.createStatement(subject,prop,object);
			}
		}
		else if (type.endsWith("#duration")) {
			// map duration into separate xs:yearMonthDuration and xs:dayTimeDuration
			// introduce intermediate bnode
			Resource object = m.createResource();
			stmt = m.createStatement(subject,prop,object);
			RDFNode ym = schema.yearMonthDuration(m,value);
			RDFNode dt = schema.dayTimeDuration(m,value);
			if (ym!=null) object.addProperty(RDF.value, ym);
			if (dt!=null) object.addProperty(RDF.value, dt);
		}
		else if (type.endsWith("#ENTITY")) {
			DocumentType doctype = elem.getOwnerDocument().getDoctype();
			Pattern entityPattern = Pattern.compile("<!ENTITY\\s+"+value+"\\s+'(.*)'>");
			Matcher match = entityPattern.matcher(doctype.getInternalSubset());
			if (match.find()) value = match.group(1);
			Literal l = m.createTypedLiteral(value,RDF.getURI()+"XMLLiteral");
			stmt = m.createStatement(subject,prop,l);
		}
		else { // schema datatype?
			RDFDatatype dt = getDatatype(type);
			if (dt != null) {
				Literal l = m.createTypedLiteral(processWhitespace(elem,value,type,ctx), dt);
				stmt = m.createStatement(subject,prop,l);
			}
		}
		return stmt;
	}
	
	static String processWhitespace(Node node, String value, String type, Context ctx) {
		if (value==null) return null;
		// whitespace processing is a two-step process involving whitespace replacement and collapse
		boolean replace = true, collapse = true;
		
		// get xml:space setting for this node
		boolean preserve = preserved(node,ctx.isPreserved());
		
		// don't process text (unless overridden)
		if (type==null) replace = collapse = !preserve;
		
		// don't process strings (unless overridden)
		else if (type.equals(schema.XSD_URI+"#string"))
			replace = collapse = false;
				
		// only perform whitespace replacement on normalized strings (unless overridden)
		else if (type.equals(schema.XSD_URI+"#normalizedString"))
			collapse = false; // replace whitespace only
		
		if (replace) value = replaceWhitespace(value);
		if (collapse) value = collapseWhitespace(value);
		return value;
	}
	
	/** whitespace replacement */
	static String replaceWhitespace(String value) {
		if (value!=null) return value.
		replaceAll("\t", " ").
		replaceAll("\r", " ").
		replaceAll("\f", " ").
		replaceAll("\n", " ");
		return value;
	}
	
	/** whitespace collapse */
	static String collapseWhitespace(String value) {
		if (value!=null) {
			value = value.trim();
			while (value.indexOf("  ")>0) value = value.replaceAll("  "," ");
		}
		return value;
	}
	
	static boolean preserved(Node node, boolean def) {
		if (node==null) return def;
		switch (node.getNodeType()) {
		case Node.ATTRIBUTE_NODE:
			Attr a = (Attr) node;
			if (a.getNamespaceURI()!=null && a.getNamespaceURI().equals(XML) && a.getLocalName().equals("space"))
				return a.getValue().equals("preserve");
			return preserved(a.getOwnerElement(),def);
		case Node.ELEMENT_NODE:
			Element e = (Element) node;
			if (e.hasAttributeNS(XML,"space"))
				return e.getAttributeNS(XML,"space").equals("preserve");
			return preserved(e.getParentNode(),def);
		default:
			return def;
		}
	}
	
	/** map an array of attributes to RDF */
	
	public void toRDF(Resource subject, Node node, attribute[] attribute, Context ctx) 
	throws Exception {
		Model m = subject.getModel();
		NamedNodeMap atts = node.getAttributes();
		// expand attribute names
		Map<String,Node> map = new HashMap<String,Node>();
		for (int i=0; i<atts.getLength(); i++) {
			// xmlns declaration
			if (atts.item(i).getNodeName().startsWith("xmlns")) {
				// set the namespace definition on the model
				String name = atts.item(i).getNodeName();
				String prefix = "";
				if (name.indexOf(":")>=0) {
					prefix = name.substring("xmlns:".length());
					String ns = atts.item(i).getNodeValue();
					if (!ns.equals(XMLBean.XML) && !ns.equals(schema.XSI)) 
						addPrefixes(prefix,ns,m);
				}
				continue;
			}
			else {
				String name = expandQName((Attr) atts.item(i),ctx);
				// The XML namespace is disallowed in RDF
				if (!name.startsWith(XMLBean.XML)) map.put(name,atts.item(i));
			}
		}		
		for (int i = 0; attribute != null && i < attribute.length; i++) {
			attribute def = attribute[i].getDefinition(m,ctx);
			if (def==null) {
				if (!attribute[i].getRef().startsWith("xml:"))
					Gloze.logger.warn("undefined attribute: "+attribute[i].getRef());
				continue;
			}			
			Attr a = (Attr) map.get(def.createURI(m,ctx)); //map.getNamedItem(name);
			// the attribute may not have any occurrences
			attribute[i].toRDF(subject, a, ctx);
		}		
	}
	
	/** map a list type to an RDF list, e.g. IDREFS (list of IDREF) */

	public static RDFList toRDFList(Node node, String values, String itemType, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		if (values==null) return null;
		RDFList l= ctx.getModel().createList();
		StringTokenizer t = new StringTokenizer(values);
		while (l!=null && t.hasMoreTokens())
			l = toRDFList(node,t.nextToken(),itemType,l,restrictions,ctx);
		return l;
	}
	
	/** incrementally add a value to an RDFList */
	
	public static RDFList toRDFList(Node node, String value, String itemType, RDFList list, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		RDFDatatype dt = getDatatype(itemType);
		Model m = ctx.getModel();
		RDFNode object = null;
		
		value = processWhitespace(node,value,itemType,ctx);

		if (itemType!=null && itemType.equals(XSD_URI+"#QName")) {
			String name = expandQName(ctx.getDefaultNS(),null,value,node,ctx.getModel());
			if (name==null) return null; // one bad value invalidates the entire list
			else object = m.createResource(name);
		}
		else if (itemType!=null && itemType.equals(XSD.IDREF.getURI())) {
			if (value.indexOf(':')>=0) return null; // avoid confusion with QNames
			object = ctx.getModel().createResource(addFragment(ctx.getBaseMap(), value).toString());
		}
		
		else if (itemType!=null && itemType.equals(XSD_URI+"#anyURI")) {
			URI uri = null;
			if (isValidURI(value)) uri = new URI(value);
			// ensure this isn't a QName
			else if (value.indexOf(":")<0 && Character.isJavaIdentifierStart(value.charAt(0)))
				uri = resolveBase(node, new URI(value), ctx);
			if (uri!=null) object = m.createTypedLiteral(uri.toString(),dt);
			else return null;
		}
		else if (value!=null && dt!=null && dt.isValid(value)) object = m.createTypedLiteral(value, dt);
		else object = m.createLiteral(value==null?"":value);
		
		// add the value to the list
		if (list.isEmpty()) list = ctx.getModel().createList(new RDFNode[] {object});
		else list.add(object);
		return list;		
	}

	public static URI resolveBase(Node node, URI uri, Context ctx) {
		// get base from instance document
		while (node!=null && node.getNodeType()!=Node.DOCUMENT_NODE) {
			String base = node.getBaseURI();
			if (base!=null) try {
				uri = new URI(base).resolve(uri);
				if (uri.toString().startsWith("http://")) return uri;
			} catch (URISyntaxException e) {
				Gloze.logger.warn("bad base: "+base);
			}
			node = node instanceof Attr?((Attr) node).getOwnerElement():node.getParentNode();
		}
		// default base setting?
		return ctx.getBaseMap().resolve(uri);
	}
		
	// convert a datatyped value into an XML text value
	
	public boolean toXMLText(Element element, RDFNode rdf, String type, String pack, Context ctx) {
		String v;
		Document doc = element.getOwnerDocument();
		
		simpleType s = ctx.getSimpleType(type);
		if (s!=null) return s.toXML(element,rdf,pack,ctx);

		if (type!=null && type.equals(XSD_URI+"#IDREFS") && rdf instanceof Resource
				&& ((Resource)rdf).hasProperty(RDF.first)
				&& rdf.canAs(RDFList.class)) {
			RDFList l = (RDFList) rdf.as(RDFList.class);
			for (ExtendedIterator i=l.iterator(); i.hasNext();) {
				v = toXMLValue(element, (RDFNode) i.next(), XSD.IDREF.getURI(), ctx);
				if (v==null) return false; // failed for this type
				element.appendChild(doc.createTextNode(pack==null?v:pack+v));
				pack = " ";
			}
			return true;
		}
		String val = toXMLValue(element, rdf, type, ctx);
		if (val!=null) {
			element.appendChild(doc.createTextNode(pack==null?val:pack+val));
			return true;
		}
		return false;
	}
	
	public boolean listToXML(Attr attr, RDFList list, String itemType, Context ctx) throws Exception {
		for (ExtendedIterator i=list.iterator(); i.hasNext();) {
			if (!toXML(attr,(RDFNode) i.next(),itemType,ctx)) return false;
		}
		return true;
	}
	
	public String listToString(Resource r, Element elem, String type, Context ctx) {
		String v = null;
		if (r.canAs(RDFList.class)) {
			RDFList l = (RDFList) r.as(RDFList.class);
			for (ExtendedIterator i=l.iterator(); i.hasNext();) {
				String s1 = toXMLValue(elem,(RDFNode) i.next(),type,ctx);
				v = v==null?s1:v+ " "+s1;
			}
		}				
		return v;
	}

	/** convert a datatyped value into an XML attribute value */
	
	public boolean toXML(Attr attr, RDFNode rdf, String type, Context ctx) throws Exception {
		Element e = attr.getOwnerElement();
		String a = attr.getValue();
		String v = toXMLValue(e, rdf, type, ctx);
		if (v!=null) {
			if (a==null || a.equals("")) attr.setValue(v);
			else attr.setValue(a+" "+v);
			return true;
		}
		return false;
	}

	public String toXMLValue(Element elem, RDFNode rdf, String type, Context ctx) {
		String v = null;
		if (rdf instanceof Resource) {
			Resource r = (Resource) rdf;
			
			if (type!=null && (type.equals(XSD_URI+"#QName") || type.equals(XSD_URI+"#NOTATION")) && !r.isAnon()) 
				v = r.getURI();
			else if (type!=null && type.equals(XSD_URI+"#ID") && !r.isAnon())
				v = r.getLocalName();
			else if (type!=null && type.equals(XSD.IDREF.getURI()) && !r.isAnon()
					&& XMLBean.equalNS(r.getNameSpace(),ctx.getBaseMap().toString())) 
				v = r.getLocalName();
			else if (type!=null && type.equals(XSD_URI+"#IDREFS")) {
				v = listToString(r,elem,XSD.IDREF.getURI(),ctx);
			}
			else if (type!=null && type.equals(XSD_URI+"#NMTOKENS")) {
				v = listToString(r,elem,XSD.NMTOKEN.getURI(),ctx);
			}
			else if (r.hasProperty(RDF.value)) {
				Statement stmt = r.getProperty(RDF.value);
				if (type!=null && type.equals(XSD_URI+"#IDREF"))
					v = stmt.getResource().getLocalName();
				else if (stmt.getObject() instanceof Literal) v = stmt.getString();		
			}			
			else if (r.canAs(RDFList.class)) {
				v = listToString(r,elem,null,ctx);
			}
		}
		else if (rdf instanceof Literal) {
			v = ((Literal) rdf).getString();
		}
		
		if (type!=null && type.equals(XSD_URI+"#QName"))
			v = contractQName(v, elem, ctx.getModel(), ctx.getBaseMap());
		
		else if (type!=null && (type.equals(XSD_URI+"#anyURI") || type.equals(XSD_URI+"#NOTATION")))
			try {
				v = relativize(ctx.getBaseMap(),new URI(v)).toString();
			} catch (Exception e) {
				// non-fatal
			}
 
		return v;
	}
	
	// precompiled time patterns
	
	static Pattern durationPattern = Pattern.compile("P((\\d+)Y)?((\\d+)M)?((\\d+)D)?T((\\d+)H)?((\\d+)M)?((\\d+)S)?");
	
	/** map duration into separate dayTimeDuration and yearMonthDuration typed literals */
	
	static RDFNode yearMonthDuration(Model mod, String value) {
		Matcher m = durationPattern.matcher(value);
		if (m.find()) {
			String ym = "P"+
			 (m.group(1)!=null?m.group(1):"")+
			 (m.group(3)!=null?m.group(3):"");
			// if both components are missing return null
			if (!ym.equals("P"))
				return mod.createTypedLiteral(ym, XSD_URI+"#yearMonthDuration");
		}
		return null;
	}

	static RDFNode dayTimeDuration(Model mod, String value) {
		Matcher m = durationPattern.matcher(value);
		if (m.find()) {
			String dt = "P"+
			 (m.group(5)!=null?m.group(5):"")+"T"+
			 (m.group(7)!=null?m.group(7):"")+
			 (m.group(9)!=null?m.group(9):"")+
			 (m.group(11)!=null?m.group(11):"");
			// if all components are missing return null
			if (!dt.equals("P"))
				return mod.createTypedLiteral(dt, XSD_URI+"#dayTimeDuration");
		}
		return null;
	}
	
	/** parse time datatypes into their java counterparts */
	
	public static XMLGregorianCalendar getCalendar(String value) {
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(value);
		}
		catch (Exception e) {
			return null;
		}		
	}
	

	public static Duration getDuration(String value) {
		try {
			return DatatypeFactory.newInstance().newDuration(value);
		}
		catch (Exception e) {
			return null;
		}
	}

	public static String normalizeString(String value) {
		value = value.replaceAll("\t"," ");
		value = value.replaceAll("\n"," ");
		value = value.replaceAll("\r"," ");
		return value;
	}
	
	// if an owl target file is supplied the output is saved to file
	// if the request is deep map imported and included schema
	// if the target is null and deep then embed the sub-schema
	// pass the schema location relative to the base
	
	public OntModel toOWL(File target, String location, boolean redefinition, Context ctx) 
	throws Exception {
		if (processed) return ont;
		// ontology header resource
		Ontology ontology = null; 
		if (ctx.getBaseMap()!=null && !redefinition) {
			ontology = ont.getOntology(ctx.getBaseMap().toString()) ;
			if (ontology==null) ontology = ont.createOntology(ctx.getBaseMap().toString());
		}
		
		// add named namespace declarations before anonymous
		// this takes user specified prefix in preference to generated prefix
		declareGlobalNS(ont, ctx);

		// process schema annotation
		for (int i=0; ontology!=null && annotation!=null && i<annotation.length; i++)
			annotation[i].toOWL(ontology,ctx);

		// process included schema
		for (int i=0; include!=null && i<include.length; i++)
			include[i].toOWL(target,location,ctx);

		// process imported schema
		for (int i=0; _import!=null && i<_import.length; i++)
			_import[i].toOWL(target,location,ctx);

		// process redefined schema
		for (int i=0; redefine!=null && i<redefine.length; i++)
			redefine[i].toOWL(ont,target,location,ctx);
		
		if (!ctx.isOverwrite() && target!=null && target.exists()) {
			ont.getDocumentManager().setProcessImports(false);
			Gloze.logger.info("reading "+target.getPath());
			ont.read(new FileInputStream(target), ctx.getBaseMap().toString());

			// sanity check - side-effect adds ont to the global model
			for (ResIterator ri = ont.listSubjectsWithProperty(RDF.type, OWL.Class); ri.hasNext(); )
				ctx.assertOK(ri.nextResource());		
			if (ctx.get_class().equals("intersectionOf"))
				ctx.mergeModel(ont.getBaseModel(), true, new HashMap<RDFNode,RDFNode>());
		}
		else { // create the ontology			
			// global complex types
			for (int i=0; complexType!=null && i<complexType.length; i++)
				complexType[i].toOWL(null,false,ctx);
			// global simple types
			for (int i=0; simpleType!=null && i<simpleType.length; i++)
				simpleType[i].toOWL(ctx);
			// global elements
			for (int i=0; element!=null && i<element.length; i++)
				element[i].toOWL(null,1,1,ctx);
			// global attributes
			for (int i=0; attribute!=null && i<attribute.length; i++)
				attribute[i].toOWL(null,ctx);
			// groups
			for (int i=0; group!=null && i<group.length; i++)
				group[i].toOWL(null,1,1,ctx);
			// attribute groups
			for (int i=0; attributeGroup!=null && i<attributeGroup.length; i++)
				attributeGroup[i].toOWL(null,ctx);
			
			// sanity check - side-effect adds ont to the global model
			//if (ctx.get_class().equals("intersectionOf"))
			ctx.mergeModel(ont.getBaseModel(), true, new HashMap<RDFNode,RDFNode>());

			removeOrphans(ont, OWL.Class);
			removeOrphans(ont, OWL.Restriction);

			// output
			
			// set base
			RDFWriter writer = ont.getWriter(ctx.getLang());
			writer.setProperty("xmlbase", ctx.getBaseMap().toString());
	
			if (target!=null) {
				if (ctx.isVerbose()) Gloze.logger.info("writing "+target.getPath());
				write(ont, writer, target, ctx.getLang(), ctx.getBaseMap().toString());
			}
			else if (!ctx.isSilent()) {
				writer.write(ont.getBaseModel(),System.out, ctx.getBaseMap().toString());
				System.out.println();
			}
		}
		
		
		// save model in the cache in case it is included/imported elsewhere
		ctx.setOntModel(this,ont);
		processed = true;
		return ont;
	}
	
	void removeOrphans(OntModel ont, Resource type) {
		boolean b = true;
		// removing orphans may create orphans
		while (b) {
			b = false;
			// orphaned classes
			for (ResIterator classes = ont.listSubjectsWithProperty(RDF.type,type); classes.hasNext(); ) {
				Resource c = classes.nextResource();
				if (c.isAnon()) {
					// an orphan is anonymous and has no referee other than itself
					Resource r = null;
					Statement s = null;
					for (StmtIterator si = ont.listStatements(null,null,c); si.hasNext(); ) {
						s = si.nextStatement();
						r = s.getSubject();
						if (!r.equals(c)) break;
					}					
					if (r==null || r.equals(c)) {
						b = true;
						c.removeProperties();
					}
				}
			}
		}
	}

	public static void cleanup(Resource type) {
		if (type!=null && !type.isAnon() && type.getURI().startsWith(RDFS.getURI())) return;
		Set<Resource> remove = new HashSet<Resource>();
		for (StmtIterator i = type.listProperties(RDFS.subClassOf); i.hasNext();) {
			Resource r = i.nextStatement().getResource();
			if (r.isAnon()) remove.add(r);
		}			
		for (Resource r: remove) r.removeProperties();
	}

	public void declareGlobalNS(Model model, Context ctx) {
		addPrefix("rdf",RDF.getURI(),model);
		if (_node instanceof Element) {
			NamedNodeMap m = ((Element)_node).getAttributes();
			for (int i=0; i<m.getLength(); i++) {
				Attr a = (Attr) m.item(i);
				if (a.getName().startsWith("xmlns:")) {
					String ns = a.getValue();
					addPrefixes(a.getName().substring("xmlns:".length()),ns,model);
				}
			}		
		}
		
		// add anonymous namespace declaration		
		if (_node instanceof Element) {
			NamedNodeMap m = ((Element)_node).getAttributes();
			for (int i=0; i<m.getLength(); i++) {
				Attr a = (Attr) m.item(i);
				if (a.getName().equals("xmlns")) {
					String ns = a.getValue();
					if (!model.getNsPrefixMap().containsValue(terminateNS(ns)))
						addPrefix(ctx.createNSPrefix(),ns,model);
					break;
				}
			}		
		}

		// user specified default namespace
		String ns  = ctx.getDefaultNS();
		if (ns!=null && !model.getNsPrefixMap().containsValue(terminateNS(ns))) 
				addPrefix(ctx.createNSPrefix(),ns,model);
		
		// target namespace
		String tns = getTargetNamespace();
		if (tns!=null && !model.getNsPrefixMap().containsValue(terminateNS(tns))) 
			addPrefix(ctx.createNSPrefix(),tns,model);
	}

	private void write(OntModel ont, RDFWriter writer, File target, String lang, String base) throws IOException {
		File dir = target.getCanonicalFile();
		if (!dir.isDirectory()) dir = dir.getParentFile();
		if (!dir.exists()) dir.mkdirs();
		writer.write(ont.getBaseModel(), new FileWriter(target), base);
	}
	
	/** Is this a valid data-type rather than an object type **/
	
	// we allow xs:ID so we can delete related cardinality restrictions
	public static boolean  isValidDatatype(String type) {
		if (type==null) return false;
		
		// All the XSD types below may be object rather than data types
		
		if (type.equals(schema.XSD_URI+"#anyType") ||
			// structured type
			type.equals(schema.XSD_URI+"#duration") ||
			// list type
			type.equals(schema.XSD_URI+"#IDREFS") ||
			type.equals(schema.XSD_URI+"#ENTITIES") ||
			type.equals(schema.XSD_URI+"#NMTOKENS") ||
			// object reference
			type.equals(schema.XSD_URI+"#IDREF") ||
			type.equals(schema.XSD_URI+"#QName") ||
			type.equals(schema.XSD_URI+"#NOTATION")) {
			
			return false;
		}
		else return true;
	}

	/* set ontology property type (inclusively object or datatype) */
		
	public static void defineType(Property prop, String type) {
		if (type==null || type.equals(schema.ID) || !type.startsWith(schema.XSD_URI) || prop==null) return;
		if (isValidDatatype(type)) prop.addProperty(RDF.type,OWL.DatatypeProperty);
		else if (!schema.anyType.equals(type)) prop.addProperty(RDF.type,OWL.ObjectProperty);
	}

	/* return a resource representing the type */
 
	public static Resource toOWL(OntModel ont, String type) {
		if (type==null) return null;
		if (type.equals(schema.anySimpleType)) return RDFS.Literal;
		else if (type.equals(schema.ENTITY)) return ont.getResource(RDF.getURI()+"XMLLiteral");
		else if ( // structured type
			type.equals(schema.XSD_URI+"#anyType") ||
			type.equals(schema.XSD_URI+"#duration")) {
			return null;
		}
		// list type
		else if (type.equals(schema.XSD_URI+"#IDREFS"))
			return RDF.List;
		else if (type.equals(schema.XSD_URI+"#ENTITIES"))
			return toList(ont,type,schema.XSD_URI+"#ENTITY");
		else if (type.equals(schema.XSD_URI+"#NMTOKENS"))
			return toList(ont,type,schema.XSD_URI+"#NMTOKEN");
			
		else if ( // object reference
			type.equals(schema.XSD_URI+"#IDREF") ||
			type.equals(schema.XSD_URI+"#QName") ||
			type.equals(schema.XSD_URI+"#NOTATION")) {
			return null;
		}		
		else return ont.getResource(type);
	}
	
	public static boolean isID(Resource type) {
		if (!type.isAnon()) return type.getURI().equals(schema.ID);
		for (StmtIterator i = type.listProperties(RDFS.subClassOf); i.hasNext();) {
			Resource r = i.nextStatement().getResource();
			if (!type.equals(r) && isID(r)) return true;
		}
		return false;	
	}
	
	public static Resource toList(OntModel ont, String type, String itemType) {
		OntClass c = ont.createClass(type);
		Resource i = ont.getResource(itemType);
		c.addSuperClass(RDF.List);			
		c.addSuperClass(ont.createAllValuesFromRestriction(null,RDF.first,i));
		c.addSuperClass(ont.createAllValuesFromRestriction(null,RDF.rest,c));
		return c;		
	}
	
	public static void removeSubClass(OntModel ont, Resource c, Resource d) {
		ont.remove(ont.createStatement(c,RDFS.subClassOf,d));
	}

	public complexType[] getComplexType() {
		return complexType;
	}

	public element[] getElement() {
		return element;
	}

	public simpleType[] getSimpleType() {
		return simpleType;
	}

	public void setComplexType(complexType[] types) {
		complexType = types;
	}

	public void setElement(element[] elements) {
		element = elements;
	}

	public void setSimpleType(simpleType[] types) {
		simpleType = types;
	}

	public String getElementFormDefault() {
		return elementFormDefault;
	}

	public String getTargetNamespace() {
		return targetNamespace;
	}

	public void setElementFormDefault(String string) {
		elementFormDefault = string;
	}

	public void setTargetNamespace(String string) {
		//namespace = 
		targetNamespace = string;
	}

	public attribute[] getAttribute() {
		return attribute;
	}

	public void setAttribute(attribute[] attributes) {
		attribute = attributes;
	}

	public String getAttributeFormDefault() {
		return attributeFormDefault;
	}

	public void setAttributeFormDefault(String string) {
		attributeFormDefault = string;
	}

	public String getBlockDefault() {
		return blockDefault;
	}

	public String getFinalDefault() {
		return finalDefault;
	}

	public void setBlockDefault(String string) {
		blockDefault = string;
	}

	public void setFinalDefault(String string) {
		finalDefault = string;
	}

	public Import[] get_import() {
		return _import;
	}

	public void set_import(Import[] imports) {
		_import = imports;
	}

	public attributeGroup[] getAttributeGroup() {
		return attributeGroup;
	}

	public void setAttributeGroup(attributeGroup[] groups) {
		attributeGroup = groups;
	}

	public Include[] getInclude() {
		return include;
	}

	public void setInclude(Include[] include) {
		this.include = include;
	}
	
	public void set_owner(XMLBean bean) {
		_owner = bean;
	}

	public group[] getGroup() {
		return group;
	}

	public void setGroup(group[] group) {
		this.group = group;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public URL get_url() {
		return _url;
	}

	public void set_url(URL _url) {
		this._url = _url;
	}

	public redefine[] getRedefine() {
		return redefine;
	}

	public void setRedefine(redefine[] redefine) {
		this.redefine = redefine;
	}

	public annotation[] getAnnotation() {
		return annotation;
	}

	public void setAnnotation(annotation[] annotation) {
		this.annotation = annotation;
	}

}
