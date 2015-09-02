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
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import com.hp.gloze.Context;
import com.hp.gloze.Gloze;
import com.hp.gloze.Restrictions;
import com.hp.gloze.XMLBean;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/*! \page extension extension
 The term 'extension' as used in XML schema is not used in its formal, mathematical sense.  
 Rather than defining a superset of valid instances, an extension describes a different, possibly intersecting set of instances that structurally extend the base type. 
 In many cases an extension will describe a valid sub-class, but there are cases where this does not hold. 
 In the valid cases we assert a subClassOf relationship.
 If we use extensions simply to add new elements and attributes to an existing type we have nothing to worry about. 
 The problems arise by adding additional occurrences of existing elements. 
 
 - \subpage extensionComplexContent "Extension of Complex Content"
 */

public class extension extends XMLBean {
	
	/** an extension adds additional attributes and elements
	 * extension of simple content adds attributes alongside the value 
	 * extension of complex content adds attributes, and appends elements to the base type
	 */

	private String base, id;
	private sequence sequence;
	private choice choice;
	private group group;
	private all all;
	private attribute[] _attribute;
	private attributeGroup[] attributeGroup;
	private anyAttribute anyAttribute;
	
	/**
	 * @return Returns the anyAttribute.
	 */
	public anyAttribute getAnyAttribute() {
		return anyAttribute;
	}
	/**
	 * @param anyAttribute The anyAttribute to set.
	 */
	public void setAnyAttribute(anyAttribute anyAttribute) {
		this.anyAttribute = anyAttribute;
	}

	public extension() throws IntrospectionException {
	}
	
