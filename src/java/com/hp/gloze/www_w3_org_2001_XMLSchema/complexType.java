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
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hp.gloze.Content;
import com.hp.gloze.Context;
import com.hp.gloze.Gloze;
import com.hp.gloze.Restrictions;
import com.hp.gloze.XMLBean;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.UnionClass;
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
import org.apache.jena.vocabulary.RDFS;

/*! \page complexType complexType

 The only complex type predefined in XML schema is anyType, the default type for elements, and a super-class of any user-defined type. 
 The nearest analogue is RDF Resource.
 User defined complex types correspond to OWL classes which may be anonymous, globally qualified by the target namespace, or locally named and defined in a default namespace. 
 For complex types with mixed or simple content, we model these as restrictions on its RDF value.
		
	Global, named complex types are defined in the target namespace. The following schema defines a complex type,
	<http://example.org/Foo>.
	
	\include complexType1.xsd
	\include complexType1.owl
	
	If the target namespace ends with an alpha-numeric, a fragment separator '#' is introduced.
	
 For locally defined elements we derive value (OWL allValuesFrom) restrictions from their type.
 A complex type defines a content model (particle) for element content. 
 For global elements the type is set by the property range. 
 The Russian Doll style of schema mirrors the structure of the XML instance document. 
 Short on references to global definitions, we see attributes & elements defined in-situ (locally), where names are easily recycled. 
 We might foresee a problem with different appearances of an element within the particle having different types. 
 However, the Element Declarations Consistent constraint limits elements within the same particle to be of the same type. 
 This permits us to construct an allValuesFrom restriction based only on the first appearance of an element within a particle.
  
	This example also shows the definition of a property range based on an anonymous complex type.
	It also shows how simple content is captured in the form of an rdf:value. In this case the element value can appear at most once. 
	If the element is empty the value isn't added.
	
	\include complexType2.xsd
	\include complexType2.owl
	
	Instances of this in both XML and RDF are as follows:
	
	\include complexType2.xml
	\include complexType2.n3
	
	We may add attributes to a complex type either directly as in the example below (or as part of an attribute group),
	or within nested restrictions or extensions.
	
	\include complexType3.xsd
	
	The attribute 'bar' is unqualified, so the corresponding property is defined in the default namespace 
	(xmlns=http://example.org/def/).
	
	\include complexType3.owl
	
	We can add structured content to a complex type, using the compositors, 'all', 'choice', 'sequence', and sometimes 'group'. 
	These don't add any nested structure to the class itself but are used to determine the cardinalities of any
	added properties.

\section complexTypeChildren Child components

- \ref simpleContent
- \ref complexContent
- \ref group
- \ref all
- \ref choice
- \ref sequence
- \ref attribute
- \ref attributeGroup
- \ref anyAttribute
- \ref annotation

*/

public class complexType extends Content {

	private String _abstract="false", id, mixed="false", name, _final;
	private simpleContent simpleContent;
	private complexContent complexContent;
	private group group;
	private all all;
	private choice choice;
	private sequence sequence;
	private attribute[] _attribute;
	private attributeGroup[] attributeGroup;
	private anyAttribute anyAttribute;
	private annotation annotation;

	public complexType() throws IntrospectionException {
	}
	
	/** add attributes from the XML source to the complexType bean **/

	public boolean addAttributes(XMLBean bean, NamedNodeMap map)
		throws Exception {
		for (int i = 0; i < _attribute.length; i++) {
			Node n;
			if ((n = map.getNamedItem(_attribute[i].getName())) != null) {
				_attribute[i].addAttribute((Attr) n);
			} else Gloze.logger.warn("can't add attribute");
		}
		return true;
	}
	
	public String createURI(Model model, Context ctx) {
		schema xs = (schema) this.get_owner();
		return xs.expandName(getName(),model,ctx);
	}

	public boolean needSeq(Set<String> names, Context ctx) {
		if (mixed.equals("true")) return true;
		return super.needSeq(names, ctx);
	}
	
	/** complex type */
	
