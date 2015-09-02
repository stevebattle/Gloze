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
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hp.gloze.Context;
import com.hp.gloze.Gloze;
import com.hp.gloze.Restrictions;
import com.hp.gloze.XMLBean;
import org.apache.jena.ontology.DataRange;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/*! \page restriction
 New simple types can be derived by restriction.
 If a complex type is a restriction of another type we can derive a subclass relationship between them.
 Restrictions are easier to translate into OWL than extensions because they only add new constraints, thereby reducing the set of valid instances. 
 The occurrences of elements and attributes may be reduced (though not below the minimum of the parent class) and their types may be narrowed. 
 A restriction defines a subset of instances of its base type.
 OWL may be used to declare restrictions of simple types but is not able to define constraints on the new value space other than by enumeration. 
  
- \subpage restrictionComplexContent "Restriction of Complex Content"
*/


public class restriction extends XMLBean {

	/** a restriction constrains attributes, elements and values
	 * restriction of simple type constrains values 
	 * restriction of simple content constrains attributes and value 
	 * restriction of complex content constrains attributes and elements
	 */

	private String base, id;
	private whiteSpace whiteSpace;
	private attribute[] _attribute;
	private attributeGroup[] attributeGroup;
	private anyAttribute anyAttribute;
	private enumeration[] _enumeration;
	private length length;
	private minInclusive minInclusive;
	private maxInclusive maxInclusive;
	private minExclusive minExclusive;
	private maxExclusive maxExclusive;
	private maxLength maxLength;
	private minLength minLength;
	private pattern[] pattern;
	private totalDigits totalDigits;
	private fractionDigits fractionDigits;
	private sequence sequence;
	private simpleType simpleType;
	private group group;
	private all all;
	private choice choice;
	
	public restriction() throws IntrospectionException {
	}

	public void resolve(Model model, Context ctx) {
		ctx.putBase(this,get_baseType(model, ctx));
		super.resolve(model,ctx);
	}
	
	public String getID(Element element, Context ctx) {
		// is the base type xs:ID
		if (base!=null) {
			String uri = expandQName(ctx.getDefaultNS(),base,ctx.getModel());
			if (uri.startsWith(schema.XSD_URI) && uri.endsWith("#ID")) return getValue(element);
		}
		// check the attributes for ID
		NamedNodeMap m = element.getAttributes();
		// there may be no attributes defined (check for null attribute)
		String id=null;
		for (int i = 0; id==null && _attribute != null && i < _attribute.length; i++) {
			String n = _attribute[i].getName();
			Attr a = (Attr) (n==null?null:m.getNamedItem(n));
			// the attribute may not have any occurrences
			id = _attribute[i].getID(a, ctx);
		}
		return id;
	}

	private XMLBean get_baseType(Model model, Context ctx) {
		XMLBean b = ctx.getBase(this);
		if (b!=null) return b;
		if (base!=null) {
			String uri = expandQName(ctx.getDefaultNS(),base,model);
			if (!uri.startsWith(schema.XSD_URI)) {
				b = ctx.getComplexType(uri);
				if (b==null) b = ctx.getSimpleType(uri);
				if (b!=null) ctx.putBase(this,b);
				else Gloze.logger.warn("no such base type: "+uri);
			}
		}
		// may be defined inline
		else if (simpleType!=null) ctx.putBase(this,b = simpleType);
		return b;
	}
	
	/* restriction of simple content */
	
	public boolean toRDF(Resource subject, Node node, String value, Property prop, Seq seq, Set<restriction> restrictions, Context ctx) 
	throws Exception {	
		schema xs = (schema) this.get_owner();

		// attributes with additional restrictions
		if (_attribute!=null ) xs.toRDF(subject, node, _attribute, ctx);
		for (int i = 0; attributeGroup != null && i < attributeGroup.length; i++)
			attributeGroup[i].toRDF(subject,node,restrictions,ctx);	
		if (anyAttribute!=null) anyAttribute.toRDF(subject, node, restrictions, ctx);
				
		// add a restriction set when we pass through a restriction element
		if (restrictions==null) restrictions = new HashSet<restriction>();
		restrictions.add(this);

		// inherit attributes and simple content from the base
		XMLBean b = get_baseType(ctx.getModel(),ctx);
		if (b!=null && b instanceof simpleType) 
			return ((simpleType)b).toRDF(subject,prop,node,value,seq,restrictions,ctx);
		
		else if (b!=null && b instanceof complexType)
			// complex types are OK as long as they also have simple content
			return ((complexType)b).toRDF(subject,prop,node,value,seq,restrictions,ctx);
		
		else {
			String type = expandQName(ctx.getDefaultNS(),base, ctx.getModel());
			if (whiteSpace!=null) return whiteSpace.toRDF(subject,node,value,prop,type,seq,restrictions,ctx);
			value = schema.processWhitespace(node,value,type,ctx);
			return xs.toRDF(node,subject,prop,value,type,seq,restrictions,ctx);
		}
	}