	public boolean needSeq(Set<String> names, Context ctx) {
		try {
			if (base!=null) {
				//complexType c = xsd.getGlobalComplexType(base, ctx);
				XMLBean b = get_baseType(ctx.getModel(),ctx); // extension of base
				if (b instanceof simpleType) return false;
				complexType c = (complexType) b;
				if (c!=null && c.needSeq(names,ctx)) return true;	
			}
			return super.needSeq(names,ctx);
		} catch (Exception e) {
			return false;
		}
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

	public void resolve(Model model, Context ctx) {
		ctx.putBase(this,get_baseType(ctx.getModel(),ctx));
		super.resolve(model, ctx);
	}
	
	private XMLBean get_baseType(Model model,Context ctx) {
		XMLBean b = ctx.getBase(this);
		if (b!=null) return b;
		if (base!=null) {
			String uri = expandQName(ctx.getDefaultNS(),base,model);
			if (!uri.startsWith(schema.XSD_URI)) {
				b = ctx.getComplexType(uri);
				if (b==null) b = ctx.getSimpleType(uri);
				if (b!=null) ctx.putBase(this,b);
				else Gloze.logger.warn("no such base: "+base);
			}
		}
		return b;
	}
	
	// extension of complex content
	
	public int toRDF(boolean mixed, Resource subject, Element elem, int index, Seq seq, Set<restriction> restrictions, Context ctx) 
	throws Exception {
		Model m = ctx.getModel();
		schema xs = (schema) this.get_owner();
		complexType c = (complexType) get_baseType(ctx.getModel(),ctx); // extension of base
		if (c!=null) index = c.toRDF(subject,elem, index, seq, null, false, ctx);
		else { // it may be a schema type
			String type = expandQName(ctx.getDefaultNS(), base, m);
			if (type!=null && type.startsWith(schema.XSD_URI)) {
				// predefined schema datatype
				Statement stmt = schema.toRDFStatement(subject, elem, RDF.value, getValue(elem), type, ctx);
				if (stmt!=null) m.add(stmt);
			}
		}
		
		if (sequence!=null) index = sequence.toRDF(subject,elem,index,seq, mixed, ctx);
		else if (choice!=null) index = choice.toRDF(subject,elem,index,seq, mixed, ctx);
		else if (group!=null) index = group.toRDF(subject,elem,index,seq, mixed, ctx);
		else if (all!=null) index = all.toRDF(subject,elem,index,seq, mixed, ctx);

		if (_attribute!=null) xs.toRDF(subject, elem, _attribute, ctx);
		for (int i = 0; attributeGroup != null && i < attributeGroup.length; i++)
			attributeGroup[i].toRDF(subject, elem, restrictions, ctx);
		if (anyAttribute != null) anyAttribute.toRDF(subject, elem, null, ctx);
		return index;
	}
	
	/* extension of simple content (no sequencing required) */

	public boolean toRDF(Resource subject, Element element, Property prop, Set<restriction> restrictions, Context ctx)
		throws Exception {
		schema xs = (schema) this.get_owner();
		boolean ok = true;

		// there is no content if the element is nil
		if (!element.hasAttributeNS(schema.XSI,"nil") 
		 && !element.getAttributeNS(schema.XSI,"nil").equals("true")) {
			String uri = expandQName(ctx.getDefaultNS(),base, ctx.getModel());
			ok = xs.toRDF(subject,prop,getValue(element),uri,element,ctx);
		}
		// add the attributes
		if (ok && _attribute!=null) xs.toRDF(subject, element, _attribute, ctx);
		for (int i = 0; ok && attributeGroup != null && i < attributeGroup.length; i++)
			attributeGroup[i].toRDF(subject, element, restrictions, ctx);
		if (anyAttribute != null) anyAttribute.toRDF(subject, element, restrictions, ctx);
		
		// consult restrictions
		if (restrictions!=null)
			for (restriction r: restrictions)
				if (!r.isValid(subject,ctx)) return false;

		return true;
	}

	/* extension of complex content */
	
	public int toXML(Element e, Resource rdf, int index, Set<Statement> pending, Context ctx) {
		schema xs = (schema) this.get_owner();
		// set of rdf properties that have been processed
		if (base != null) {
			String b = expandQName(ctx.getDefaultNS(),base, ctx.getModel());
			complexType c = (complexType) get_baseType(ctx.getModel(),ctx);
			if (c!=null) index = c.toXML(e,rdf,index,pending,ctx);
			else xs.toXMLText(e,rdf,b,null,ctx);
		}	
		// extension to the base
		if (sequence!=null) index = sequence.toXML(e,rdf,index,pending,ctx);
		else if (choice!=null) index = choice.toXML(e,rdf,index,pending,ctx);
		else if (group!=null) index = group.toXML(e,rdf,index,pending,ctx);
		else if (all!=null) index = all.toXML(e,rdf,index,pending,ctx);

		for (int i = 0; _attribute != null && i < _attribute.length; i++)
			_attribute[i].toXML(e, rdf, pending,ctx);
		for (int i = 0; attributeGroup != null && i < attributeGroup.length; i++)
			attributeGroup[i].toXML(e, rdf, pending,ctx);
		if (anyAttribute!=null) anyAttribute.toXML(e,rdf,pending,ctx);
		
		return index;
	}
	
	public void assertType(Resource subject, Context ctx) {
		if (base!=null) {
			complexType c = ctx.getComplexType(expandQName(ctx.getDefaultNS(),base,ctx.getModel()));
			if (c!=null) c.assertType(subject,ctx);
		}		
	}
	
	/** extension to simple content */
	
	public void toXML(Element e, Resource subject, Set<Statement> pending, Context ctx) {
		schema xs = (schema) this.get_owner();
		Document doc = e.getOwnerDocument();
		if (base != null) {
			String b = expandQName(ctx.getDefaultNS(),base, ctx.getModel());
			Statement s = subject.getProperty(RDF.value);
			if (s!=null) xs.toXMLText(e,s.getObject(),b,null,ctx);
			else if (b.equals(schema.ID) && !subject.isAnon())
				e.appendChild(doc.createTextNode(subject.getLocalName()));
		}
		// add attributes that extend simple content
		for (int i = 0; _attribute != null && i < _attribute.length; i++)
			_attribute[i].toXML(e, subject, pending,ctx);
		for (int i = 0; attributeGroup != null && i < attributeGroup.length; i++)
			attributeGroup[i].toXML(e, subject, pending,ctx);
		if (anyAttribute!=null) anyAttribute.toXML(e,subject,pending,ctx);
	}
	
	public void toOWLComplexContent(Resource cls, Restrictions rest, Context ctx) {		
		schema xs = (schema) this.get_owner();
		// tally restrictions in base type
		Resource range = null;
		
		XMLBean b = get_baseType(xs.ont,ctx);		
		if (b!=null && b instanceof complexType)
			range = ((complexType) b).toOWL(rest,false,ctx);
		else { // schema datatype
			String t = expandQName(ctx.getDefaultNS(),base, xs.ont);
			if (t!=null && rest!=null && schema.isValidDatatype(t)) {
				Resource r = schema.toOWL(xs.ont, t);
				rest.addRange(RDF.value.getURI(),r);
				// complex content is single-valued
				rest.addMin(RDF.value.getURI(),1,true,xs.ont);
				rest.addMax(RDF.value.getURI(),1,xs.ont);
				schema.defineType(xs.ont.createProperty(RDF.value.getURI()),t);
			}
		}

		if (sequence!=null) sequence.toOWL(rest, 1, 1, ctx);

		for (int i=0; _attribute!=null && i<_attribute.length; i++)
			_attribute[i].toOWL(rest, ctx);

		for (int i=0; attributeGroup!=null && i<attributeGroup.length; i++)
			attributeGroup[i].toOWL(rest, ctx);

	}

	public void toOWLSimpleContent(Resource cls, Restrictions rest, Context ctx) {		
		schema xs = (schema) this.get_owner();
		// tally restrictions in base type
		XMLBean b = get_baseType(xs.ont,ctx); // extension of base type
		Resource range = null;
		
		if (b!=null && b instanceof complexType)
			range = ((complexType) b).toOWL(rest,false,ctx);
		
		// extension of simple type
		else if (b!=null && b instanceof simpleType) ;
		//return ((simpleType)b).toOWL(ctx);
		else {
			// xsd type
			String t = expandQName(ctx.getDefaultNS(),base, xs.ont);
			if (t!=null && rest!=null && schema.isValidDatatype(t)) {
				Resource r = schema.toOWL(xs.ont, t);
				rest.addRange(RDF.value.getURI(),r);
				// complex content is single-valued
				rest.addMin(RDF.value.getURI(),1,true,xs.ont);
				rest.addMax(RDF.value.getURI(),1,xs.ont);
				schema.defineType(xs.ont.createProperty(RDF.value.getURI()),t);
			}
		}

		if (sequence!=null) sequence.toOWL(rest, 1, 1, ctx);

		for (int i=0; _attribute!=null && i<_attribute.length; i++)
			_attribute[i].toOWL(rest, ctx);

		for (int i=0; attributeGroup!=null && i<attributeGroup.length; i++)
			attributeGroup[i].toOWL(rest, ctx);

	}

	/*! \page extensionComplexContent Extension of Complex Content
	 
	 Rather than defining a superset of valid instances an extension describes a different, possibly intersecting set of instances that
	 structurally extend the base type. In many cases the extension will describe a valid subclass of the parent, and in these cases
	 Gloze will assert a subClass relationship.
	 
	 The schema below defines a class 'Foo' with a single element 'foo', and two extensions of it.
	 If we use extensions simply to add new elements and attributes we have nothing to worry about.
	 The first type 'FooBar' adds a new element 'bar'. The latter type 'FooFoo' extends the base type by adding
	 another occurrence of 'foo'.
	 
	 \include extension.xsd
	 
	 The first extension 'FooBar' results in a valid subClass relationship, with each element having cardinality 1.
	 The latter class 'FooFoo' is not a valid subClass,
	 the cardinality of foo is 2, so the minimum cardinality (2) from the child is greater than the maximum cardinality (1)
	 inherited from the parent. Because this interval is empty, the class 'FooFoo' is unsatisfiable. 
	 
	 \include extension.owl

	 Invalid subClass relationships are detected logically, by asserting the hypothetical relationship and seeing if it results in
	 an inconsistency. If it does - the subclass relationship is retracted. In the next section we will see
	 why schema that use extension in particular ways must be defined in terms of their necessary and sufficient conditions.
	 
	 \section badExtensions When extensions go bad
	 
	 Unfortunately, a minor change to this example turns this violation into something far nastier.
	 In the example below, we change the type of element 'foo' from an object to a datatype property (an xs:string).
	 A valid instance of 'FooFoo' will include two occurrences of 'foo' with \em identical values. When this is mapped into RDF,
	 these count as a single logical statement, so a valid instance may have a cardinality for 'foo' of 1. Of course we still have the typical
	 case where each occurrence has a different value, with a cardinality of foo of 2. So the cardinality of 'foo' in 'FooFoo' lies in the interval [1,2].
	 The cardinality of 'foo' in class 'Foo' is 1, as before.
	 
	 The previous line of reasoning now fails, as the minimum cardinality of the child (1) no longer crosses the
	 maximum cardinality of the parent (1). Nor is it valid to argue that the maximum cardinality of the child (2) is greater than the maximum
	 cardinality of the child (1), in effect widening the definition.
	 The child is defined only in terms of its necessary conditions, and taking the intersection with its parent we still end up with a satisfiable class with a cardinality for 'foo' of 1. 
	 
     It turns out that 'FooFoo' is a valid subclass of 'Foo' only because of those errant instances where 'foo' has identical values. 
	 It's unlikely that this is the meaning of 'FooFoo' intended by the schema author, especially as this
	 particular implication remains implicit. One solution to this problem is to define classes
	 in terms of their necessary and \em sufficient conditions, meaning that any instance satisfying the conditions is a member by definition.
	 We express necessary and sufficient conditions in OWL by defining classes as an intersectionOf a set of restrictions.
	 
	 We reason \em semantically as follows. Because to be a member of 'FooFoo' it is sufficient
	 to have a cardinality of 'foo' of no more than 2, there must be a valid instance of 'FooFoo' with exactly 2 'foo' properties.
	 This individual can't be a model of 'Foo' because this has a maximum cardinality for 'foo' of 1. If 'FooFoo' were a subClass of 'Foo'
	 then every model of 'FooFoo' must also be a model of 'Foo'.
	 	 
	 \include extension1.xsd
	 
	 The gloze mapping is invoked with class=intersectionOf. The logical inconsistency is detected
	 and the subClass relationship between 'FooFoo' and 'Foo' is correctly retracted.
	 
	 \include extension1.owl


	 */
	
	public void subClass(Resource c, schema.type type, Context ctx) {
		OntModel ont = (OntModel) c.getModel();
		Resource superclass = null;
		XMLBean b = get_baseType(ont,ctx);
		
		if (b!=null && b instanceof simpleType)
			superclass = ((simpleType) b).toOWL(ctx);
		else if (b!=null && b instanceof complexType)
			superclass = ((complexType) b).toOWL(null,true,ctx);
		else if (base != null) {
			String t = expandQName(ctx.getDefaultNS(),base, ont);
			if (t.startsWith(schema.XSD_URI)) {
				if (type==schema.type.simpleType) superclass = schema.toOWL(ont,t);
			}	
			// extension as superclass can only be done in a context where classes are defined by intersection
			else superclass = ont.getResource(t);
		}
		
		// this ignores xs:anyType extensions which are null
		if (superclass!=null) c.addProperty(RDFS.subClassOf,superclass);

		// is this consistent
		if (!ctx.checkConsistency(c,ont.getBaseModel())) {
			schema.removeSubClass(ont,c,superclass);
			String subject = c.getLocalName();
			if (subject==null) subject = "anonymous";
			Gloze.logger.warn(subject+" invalid subClassOf "+superclass.getLocalName());
		}
	}
	
	public boolean subtype(String type, Model model, Context ctx) {
		return base!=null && expandQName(ctx.getDefaultNS(), base, model).equals(type);
	}

	public attribute[] getAttribute() {
		return _attribute;
	}

	public String getBase() {
		return base;
	}

	public String getId() {
		return id;
	}

	public void setAttribute(attribute[] attributes) {
		_attribute = attributes;
	}

	public void setBase(String string) {
		base = string;
	}

	public void setId(String string) {
		id = string;
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

	public void setAttributeGroup(attributeGroup[] groups) {
		attributeGroup = groups;
	}
	public choice getChoice() {
		return choice;
	}
	public void setChoice(choice choice) {
		this.choice = choice;
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
}