	public int toRDF(Resource subject, Element elem, int index, Seq seq, Set<restriction> restrictions, boolean addType, Context ctx)
		throws Exception {
		Model m = subject.getModel();
		schema xs = (schema) this.get_owner();
		
		if (name != null && addType)
			subject.addProperty(RDF.type, m.createResource(createURI(m,ctx)));
		
		// add the attributes
		if (_attribute!=null) xs.toRDF(subject, elem, _attribute, ctx);
		
		for (int i = 0; attributeGroup != null && i < attributeGroup.length; i++)
			attributeGroup[i].toRDF(subject, elem, restrictions, ctx);

		// add any attribute
		if (anyAttribute != null)
			anyAttribute.toRDF(subject, elem, null, ctx);
		
		// that's it for inheritable content
		if (restrictions!=null) return index;
		
		// add all
		if (group != null)
			index = group.toRDF(subject, elem, index, seq, mixed.equals("true"), ctx);

		// add all
		else if (all != null)
			index = all.toRDF(subject, elem, index, seq, mixed.equals("true"), ctx);

		// add sequence
		else if (sequence != null)
			index = sequence.toRDF(subject, elem, index, seq, mixed.equals("true"), ctx);
			
		// add choice
		else if (choice != null)
			index = choice.toRDF(subject, elem, index, seq, mixed.equals("true"), ctx);
			
		// add simple content
		else if (simpleContent != null) {
			// simple content can't appear in the context of a sequence
			// if there's a problem, remove the problematic content
			if (!simpleContent.toRDF(subject, elem, ctx)) subject.removeProperties();
		}
		// add complex content
		else if (complexContent != null)
			index = complexContent.toRDF(subject, elem, index, seq, mixed.equals("true"), restrictions, ctx);
		
		// add text content
		if (mixed.equals("true")) return schema.textToRDF(subject, seq, elem, index, ctx);
		return index;
	}
	
	/** simple content **/
	
	public boolean toRDF(Resource subject, Property prop, Node node, String value, Seq seq, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		if (simpleContent!=null) return simpleContent.toRDF(subject,prop,node,value,seq,restrictions,ctx);
		return false;
	}

	public boolean toXML(Element e, RDFNode rdf, Set<Statement> pending, Context ctx) {
		if (simpleContent!=null) return simpleContent.toXML(e,rdf,pending,ctx);
		return false;
	}	

	/*
	 * done includes consumed statements
	 * names includes the element names in this content 
	 * both super-content (inherited) and sub-content (sub-choice, sub-seq, sub-all)
	 */
	public int toXML(Element e, Resource rdf, int index, Set<Statement> pending, Context ctx) {
		return toXML(e,rdf,index,pending,false,ctx);
	}

	public int toXML(Element e, Resource rdf, int index, Set<Statement> pending, boolean extension, Context ctx) {
		schema xs = (schema) this.get_owner();
		complexType x = extension(pending, ctx);
		if (x!=null) return x.toXML(e,rdf,index,pending,true,ctx);
		
		for (int i = 0; _attribute != null && i < _attribute.length; i++)
			_attribute[i].toXML(e, rdf, pending, ctx);
		
		for (int i = 0; attributeGroup != null && i < attributeGroup.length; i++)
			attributeGroup[i].toXML(e, rdf, pending, ctx);
		
		if (anyAttribute!=null) anyAttribute.toXML(e,rdf,pending,ctx);

		if (all != null) index = all.toXML(e, rdf, index, pending,ctx);
		else if (sequence != null) index =  sequence.toXML(e, rdf, index, pending,ctx);
		else if (choice != null) index = choice.toXML(e, rdf, index, pending,ctx);
		else if (group != null) index = group.toXML(e, rdf, index, pending,ctx);
		else if (simpleContent!=null) simpleContent.toXML(e,rdf,pending,ctx);
		else if (complexContent!=null) index = complexContent.toXML(e,rdf,index, pending,ctx);
		
		if (mixed.equals("true")) index = textToXML(e,rdf,index,pending,ctx);
		
		// are there subclasses that could consume more properties
		if (extension) {
			// names are declared in the target namespace
			String name = xs.qualifiedName(getName(), ctx.getAttributeSymbol(),ctx.getModel());
			e.setAttributeNS(schema.XSI,"xsi:type",contractQName(name,e,ctx.getModel(),ctx.getBaseMap()));
		}		
		return index;
	}
	