	// restriction of complex content
	// the restriction overrides the content model of the base type
	
	public int toRDF(boolean mixed, Resource subject, Node node, int index, Seq seq, complexContent complex, Set<restriction> restrictions, Context ctx)
	throws Exception {
		schema xs = (schema) this.get_owner();
		
		// assert relationship to base type
		assertType(subject,ctx);

		// add sequences
		if (sequence!=null) index = sequence.toRDF(subject,node,index,seq, mixed,ctx);
		else if (group!=null) index = group.toRDF(subject,node,index,seq,mixed,ctx);
		else if (all!=null) index = all.toRDF(subject,node,index,seq,mixed,ctx);
		else if (choice!=null) index = choice.toRDF(subject,node,index,seq,mixed,ctx);
		
		if (mixed) index = schema.textToRDF(subject, seq, node, index, ctx);

		// add attributes and attribute groups
		if (_attribute!=null) xs.toRDF(subject, node, _attribute, ctx);
		for (int i=0; attributeGroup!=null && i<attributeGroup.length; i++)
			attributeGroup[i].toRDF(subject,node,restrictions,ctx);
		if (anyAttribute!=null) anyAttribute.toRDF(subject, node, null, ctx);
		

		// add a restriction set when we pass through a restriction element
		if (restrictions==null) restrictions = new HashSet<restriction>();
		restrictions.add(this);

		// inherit attributes and simple content from the base
		XMLBean b = get_baseType(ctx.getModel(),ctx);
		if (b!=null && b instanceof simpleType)
			Gloze.logger.error("simpleType in restriction of complex type");
		
		else if (b!=null && b instanceof complexType)
			// complex types are OK as long as they also have simple content
			return ((complexType)b).toRDF(subject,(Element)node,index,seq,restrictions,true,ctx);
		
		else {
			String type = expandQName(ctx.getDefaultNS(),base, ctx.getModel());
			if (type!=null && type.equals(schema.XSD_URI+"#anyType")) {
			}
			else Gloze.logger.error("predefined xsd type in restriction of complex type: "+base);
		}

		return index;
	}
	
	public RDFList toRDFList(Node node, String value, RDFList list, Set<restriction> restrictions, Context ctx) 
	throws Exception {	
		schema xs = (schema) this.get_owner();
		Model m = ctx.getModel();
		// only add restriction if required
		if (restrictions!=null) restrictions.add(this);

		// user-defined simple base
		simpleType s = (simpleType) get_baseType(m,ctx);
		if (s!=null) return s.toRDFList(node,value,list,restrictions,ctx);
		else {
			if (restrictions!=null)
				for (restriction r: restrictions) 
					if (!r.isValid(value,expandQName(ctx.getDefaultNS(),base, m),ctx)) return null;
			
			String type = expandQName(ctx.getDefaultNS(),base, m);
			if (whiteSpace!=null) whiteSpace.toRDFList(node,value,type,list,ctx);
			else return schema.toRDFList(node,value,type,list,restrictions,ctx);
			return list;
		}
	}
	
	/** check validity of simple content */
	
	public boolean isValid(Resource resource, Context ctx) {
		boolean valid = true;
		// check restrictions on attributes
		for (int i=0; _attribute!=null && i<_attribute.length; i++) {
			valid = _attribute[i].isValid(resource, ctx);
		}
		return valid;
	}

	public boolean isValid(String value, String type, Context ctx) {
		return isValid(value,type,(RDFList)null,ctx);
	}
	
	/** is this value a valid instance of this restriction */
	
	boolean isValid(String value, Context ctx) {
		if (base!=null) {
			String type = expandQName(ctx.getDefaultNS(),base, ctx.getModel());
			return isValid(value, type,ctx);
		}
		return true;
	}