	protected complexType extension(Set<Statement> pending, Context ctx) {
		complexType complex = null;
		Model m = ctx.getModel();
		Set<Statement> done = new HashSet<Statement>();
		done: for (Iterator i = pending.iterator(); i.hasNext(); ) {
			Statement stmt = (Statement) i.next();
			if (stmt.getPredicate().equals(RDF.type)) {
				Resource type = (Resource) stmt.getObject();
				// ignore RDF types, e.g. rdf:Seq, or URI of this type
				if (type.getURI().startsWith(RDF.getURI())
				 || type.getURI().equals(createURI(m,ctx))) {
					done.add(stmt);
					continue;
				}
				complexType c = ctx.getComplexType(type.getURI());
				// map direct ONLY descendents of thisclass to XML
				if (c!=null && c.subtype(createURI(m,ctx), m,ctx)) {
					done.add(stmt);
					complex = c;
					break done;
				}
			}
		}
		pending.removeAll(done);
		return complex;
	}

	public void assertType(Resource subject, Context ctx) {
		Model m = subject.getModel();
		subject.addProperty(RDF.type, m.createResource(createURI(m,ctx)));
		if (complexContent!=null) complexContent.assertType(subject,ctx);
	}
	
	public boolean subtype(String type, Model model, Context ctx) {
		if (complexContent!=null) return complexContent.subtype(type, model, ctx);
		else return false;
	}

	private int textToXML(Element e, Resource rdf, int index, Set<Statement>pending, Context ctx) {
		Document doc = e.getOwnerDocument();
		// consume ordinal (indexed) statements
		Seq seq = rdf.getModel().getSeq(rdf);
		while (index<seq.size()) {
			Statement s = (Statement) asStatement((Resource) seq.getObject(index+1));
			if (s.getPredicate().equals(RDF.value))
				e.appendChild(doc.createTextNode(s.getLiteral().getString()));
			else return index;
			index++;
		}
		// consume non-ordinal (pending) statements
		Set<Statement> done = new HashSet<Statement>();
		for (Statement s: pending) {
			if (s.getPredicate().equals(RDF.value)) {
				e.appendChild(doc.createTextNode(s.getLiteral().getString()));
				done.add(s);
			}
		}
		pending.removeAll(done);
		return index;
	}
	
	public attribute getNamedAttribute(String name) {
		attribute[] a = getAttribute();
		for (int i = 0; a != null && i < a.length; i++) {
			if (a[i].getName().equals(name))
				return a[i];
		}
		return null;
	}

	/** return the ID if defined, may be attribute, simpleContent */
	
	public String getID(Element element, Context ctx) {
		if (element.hasAttribute("xml:id")) return element.getAttribute("xml:id");
		// check the attributes for ID
		NamedNodeMap m = element.getAttributes();
		// there may be no attributes defined (check for null attribute)
		for (int i = 0; _attribute != null && i < _attribute.length; i++) {
			String id = null;
			Attr a = (Attr) m.getNamedItem(_attribute[i].getDefinition(ctx.getModel(),ctx).getName());
			// the attribute may not have any occurrences
			if (a != null) id = _attribute[i].getID(a,ctx);
			if (id != null) return id;
		}
		// look for attributes embedded within simple content declaration
		if (simpleContent!=null) return simpleContent.getID(element, ctx);
		return null;
	}
	
	/*! \page cardinality Cardinality Restrictions
	 By looking at attribute usage and element occurrence we can derive the appropriate OWL cardinality restrictions.
	 Cardinality restrictions are derived from occurrence constraints on particles. 
	 For straightforward content there is almost a direct mapping from occurrences to cardinality. 
	 Elements occur once by default, and attributes are optional. 
	 We can also change minOccurs and maxOccurs on an element and this will be directly reflected in the OWL cardinality constraint. 
	 However, this belies a significant difference between occurrence constraints and cardinality restrictions.
     An occurrence constraint in XML schema refers to the occurrence of content at a given lexical position in the document. 
     Similar content may occur at different positions within the same particle. 
     In contrast, Cardinality restrictions limit the total number of appearances of a property in the context of a given class.
	 
	 The following example shows how cardinality restrictions are derived from occurrence constraints on complex content.
	 The content of the 'all' compositor and sub-element 'foo' occur once
	 by default. The maximum number of occurrences of 'bar' is unbounded. 
	 By default, the use of attribute 'baz' is optional.
	 
	 \include cardinality1.xsd
	 
	 Cardinality restrictions only constrain the number of occurences of a property in the context of a given class.
	 They don't affect global property definitions. Property 'foo' is restricted to a cardinality of 1, while property 'bar' has a \em minimum cardinality of 1
	 (the maximum is unlimited).
	 The optional attribute 'baz' has a maximum cardinality of 1 - occurring at most once.
	 Note also, that because 'baz' is a locally defined attribute, it alone has an additional value constraint.
	 The type of 'baz' is local to the class, whereas the ranges of 'foo' and 'bar' are globally defined.
	 
	 \include cardinality1.owl
	 
	 An occurrence constraint in XML schema refers to the occurrence of content at a particular lexical position
	 in the document. Similar content may occur at a different position <em>within the same particle</em>.
	 This is demonstrated in the following example, where a single element 'foo' may be followed (in sequence)
	 by yet another element 'foo'. 
	 Because these are both properties of the same resource we calculate the cardinality of 'foo'
	 by summing repeated occurrences.
	 
	 \include cardinality2.xsd
	 
	 Summing the occurrences of 'foo' makes for a cardinality of 2, right? 
	 Wrong. The OWL below has a \em maximum cardinality of 2, but a minimum cardinality of 1.
	 
	 \include cardinality2.owl
	 
	 There is an edge case where there may be two occurrences of 'foo' but only one \em distinct property/value pair.
	 This happens if both occurrences have the same literal value, as demonstrated below. Both occurrences map to the
	 same statement [ ns1:foo "foo"^^xs_:string ] (with the same subject). 
	 Because these are \em logical statements it makes no difference
	 how many times they are asserted, it amounts to saying the same thing twice. 
	 The actual cardinality of 'foo' in this case is 1.
	 Because of this, the minimum cardinality of any datatype property will never exceed 1.
	 
	 \include cardinality2.xml
	 
	 The two elements above amount to saying the same thing twice.
	 
	 \include cardinality2.n3
	 
	 Does this mean we lose information and can't achieve the reverse mapping back into RDF? 
	 If the number of times something was asserted is significant we have to record the sequence in which they occur. The example below shows how each
	 occurrence of the element has been \em reified, making each an objectified resource in its own right, with its own identity. 
	 Each reified 'foo' is added to the object (of 'foofoo') as an rdf:Seq in their correct lexical order. With this
	 additional metadata we see that there are two identical statements.
	 This sequencing information is viewed as a data-structuring issue and is not modelled ontologically. 
	 
	 \include cardinality2a.n3
	 
	 Compositors like sequence, choice, all (and group references) are not directly represented in OWL because they are concerned with the lexical form of a document. 
	 However, their effect is to modulate the occurrences of elements within a particle. 
	 Like elements the maximum is set by maxOccurs, and the minimum by minOccurs. 
	 With nested compositors, the cardinality is calculated by multiplying nested elements by the occurrence constraints on the compositor. 
	 By default, the sequence compositor has a minimum and maximum factor of 1. The choice compositor
	 has a maximum factor of 1, but an overriding minimum factor of 0 because all but one of its elements will not occur at all.
	 
	 \include cardinality3.xsd
	 
	 The minimum cardinality of both 'foo' and 'bar' is 1*0*1 = 0.
	 The maximum cardinality of both 'foo' and 'bar' is 2*2*1 = 4. 
	 
	 \include cardinality3.owl
	 
	 Another feature that will affect cardinality is the appearance of an xs:any wild-card.
	 We have to count how many of these could match other elements in the particle and increment their cardinalities accordingly.
	 The schema below includes an element 'foo' followed by a selection of xs:any elements that may,
	 in principle, match yet another occurrence of 'foo'. We count how many of these may match 'foo'
	 and increment its min and max cardinalities. The last two xs:any cannot match 'foo'.
	 The 'foo' element is defined in the target namespace, but '##other' will only match an
	 element in another namespace, while '##local' will only match a local, unqualified element.
	 
	 \include cardinality4.xsd
	 
	 As for other properties that may potentially match xs:any, there are no specific restrictions
	 that can be derived from the schema. In an open-world we may pass over them in silence.
	 
	 \include cardinality4.owl
	 
	 The final feature relevant to cardinality is the use of substitution groups.
	 
	 \include substitution.xsd
	 
	 The member element 'bar' can substitute for the head element 'foo', so the
	 content may include up to 2 'bar's (we assume there may be other potential sub-properties that could also substitute for 'foo'). 
	 Conversely, as 'bar' is a sub-property of
	 'foo' each 'bar' statement implies a 'foo' statement. There are therefore up to 2 'foo's.
	 Ordinarily, we might conclude that exactly two 'foo's are implied, but there is again the possibility that
	 we might have two identical literal values.

	 \include substitution.owl
	 
	 */
	