	public boolean isValid(String value, String type, RDFList list, Context ctx) {
		if (value==null) return false;
		// ensure value is enumerated
		boolean valid = _enumeration==null && pattern==null;
		for (int i = 0;  !valid && _enumeration!=null && i < _enumeration.length; i++)
			valid=_enumeration[i].isValid(value,type);
		for (int i = 0;  !valid && pattern!=null && i < pattern.length; i++)
			valid=pattern[i].isValid(value,type);

		// these may relate to the length of the sequence
		if (length!=null && !length.isValid(value,type,list)) return false;
		if (minLength!=null && !minLength.isValid(value,type,list)) return false;
		if (maxLength!=null && !maxLength.isValid(value,type,list)) return false;	
		if (minInclusive!=null && !minInclusive.isValid(value,type)) return false;
		if (maxInclusive!=null && !maxInclusive.isValid(value,type)) return false;
		if (minExclusive!=null && !minExclusive.isValid(value,type)) return false;
		if (maxExclusive!=null && !maxExclusive.isValid(value,type)) return false;
		if (totalDigits!=null && !totalDigits.isValid(value,type)) return false;
		if (fractionDigits!=null && !fractionDigits.isValid(value,type)) return false;
		return valid;
	}

	public void assertType(Resource subject, Context ctx) {
		if (base!=null) {
			complexType c = (complexType) get_baseType(ctx.getModel(),ctx);
			if (c!=null) c.assertType(subject, ctx);
		}		
	}

	// restriction of complex content
	
	public int toXML(Element e, Resource rdf, int index, Set<Statement> pending, complexContent complex, Context ctx) {
		return toXML(e,rdf,index,pending,complex,null,ctx);
	}
	
	// restriction of complex content element
	
	public int toXML(Element e, Resource rdf, int index, Set<Statement> pending, complexContent complex, String pack, Context ctx) {
		if (sequence!=null) index = sequence.toXML(e,rdf,index,pending,ctx);
		else if (group!=null) index = group.toXML(e,rdf,index,pending,ctx);
		else if (all!=null) index = all.toXML(e,rdf,index,pending,ctx);
		else if (choice!=null) index = choice.toXML(e,rdf,index,pending,ctx);
		
		for (int i = 0; _attribute != null && i < _attribute.length; i++)
			_attribute[i].toXML(e, rdf, pending,ctx);

		for (int i=0; attributeGroup!=null && i<attributeGroup.length; i++)
			attributeGroup[i].toXML(e,rdf,pending,ctx);

		return index;
	}

	/** restriction of simple type (element) **/

	public boolean toXML(Element e, RDFNode rdf, String pack, Context ctx) {
		schema xs = (schema) this.get_owner();
		XMLBean b = get_baseType(ctx.getModel(),ctx);
		if (b!=null && b instanceof simpleType) 
			return ((simpleType)b).toXML(e,rdf,pack,ctx);
		else return xs.toXMLText(e,rdf,expandQName(ctx.getDefaultNS(),base, ctx.getModel()),pack,ctx);
	}
	
	/** restriction of simple content **/

	public boolean toXML(Element e, RDFNode rdf, Set<Statement> pending, Context ctx) {
		schema xs = (schema) this.get_owner();
		
		// attributes and attribute groups appearing in the restriction must also appear in the base
		// assume anyAttribute is not inherited in the same way

		XMLBean b = get_baseType(ctx.getModel(),ctx);
		if (b!=null && b instanceof simpleType) 
			return ((simpleType)b).toXML(e,rdf,ctx);
		else if (b!=null && b instanceof complexType)
			return ((complexType)b).toXML(e,rdf,pending,ctx);			
		else return xs.toXMLText(e,rdf,expandQName(ctx.getDefaultNS(),base, ctx.getModel()),null,ctx);
	}

	// restriction of simple type (attribute)
	
	public boolean toXML(Attr attr, RDFNode rdf, Context ctx) throws Exception {
		schema xs = (schema) this.get_owner();
		simpleType simple = (simpleType) get_baseType(ctx.getModel(),ctx);
		if (simple!=null) return simple.toXML(attr,rdf,ctx);
		else return xs.toXML(attr,rdf,expandQName(ctx.getDefaultNS(),base, ctx.getModel()),ctx);
	}
		
	public void defineType(Property prop, Context ctx) {
		schema xs = (schema) this.get_owner();
		simpleType s = (simpleType) get_baseType(xs.ont,ctx);
		if (s!=null) s.defineType(prop,ctx);
		else schema.defineType(prop,expandQName(ctx.getDefaultNS(),getBase(), xs.ont));
	}
	
	/* restriction of simple content */

	public Resource toOWLSimpleType(String uri, Context ctx) {
		schema xs = (schema) this.get_owner();
		// get base type
		String base = null;
		simpleType s = (simpleType) get_baseType(xs.ont,ctx);
		if (s!=null) base = s.createURI(xs.ont,ctx);
		else base = expandQName(ctx.getDefaultNS(),getBase(), xs.ont);
		
		if (s!=null && uri==null) return s.toOWL(ctx); 
		
		if (_enumeration!=null && !schema.ID.equals(base)) {
			// use data-range in the context of a property definition
			if (schema.isValidDatatype(base) && uri==null)
				return enumeration.toOWL(xs.ont,base,_enumeration);
			// in OWL full a datarange is a class
			else if (schema.isValidDatatype(base)) {
				OntClass c = xs.ont.createClass(uri);
				DataRange e = enumeration.toOWL(xs.ont,base,_enumeration);
				// assert equivalence with the anonymous datarange
				c.addEquivalentClass(e);
				return c;
			}
			// declare enumerated complex type
			else {
				RDFList e = enumeration.toOWL(xs.ont, _enumeration, _node, ctx);
				return xs.ont.createEnumeratedClass(uri,e);
			}
		}

		if (base!=null) {
			if (schema.isValidDatatype(base)) {
				// pass over anonymous simple types
				if (uri==null) return xs.ont.getResource(base);
				OntClass cls = xs.ont.createClass(uri);
				cls.addSuperClass(xs.ont.getResource(base));
				return cls;
				
			}
			else return null;
		}

		OntClass cls = null;		
		ctx.putOntClass(uri, cls = xs.ont.createClass(uri));
		if (base!=null) cls.addSuperClass(xs.ont.getResource(base));		
		subClassSimpleType(cls,ctx);
		return cls;
	}

	/*! \page restrictionComplexContent Restriction of complex content 
	  
	 Restrictions are easier to map because they only add new constraints, thereby reducing the set of valid instances.
	 The occurrences of elements and attributes may be reduced (though not below the minimum of the parent class)
	 and their types may be narrowed.
	 
	 In the example below, the complex type 'Bar' restricts 'Foo' by reducing the number of occurrences of element 'foo'
	 from unbounded to 1; by narrowing the type from xs:anySimpleType to xs:string; and by disallowing mixed content.
	 
	 \include restriction.xsd
	 
	 Gloze assumes correctness of the schema and asserts the subclass relationship without further checks.
	 
	 \include restriction.owl

	 */
	
	/* restriction of complex content */

	public void toOWLComplexContent(OntModel ont, Resource cls, Restrictions rest, Context ctx) {
		schema xs = (schema) this.get_owner();
		// inherit attributes and simple content from the base
		XMLBean b = get_baseType(ont,ctx);
		if (b!=null && b instanceof simpleType)
			Gloze.logger.error("simpleType in restriction of complex type");

		if (b!=null && b instanceof complexType) ;
		else {
			// xsd type
			String type = expandQName(ctx.getDefaultNS(),base, ont);
			if (rest!=null && type!=null && schema.isValidDatatype(type)){
				rest.addRange(ont,RDF.value.getURI(),type);
				rest.addMin(RDF.value.getURI(),1,true,ont);
				rest.addMax(RDF.value.getURI(),1,ont);
				schema.defineType(ont.createProperty(RDF.value.getURI()),type);
			}
		}
		
		for (int i=0; _attribute!=null && i<_attribute.length; i++)
			_attribute[i].toOWL(rest,ctx);
		for (int i=0; attributeGroup!=null && i<attributeGroup.length; i++)
			attributeGroup[i].toOWL(rest,ctx);
		
		if (group!=null) group.toOWL(rest,1,1,ctx);
		else if (all!=null) all.toOWL(rest,1,1,ctx);
		else if (choice!=null) choice.toOWL(rest,1,1,ctx);
		else if (sequence!=null) sequence.toOWL(rest,1,1,ctx);

		subClassComplexContent(cls, ctx);
	}

	/* restriction of simple content */

	public void toOWLSimpleContent(Resource cls, Restrictions rest, Context ctx) {
		schema xs = (schema) this.get_owner();
		// inherit attributes and simple content from the base
		XMLBean b = get_baseType(xs.ont,ctx);
		if (b==null) {
			// xsd type
			String type = expandQName(ctx.getDefaultNS(),base, xs.ont);
			if (rest!=null && type!=null && schema.isValidDatatype(type)){
				rest.addRange(xs.ont,RDF.value.getURI(),type);
				rest.addMin(RDF.value.getURI(),1,true,xs.ont);
				rest.addMax(RDF.value.getURI(),1,xs.ont);
				schema.defineType(xs.ont.createProperty(RDF.value.getURI()),type);
			}
		}
		
		for (int i=0; _attribute!=null && i<_attribute.length; i++)
			_attribute[i].toOWL(rest,ctx);
		for (int i=0; attributeGroup!=null && i<attributeGroup.length; i++)
			attributeGroup[i].toOWL(rest,ctx);
		
		if (group!=null) group.toOWL(rest,1,1,ctx);
		else if (all!=null) all.toOWL(rest,1,1,ctx);
		else if (choice!=null) choice.toOWL(rest,1,1,ctx);
		else if (sequence!=null) sequence.toOWL(rest,1,1,ctx);

		subClassComplexContent(cls, ctx);
	}