	// if restrictions are instantiated they are required by the caller (eg. an extension/restriction)
	
	public Resource toOWL(Restrictions rest, boolean createAnon, Context ctx) {
		schema xs = (schema) this.get_owner();
		String uri = createURI(xs.ont,ctx);

		// create class if this is the first time we've seen it
		Resource c = ctx.getOntClass(uri), cls = c ;
		if (c==null) {
			if (uri!=null) ctx.putOntClass(uri, cls = xs.ont.createClass(uri));
		}
		else if (rest==null) return c;
		if (cls==null) cls = xs.ont.createClass(uri);
		if (cls!=null) cls.addProperty(RDFS.subClassOf,OWL.Thing);

		// restrictions are required internally to define the class, if not by the caller
		if (rest==null && (uri!=null || (uri==null && createAnon))) rest = new Restrictions();

		// declare local elements & attributes, extensions & restrictions (for subclassing below)
		if (sequence!=null) sequence.toOWL(rest, 1, 1, ctx);
		if (choice!=null) choice.toOWL(rest, 1, 1, ctx);
		
		// pass the class down to add subclasses in restrictions / extensions
		if (complexContent!=null) complexContent.toOWL(xs.ont, cls, rest, ctx);
		if (simpleContent!=null) simpleContent.toOWL(xs.ont, cls, rest, ctx);
		
		if (all!=null) all.toOWL(rest, 1, 1, ctx);
		if (group!=null) group.toOWL(rest,1,1,ctx);

		for (int i=0; _attribute!=null && i<_attribute.length; i++)
			_attribute[i].toOWL(rest, ctx);

		for (int i=0; attributeGroup!=null && i<attributeGroup.length; i++)
			attributeGroup[i].toOWL(rest, ctx);

		// only continue with anonymous classes with permission (ie. local to another class)
		if (uri==null && !createAnon) cls = null;
						
		// add cardinality restrictions	
		
		/** Assume at most minCardinality 1 on datatype properties where the same value may recur multiple times. */	
		/** this can also effect the cardinality of IDREFs and QName properties */
		
		else if (rest!=null && c==null) {
			List<OntClass> restrictions = buildRestrictions(rest, ctx);
			if (ctx.get_class().equals("intersectionOf")) {
				if (restrictions.size()>=1) {
					RDFList l = xs.ont.createList(restrictions.iterator());
					if (cls!=null) cls.removeProperties();
					if (uri!=null) ctx.putOntClass(uri, cls = xs.ont.createIntersectionClass(uri,l));
					else if (createAnon) cls = xs.ont.createIntersectionClass(null,l);
				}
			}
			// use this style for subclass compositions
			else {
				if (cls==null && uri==null && createAnon) cls = xs.ont.createClass();
				else if (cls==null && uri!=null) cls = xs.ont.createClass(uri);
				if (cls!=null)
					for (Resource r: restrictions) cls.addProperty(RDFS.subClassOf,r);		
			}
		}
		
		// add subclass relationships
		if (complexContent!=null && cls!=null) complexContent.subClass(cls,ctx);
		if (cls!=null) schema.removeSubClass(xs.ont,cls,OWL.Thing);
		ctx.assertOK(cls);
		
		// add comment
		if (annotation!=null) annotation.toOWL(cls,ctx);
		return cls;
	}