	/** add subclass relationships */

	public void subClassSimpleType(Resource cls, Context ctx) {
		OntModel ont = (OntModel) cls.getModel();
		String t = expandQName(ctx.getDefaultNS(),base, ont);
		if (t!=null && t.startsWith(schema.XSD_URI)) {
			Resource r = schema.toOWL(ont,t);
			if (r!=null) cls.addProperty(RDFS.subClassOf,r);
		}			
		try {
			simpleType s = (simpleType) get_baseType(ont,ctx);
			if (s!=null) cls.addProperty(RDFS.subClassOf,s.toOWL(ctx));
		} catch (Exception e) {}
	}

	public void subClassComplexContent(Resource cls, Context ctx) {
		try {
			complexType c = (complexType) get_baseType((OntModel) cls.getModel(),ctx);
			if (c!=null) cls.addProperty(RDFS.subClassOf,c.toOWL(null,true,ctx));
		} catch (Exception e) {}
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public String getId() {
		return id;
	}

	public void setId(String string) {
		id = string;
	}

	public attribute[] getAttribute() {
		return _attribute;
	}

	public void setAttribute(attribute[] attribute) {
		this._attribute = attribute;
	}

	public enumeration[] getEnumeration() {
		return _enumeration;
	}

	public void setEnumeration(enumeration[] enumeration) {
		this._enumeration = enumeration;
	}

	public maxInclusive getMaxInclusive() {
		return maxInclusive;
	}

	public void setMaxInclusive(maxInclusive maxInclusive) {
		this.maxInclusive = maxInclusive;
	}

	public maxLength getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(maxLength maxLength) {
		this.maxLength = maxLength;
	}

	public minInclusive getMinInclusive() {
		return minInclusive;
	}

	public void setMinInclusive(minInclusive minInclusive) {
		this.minInclusive = minInclusive;
	}

	public length getLength() {
		return length;
	}

	public void setLength(length length) {
		this.length = length;
	}

	public minLength getMinLength() {
		return minLength;
	}

	public void setMinLength(minLength minLength) {
		this.minLength = minLength;
	}

	public maxExclusive getMaxExclusive() {
		return maxExclusive;
	}

	public void setMaxExclusive(maxExclusive maxExclusive) {
		this.maxExclusive = maxExclusive;
	}

	public minExclusive getMinExclusive() {
		return minExclusive;
	}

	public void setMinExclusive(minExclusive minExclusive) {
		this.minExclusive = minExclusive;
	}
	
	public totalDigits getTotalDigits() {
		return totalDigits;
	}

	public void setTotalDigits(totalDigits totalDigits) {
		this.totalDigits = totalDigits;
	}

	public fractionDigits getFractionDigits() {
		return fractionDigits;
	}

	public void setFractionDigits(fractionDigits fractionDigits) {
		this.fractionDigits = fractionDigits;
	}

	public pattern[] getPattern() {
		return pattern;
	}

	public void setPattern(pattern[] pattern) {
		this.pattern = pattern;
	}

	public sequence getSequence() {
		return sequence;
	}

	public void setSequence(sequence sequence) {
		this.sequence = sequence;
	}

	public attributeGroup[] getAttributeGroup() {
		return attributeGroup;
	}

	public void setAttributeGroup(attributeGroup[] attributeGroup) {
		this.attributeGroup = attributeGroup;
	}

	public boolean subtype(String type, Model model, Context ctx) {
		return base!=null && expandQName(ctx.getDefaultNS(),base, model).equals(type);

	}

	public whiteSpace getWhiteSpace() {
		return whiteSpace;
	}

	public void setWhiteSpace(whiteSpace whiteSpace) {
		this.whiteSpace = whiteSpace;
	}

	public simpleType getSimpleType() {
		return simpleType;
	}

	public void setSimpleType(simpleType simpleType) {
		this.simpleType = simpleType;
	}

	public anyAttribute getAnyAttribute() {
		return anyAttribute;
	}

	public void setAnyAttribute(anyAttribute anyAttribute) {
		this.anyAttribute = anyAttribute;
	}

	public group getGroup() {
		return group;
	}

	public void setGroup(group group) {
		this.group = group;
	}

	public all getAll() {
		return all;
	}

	public void setAll(all all) {
		this.all = all;
	}

	public choice getChoice() {
		return choice;
	}

	public void setChoice(choice choice) {
		this.choice = choice;
	}


}