	private List<OntClass> buildRestrictions(Restrictions rest, Context ctx) {
		schema xs = (schema) this.get_owner();
		String tns = xs.getTargetNamespace();
		rest.adjust(xs.ont);
		List<OntClass> restrictions = new Vector<OntClass>();
		// minCardinality (and cardinality)
		for (String key: rest.getMinCard().keySet()) {
			OntModel ont = rest.getModel(key);
			OntProperty p = ont.createOntProperty(key);
			Integer minCard = rest.getMinCard().get(key);
			// maxCard includes occurrences of any
			Integer maxCard = Restrictions.sum(rest.getMaxCard().get(key), rest.getAny(key,tns));
			if (maxCard!=null && minCard==maxCard)
				restrictions.add(xs.ont.createCardinalityRestriction(null,p,minCard));
			else if (minCard>0)
				restrictions.add(xs.ont.createMinCardinalityRestriction(null,p,minCard));
		}
		// maxCardinality
		for (String key: rest.getMaxCard().keySet()) {
			OntModel ont = rest.getModel(key);
			OntProperty p = ont.createOntProperty(key);
			int m = rest.getMaxCard().get(key);
			if (m==Integer.MAX_VALUE) continue;
			Integer minCard = rest.getMinCard().get(key);
			Integer maxCard = Restrictions.sum(rest.getMaxCard().get(key),  rest.getAny(key,tns));
			if (minCard==null || minCard!=maxCard)
				restrictions.add(xs.ont.createMaxCardinalityRestriction(null,p,maxCard));
		}
		
		// add value restrictions		
		for (String key: rest.getRange().keySet()) {
			OntModel ont = rest.getModel(key);
			OntProperty p = ont.getOntProperty(key);
			Set<Resource> s = rest.getRange().get(key);

			if (s.size()==1) {
				Resource t = s.iterator().next();
				
				if (t!=null && ont!=xs.ont) t = copyResource(t);
				// is the range implied by the global property
				if (t!=null && (p.getRange()==null || !p.getRange().equals(t)))
					restrictions.add(xs.ont.createAllValuesFromRestriction(null,p,t));					
			}
			else if (s.size()>1) {
				// create an OWL union
				UnionClass u = xs.ont.createUnionClass(null,xs.ont.createList(s.iterator()));
				restrictions.add(xs.ont.createAllValuesFromRestriction(null,p,u));
			}
		}
		return restrictions;
	}
	
	// copy anonymous resources from different ontology
	Resource copyResource(Resource rez) {
		schema xs = (schema) this.get_owner();
		if (rez.isAnon()) {
			Resource r = xs.ont.createResource();
			for (StmtIterator si = rez.listProperties(); si.hasNext(); ) {
				Statement s = si.nextStatement();
				if (s.getObject().equals(rez)) continue;
				if (s.getObject().equals(RDFS.Resource)) continue;
				if (s.getObject().equals(RDFS.Class)) continue;
				else if (s.getObject().isLiteral())
					r.addProperty(s.getPredicate(),s.getLiteral());
				else 
					r.addProperty(s.getPredicate(), copyResource(s.getResource()));
			}
			return r;
		}
		else return rez;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public sequence getSequence() {
		return sequence;
	}

	public void setSequence(sequence sequence) {
		this.sequence = sequence;
	}

	public anyAttribute getAnyAttribute() {
		return anyAttribute;
	}

	public void setAnyAttribute(anyAttribute anyAttribute) {
		this.anyAttribute = anyAttribute;
	}

	public attribute[] getAttribute() {
		return _attribute;
	}

	public void setAttribute(attribute[] attribute) {
		this._attribute = attribute;
	}

	public all getAll() {
		return all;
	}

	public void setAll(all all) {
		this.all = all;
	}

	public String getMixed() {
		return mixed;
	}

	public void setMixed(String string) {
		mixed = string;
	}

	public String getId() {
		return id;
	}

	public void setId(String string) {
		id = string;
	}

	public complexContent getComplexContent() {
		return complexContent;
	}

	public void setComplexContent(complexContent content) {
		complexContent = content;
	}

	public choice getChoice() {
		return choice;
	}

	public void setChoice(choice choice) {
		this.choice = choice;
	}

	public simpleContent getSimpleContent() {
		return simpleContent;
	}

	public void setSimpleContent(simpleContent content) {
		simpleContent = content;
	}

	public attributeGroup[] getAttributeGroup() {
		return attributeGroup;
	}

	public void setAttributeGroup(attributeGroup[] groups) {
		attributeGroup = groups;
	}
	
	public String get_abstract() {
		return _abstract;
	}
	
	public void set_abstract(String _abstract) {
		this._abstract = _abstract;
	}

	public group getGroup() {
		return group;
	}

	public void setGroup(group group) {
		this.group = group;
	}

	public String get_final() {
		return _final;
	}

	public void set_final(String _final) {
		this._final = _final;
	}

	public annotation getAnnotation() {
		return annotation;
	}

	public void setAnnotation(annotation annotation) {
		this.annotation = annotation;
	}

}
