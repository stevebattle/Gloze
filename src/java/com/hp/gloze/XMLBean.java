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

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.hp.gloze.www_w3_org_2001_XMLSchema.complexType;
import com.hp.gloze.www_w3_org_2001_XMLSchema.element;
import com.hp.gloze.www_w3_org_2001_XMLSchema.schema;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

/**
 * @author steven.a.battle@googlemail.com
 */
public abstract class XMLBean {
	
	private boolean populated = false;

	private static final String NS_PREFIX = "j.";

	public static final String XML = "http://www.w3.org/XML/1998/namespace";
	public static final String BASE = XML+"#base";
	public static final String ID = XML+"#id";
	public static final String LANG = XML+"#lang";
	public static final String SPACE = XML+"#space";

	// bean initialization method (invoked after setting attributes)
	private static final String INITIALISE = "initialise";

	// xml attributes
	String base, lang, id, space;

	protected Node _node; // XML node from which this bean is derived	
	String _pcdata;
	
	/** _any is used for non-specific content * */
	protected Vector<XMLBean> _any;

	/** holds xmlns definitions * */
	protected Properties _xmlns;
	private int _nsCounter = 0;

	protected XMLBean _parent;

	// by default a bean owns itself
	// children copy this so they all end up owned by the root
	protected XMLBean _owner = this;

	// where was the bean loaded from
	protected URL _location;

	/** may need to underscore this lot */
	public String name, localName, _prefix;


	public BeanInfo info;

	public XMLBean() throws IntrospectionException {
		// initialize bean info
		info = Introspector.getBeanInfo(this.getClass(), XMLBean.class);
		BeanDescriptor bd = info.getBeanDescriptor();
		name = bd.getDisplayName();
		localName = bd.getName();
	}
	
	// implement initialise() method to add bean specific initialization;
	public static void initialiseBean(XMLBean bean) throws Exception {
		try {
			Method[] method = bean.getClass().getMethods();
			for (int i = 0; i < method.length; i++) {
				if (method[i].getName().equals(INITIALISE)) {
					method[i].invoke(bean,(Object[]) null);
					break;
				}
			}
		} catch (Exception e) {
			Gloze.logger.error("error initialising bean "+bean.name);
		}
	}
	
	// no-schema lift
	
	public static void noSchemaToRDF(Element elem, URI uri, Context ctx) throws Exception {
		Model m = ctx.getModel();
		// create a root resource with optional uri
		Resource rez;
		if (uri != null) rez = m.createResource(uri.toString());
		//	anonymous resource
		else rez = m.createResource();
		// the document element is unsequenced
		noSchemaToRDF(rez, elem, null, ctx);
	}

	public static boolean noSchemaToRDF(Resource subject, Element elem, Seq seq, Context ctx) 
	throws Exception {
		Model m = subject.getModel();
		Statement stmt = null;
		
		String uri = expandQName(elem, ctx.getModel(), ctx);
		Property prop = m.createProperty(uri);

		// anything with no attributes and a single (or no) value is simple
		// anything with attributes is complex
		// anything with children is complex
		
		String simple = getSimpleContent(elem);
		if (elem.hasAttributes() || simple==null) {
			// looks like a complex type
			
			Resource obj = null;
			if (elem.hasAttributeNS(XML,"id")) {
				String id = elem.getAttributeNS(XML,"id");
				obj = m.createResource(addFragment(ctx.getBaseMap(), id).toString());
			}
			else obj = m.createResource();
			
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
										
					Seq subSeq = null;
					if (ctx.isSequenced() && elem.hasChildNodes() && c.needSeq(new HashSet<String>(), ctx))
						subSeq = m.getSeq(o.addProperty(RDF.type, RDF.Seq));
		
					int index = c.toRDF(o, elem, 0, subSeq,null,true,ctx);
					// mop up remaining values in sequence
					produceMixed(subSeq, index, elem);

					m.add(stmt);
					return true;
				}
				else Gloze.logger.warn("undefined type: "+fullname);
			}
			
			stmt = m.createStatement(subject,prop,obj);			
			m.add(stmt);
			if (seq != null) seq.add(stmt.createReifiedStatement());			

			// we can't confuse a (single) simple rdf:value with an attribute
			Seq s = null;
			// Added extra condition - we should be able to turn off sequencing, risking confusion between attributes and object properties
			if (simple==null && ctx.isSequenced()) s = m.getSeq(obj.addProperty(RDF.type, RDF.Seq));

			// add attributes (and possibly define xmlns)
			NamedNodeMap nm = elem.getAttributes();
			for (int i=0; i<nm.getLength(); i++)
				noSchemaToRDF(obj, (Attr) nm.item(i),ctx);

			// add elements
			NodeList nl = elem.getChildNodes();
			for (int i=0; i<nl.getLength(); i++) {
				switch (nl.item(i).getNodeType()) {
				case Node.ELEMENT_NODE:
					Element e = (Element) nl.item(i);
					noSchemaToRDF(obj,e,s,ctx);
					break;
				case Node.TEXT_NODE:
					String value = ((Text) nl.item(i)).getNodeValue().trim();
					Literal lit = null;
					if (elem.hasAttributeNS(XML,"lang"))
						lit = m.createLiteral(value, elem.getAttributeNS(XML,"lang"));
					else 
						lit = ctx.getModel().createLiteral(value);
					
					stmt = ctx.getModel().createStatement(obj,RDF.value,lit);
					if (!value.equals("")) {
						ctx.getModel().add(stmt);
						if (s!=null) s.add(stmt.createReifiedStatement());
					}
				}
			}
			return true;
		}
		else { // looks like a simple type
			String value = XMLBean.getValue(elem);
			if (value!=null && ctx.isPreserved()) value = value.trim();
			if (value==null) value = "";
			
			Literal l =  m.createLiteral(value);
			stmt = m.createStatement(subject,prop,l);			
			m.add(stmt);
			if (seq != null && stmt!=null) seq.add(stmt.createReifiedStatement());			
			return true;
		}
	}
	
	/** only returns the default ns if defined */
	
	public static void noSchemaToRDF(Resource subject, Attr attr, Context ctx) {
		if (attr.getNodeName().startsWith("xmlns")) {
			// set the namespace definition on the model
			String name = attr.getNodeName();
			String ns = attr.getNodeValue();
			String prefix = "";
			if (name.indexOf(":")>=0) {
				prefix = name.substring("xmlns:".length());
				if (!ns.equals(XMLBean.XML) && !ns.equals(schema.XSI)) 
					XMLBean.addPrefixes(prefix,ns,ctx.getModel());
			}
			return;					
		}
		Literal l = ctx.getModel().createLiteral(attr.getValue());
		String u = XMLBean.expandQName(attr, ctx);
		if (!u.startsWith(schema.XSI) && !u.startsWith(XMLBean.XML)) { 
			// not a schema instance or XML property
			Property p = ctx.getModel().createProperty(u);
			subject.addProperty(p,l);
		}
		XMLBean.addPrefixes(attr.getPrefix(), attr.getNamespaceURI(), ctx.getModel());
	}
	
	private static String getSimpleContent(Element elem) {
		StringBuffer b = new StringBuffer() ;
		NodeList l = elem.getChildNodes();
		for (int i=0; i<l.getLength(); i++) {
			Node n = l.item(i);
			switch (n.getNodeType()) {
			case Node.ELEMENT_NODE:
				return null;
			case Node.TEXT_NODE:
				String v = n.getNodeValue();
				if (v!=null) b.append(v);
			}
		}
		return b.toString();				
	}
		
	public static boolean noSchemaToXML(Document doc, RDFNode rdf, Context ctx) {
		boolean qualify = ctx.getDefaultNS()!=null;
		if (rdf instanceof Resource) {
			Resource r = (Resource) rdf;
			for (StmtIterator i = r.listProperties(); i.hasNext(); ) {
				Statement stmt = i.nextStatement();
				Property p = stmt.getPredicate();
				// ignore RDF properties eg. RDF:type
				// take the first (non-rdf) property we find as document element
				if (!p.getURI().startsWith(RDF.getURI())) {
					Element e = noSchemaToElement(doc,p,ctx);
					doc.appendChild(e);
					noSchemaToXML(e,stmt.getObject(),qualify,ctx);
					return true;
				}
			}
		}
		return false;
	}

	public static boolean noSchemaToXML(Element elem, RDFNode rdf, boolean qualify, Context ctx) {
		Document doc = elem.getOwnerDocument();
		String s = null;		
		if (rdf instanceof Resource) {
			Resource r = (Resource) rdf;
			Model m = r.getModel();
			
			// is the resource explicitly typed
			complexType ct = null;
			for (StmtIterator si = r.listProperties(RDF.type); si.hasNext(); ) {
				Resource t = si.nextStatement().getResource();
				
				// ignore RDF types, e.g. rdf:Seq
				if (t.getURI().startsWith(RDF.getURI())) continue;				
				complexType c = ctx.getComplexType(t.getURI());
				if (c!=null && (ct==null || c.subtype(ct.createURI(m,ctx),m,ctx))) ct = c;
			}		
			if (ct!=null) {
				ct.toXML(elem, r, 0, unsequenced(r), ctx);
				return true;
			}			

			// add (sequenced) sub-elements
			NodeIterator ni = ctx.getModel().getSeq(r).iterator();
			while (ni.hasNext()) {
				Statement stmt = element.asStatement((Resource) ni.nextNode());
				if (stmt.getPredicate().equals(RDF.value)) {
					// add literal value
					RDFNode value = stmt.getObject();
					if (value.isLiteral())
						elem.appendChild(doc.createTextNode(value.toString()));	
				}
				else {
					Element e = noSchemaToElement(elem,stmt.getPredicate(),ctx);
					if (e!=null) {
						elem.appendChild(e);
						noSchemaToXML(e,stmt.getObject(),qualify,ctx);
					}	
				}
			}

			// add (unsequenced) properties
			Set pending = element.unsequenced((Resource) rdf);
			for (Iterator ui = pending.iterator(); ui.hasNext(); ) {
				Statement stmt = (Statement) ui.next();
				if (stmt.getPredicate().equals(RDF.value)) {
					RDFNode n = stmt.getObject();
					if (n.isLiteral()) {
						Literal l = (Literal) n;
						elem.appendChild(doc.createTextNode(l.getString()));	
						if (l.getLanguage()!=null) elem.setAttributeNS(XML,"lang",l.getLanguage());
					}
				}
				else if (stmt.getObject().isResource() ) {
					Element e = noSchemaToElement(elem,stmt.getPredicate(),ctx);
					if (e!=null) {
						elem.appendChild(e);
						noSchemaToXML(e,stmt.getObject(),qualify,ctx);
					}
				}
				else if (stmt.getPredicate().getNameSpace().equals(RDF.getURI())) ;
				else {
					Attr a = noSchemaToAttribute(doc,stmt.getPredicate(), ctx);
					elem.setAttributeNode(a);
					noSchemaToXML(a,stmt.getObject(),ctx);
					
				}
			}
			// if the resource has a URI this is the node ID
			if (!r.isAnon()) {
				elem.setAttributeNS(XMLBean.XML,"id",r.getLocalName());				
			}
		} else {
			// add literal value
			s = ((Literal) rdf).getString();
			elem.appendChild(doc.createTextNode(s));
		}
		return true;
	}

	public static boolean noSchemaToXML(Attr attr, RDFNode rdf, Context ctx) {
		if (rdf instanceof Resource) {
			Resource r = (Resource) rdf;
			if (r.hasProperty(RDF.value)) {
				attr.setNodeValue(r.getProperty(RDF.value).getString());
				return true;
			}
		}
		else {
			// literal
			Literal l = (Literal) rdf;
			attr.setNodeValue(l.getString());
		}
		return false;
	}

	public static Element noSchemaToElement(Node node, Property p, Context ctx) {
		Document doc ;
		if (node instanceof Document)doc = (Document) node;
		else doc = node.getOwnerDocument();

		String ns = p.getNameSpace();
		if (ns.equals(RDF.getURI())) return null;
		String name = p.getLocalName();
		// remove symbol space
		if (ctx.getElementSymbol()!=null && ns.endsWith(ctx.getElementSymbol())) 
			ns = ns.substring(0,ns.length()-ctx.getElementSymbol().length());
		else if (ctx.getElementSymbol()!=null && name.startsWith(ctx.getElementSymbol())) 
			name = name.substring(ctx.getElementSymbol().length()+1);
		
		String pre = XMLBean.findPrefix(p.getURI(),ctx.getModel());
		String ns1 = XMLBean.lookupNamespace(pre,ctx.getModel());
		if (pre!=null && ns.startsWith(pre)) {
			String tail = ns.substring(ns1.length());
			if (tail.equals("#")) ns = ns1;
		}
		if (ns.equals(ctx.getDefaultNS()) || ns.equals(ctx.getDefaultNS()+"#")) {
			// this namespace is reserved for 'unqualified' elements
			// if default namespace is defined it must be overriden to unqualify this element
			String dns = XMLBean.expandPrefix("", node,null,ctx.getModel());
			if (dns==null) return doc.createElement(name);
			else return doc.createElementNS("",name);
		}
		else return doc.createElementNS(ns,name);
	}

	private static Attr noSchemaToAttribute(Document doc, Property p, Context ctx) {
		String ns = p.getNameSpace();
		String name = p.getLocalName();
		// remove symbol space
		if (ctx.getAttributeSymbol()!=null && ns.endsWith(ctx.getAttributeSymbol())) 
			ns = ns.substring(0,ns.length()-ctx.getAttributeSymbol().length());
		else if (ctx.getAttributeSymbol()!=null && name.startsWith(ctx.getAttributeSymbol())) 
			name = name.substring(ctx.getAttributeSymbol().length()+1);
		
		String pre = null;
		if (ns.endsWith("#")) {
			pre = ctx.getModel().getNsURIPrefix(ns.substring(0,ns.length()-1));
			if (pre!=null) ns = ns.substring(0,ns.length()-1);
		}
		else pre = ctx.getModel().getNsURIPrefix(ns);
		
		Attr a ;
		if (ns!=null) a = doc.createAttributeNS(ns,name);
		else a = doc.createAttribute(name);
		
		if (pre!=null ) a.setPrefix(pre);
		return a;
	}
		
	
	/* does the content sequencing require disambiguation */

	public boolean needSeq(Set<String> names, Context ctx) {
		populate();
		for (int i=0; _any!=null && i<_any.size(); i++) {
			if (((XMLBean) _any.elementAt(i)).needSeq(names, ctx)) return true;
		}
		return false;
	}

	/** resolve references to simple and complex types, groups and attribute groups (for redefine)*/

	public void resolve(Model model, Context ctx) {
		populate();
		for (int i=0; _any!=null && i<_any.size(); i++)
			((XMLBean) _any.elementAt(i)).resolve(model, ctx);
	}
	
	public void reportLocalElements(Model model, Set<String> report, Context ctx) {
		populate();
		for (int i=0; _any!=null && i<_any.size(); i++)
			_any.elementAt(i).reportLocalElements(model,report,ctx);
	}

	public void reportLocalAttributes(Model model, Set<String> report, Context ctx) {
		populate();
		for (int i=0; _any!=null && i<_any.size(); i++)
			_any.elementAt(i).reportLocalAttributes(model,report,ctx);
	}

	public int toRDF(Resource subject, Node node, int index, Seq seq, boolean mixed, Context ctx) throws Exception {
		Gloze.logger.error("invoked default RDF mapping");
		return index;
	}

	public String get_namespace() {
		BeanDescriptor bd = info.getBeanDescriptor();
		String name = bd.getDisplayName();
		String localName = bd.getName();
		if (name.equals(localName)) return null;
		// the namespace is the difference between name and localName
		return name.substring(0, name.length() - localName.length());
	}

	public ContentIFace getContentModel() throws IntrospectionException {
		BeanDescriptor bd = info.getBeanDescriptor();
		ContentIFace content = (ContentIFace) bd.getValue("content");
		if (content != null) return content;
		return new Content();
	}

	public static XMLBean newInstance(Document doc, URI base)
			throws MalformedURLException {
		return newInstance(doc.getDocumentElement(), base.toURL());
	}

	public static XMLBean newInstance(Element element, URL uri) {
		XMLBean bean = null;
		try {
			Class c = BeanLoader.load(element);
			if (c != null) {
				bean = (XMLBean) c.newInstance();
				bean.set_location(uri);
				bean.set_node(element);

				ContentIFace content = bean.getContentModel();
				// add attributes
				boolean added = content.addAttributes(bean, element.getAttributes());

				// add children by delegation to content model
				added &= content.addChildNodes(bean, element.getChildNodes());
				if (!added) Gloze.logger.warn("bad content in new instance of "+c.getName());
			}
		} catch (Exception e) {
			Gloze.logger.error(e.getMessage());
		}
		// obtain prefix
		int x = element.getNodeName().indexOf(':');
		if (bean != null && x >= 0)
			bean._prefix = element.getNodeName().substring(0, x);
		
		bean.populated = true;
		return bean;
	}

	public static XMLBean newInstance(XMLBean parent, Element element) {
		XMLBean bean = null;
		
		// the parent may be set to ignore this element
		String namespace = element.getNamespaceURI();
		String localName = element.getLocalName();
		String uri = XMLBean.concatName(namespace, localName);
		
		String[] ignore = (String[]) parent.info.getBeanDescriptor().getValue("ignore");
		if (ignore!=null && Arrays.asList(ignore).contains(uri)) return null;
		
		try {
			Class c = BeanLoader.load(element);
			boolean added = false;
			if (c != null) {
				bean = (XMLBean) c.newInstance();
				if (bean != null) {
					bean.set_node(element);
					bean.set_owner(parent.get_owner());
					
					// set default namespace (of parent)
					Properties p = parent._xmlns;
					if (p==null) Gloze.logger.warn("parent has no default ns:" +parent);
					else bean._xmlns = (Properties) p.clone();
					
					//String xmlns = p!=null? (String)p.get(""):null;
					//if (xmlns!=null) bean._xmlns.put("", xmlns);
				
					ContentIFace content = bean.getContentModel();
					// add attributes (xmlns properties overrides default from parent)
					added = content.addAttributes(bean, element.getAttributes());
					
					// initialise bean once attributes are set
					initialiseBean(bean);
					
					// add children by delegation to content model
					added &= content.addChildNodes(bean, element.getChildNodes());
				}
				if (!added) Gloze.logger.warn("bad content in new instance: "+c.getName());
			}
		} catch (Exception e) {
			Gloze.logger.error(e.getMessage());
		}
		// obtain prefix
		int x = element.getNodeName().indexOf(':');
		if (bean != null && x >= 0)
			bean._prefix = element.getNodeName().substring(0, x);

		bean.populated = true;
		return bean;
	}

	public static XMLBean newShallowInstance(XMLBean parent, Element element) {
		XMLBean bean = null;
		ContentIFace state;
		
		// the parent may be set to ignore this element
		String namespace = element.getNamespaceURI();
		String localName = element.getLocalName();
		String uri = XMLBean.concatName(namespace, localName);
		
		String[] ignore = (String[]) parent.info.getBeanDescriptor().getValue("ignore");
		if (ignore!=null && Arrays.asList(ignore).contains(uri)) return null;
		
		try {
			Class c = BeanLoader.load(element);
			if (c != null) {
				bean = (XMLBean) c.newInstance();
				if (bean != null) {
					bean.set_node(element);
					bean.set_owner(parent.get_owner());
					
					// set default namespace (of parent)
					Properties p = parent._xmlns;
					if (p==null) Gloze.logger.warn("parent has no default ns:" +parent);
					else bean._xmlns = (Properties) p.clone();
					
					ContentIFace content = bean.getContentModel();
					// add attributes (xmlns properties overrides default from parent)
					content.addAttributes(bean, element.getAttributes());
					
					// initialise bean once attributes are set
					initialiseBean(bean);					
				}
			}
		} catch (Exception e) {		
			Gloze.logger.error("Cannot load bean: "+uri);
		}
		// obtain prefix
		int x = element.getNodeName().indexOf(':');
		if (bean != null && x >= 0)
			bean._prefix = element.getNodeName().substring(0, x);

		return bean;
	}

	/** create a shallow instance with attribute content only * */

	public static XMLBean newShallowInstance(Element element) throws Exception {
		XMLBean bean = null;
		try {
			Class c = BeanLoader.load(element);
			if (c != null) bean = (XMLBean) c.newInstance();
		} catch (Exception e) {
			Gloze.logger.error(e.getMessage());
		}
		
		if (bean!=null) bean.set_node(element);
		
		// set default namespace (of parent)
		if (bean._xmlns == null) bean._xmlns = new Properties();

		// obtain prefix
		int x = element.getNodeName().indexOf(':');
		if (bean != null && x >= 0)
			bean._prefix = element.getNodeName().substring(0, x);

		// add attributes
		ContentIFace content = bean.getContentModel();
		if(!content.addAttributes(bean, element.getAttributes()))
			Gloze.logger.warn("bad attribute in: "+element.getTagName());

		initialiseBean(bean);
		
		return bean;
	}

	/** populate an instance created with new shallow instance * */

	public void populate() {
		if (populated) return;
		Element element = (Element) get_node();
		// initialise the bean (all attributes are set)
		try {
			ContentIFace content = getContentModel();
			// add children by delegation to content model
			if (!content.addChildNodes(this, element.getChildNodes()))
				Gloze.logger.warn("bad content in: "+element.getTagName());
		} catch (Exception e) {
			Gloze.logger.error(e.getMessage());
		}
		populated = true;
	}

	public boolean addAttribute(Attr attribute) throws Exception {
		String name = attribute.getName();
		String value = attribute.getNodeValue();

		// namespace declaration
		if (name.equals("xmlns")) {
			// default namespace has empty string as the key
			if (_xmlns == null) _xmlns = new Properties();
			_xmlns.put("", value);
			return true;
		} else if (name.startsWith("xmlns:")) {
			// namespace prefix
			if (_xmlns == null) _xmlns = new Properties();
			_xmlns.put(name.substring(6), value);
			return true;
		} else {
			String namespace = attribute.getNamespaceURI();
			String localName = attribute.getLocalName();
			String uri = concatName(namespace, localName);
			
			// xml namespace attributes
			if (uri.equals(BASE)) setBase(value);
			else if (uri.equals(ID)) setId(value);
			else if (uri.equals(LANG)) setLang(value);
			else if (uri.equals(SPACE)) setSpace(value);
			else return addProperty(uri, value);
			return true; // in case xml property was added
		}
	}

	public boolean addProperty(String name, String value) throws Exception {
		if (name.startsWith("#")) name = name.substring(1);
		PropertyDescriptor pd[] = info.getPropertyDescriptors();
		for (int i = 0; i < pd.length; i++) {
			if (getPublicName(pd[i]).equals(name)) {
				// array properties (append value)
				if (pd[i].getPropertyType().isArray()
						&& pd[i].getPropertyType().getComponentType() == String.class) {
					String[] arg = new String[] { value };
					getSetter(pd[i]).invoke(this, new Object[] { arg });
				} else if (
						// non-array properties (overwrite existing value)
						// eg. minOccurs="0" overwrites default value "1"
						!pd[i].getPropertyType().isArray()
						&& pd[i].getPropertyType() == String.class) {
					getSetter(pd[i]).invoke(this, new Object[] { value });
				}
				return true;
			}
		}
		return false;
	}

	// the public XML attribute name may differ from the field name

	String getPublicName(PropertyDescriptor pd) {
		// the display name overrides the name where they differ
		String dname = pd.getDisplayName();
		if (dname != null) {
			// generic XML attributes
			if (dname.startsWith("xml:"))
				return concatName(XML, dname.substring(4));
			return dname;
		} else return pd.getName();
	}

	public String[] getPropertyValues(String name) throws Exception {
		PropertyDescriptor pd[] = info.getPropertyDescriptors();
		for (int i = 0; i < pd.length; i++) {
			if (pd[i].getName().equals(name)) {
				if (pd[i].getPropertyType().isArray()
						&& pd[i].getPropertyType().getComponentType() == String.class) {
					return (String[]) getGetter(pd[i]).invoke(this,
							new Object[] {});
				} else if (!pd[i].getPropertyType().isArray()
						&& pd[i].getPropertyType() == String.class) {
					String value = (String) getGetter(pd[i]).invoke(this,
							new Object[] {});
					return new String[] { value };
				}
			}
		}
		return null;
	}

	public boolean addContent(XMLBean b) throws Exception {
		if (b == null) return false;
		PropertyDescriptor pd[] = info.getPropertyDescriptors();
		String bName = b.info.getBeanDescriptor().getDisplayName();
		if (pd.length > 0 && _any == null)
			_any = new Vector<XMLBean>();
		for (int i = 0; i < pd.length; i++) {
			// compare fully qualified names
			if (getPublicName(pd[i]).equals(bName)) {
				_any.add(b);
				if (pd[i].getPropertyType().isArray()) {
					Class c = pd[i].getPropertyType().getComponentType();
					if (XMLBean.class.isAssignableFrom(c)) {
						Method getter = pd[i].getReadMethod();
						XMLBean[] children = (XMLBean[]) getter.invoke(this, (Object[]) null);
						Method setter = getSetter(pd[i]);
						setter.invoke(this, new Object[] { addChild(c,
								children, b) });
						b._parent = this;
						return true;
					}
				} else if (!pd[i].getPropertyType().isArray()) {
					Class c = pd[i].getPropertyType();
					if (XMLBean.class.isAssignableFrom(c)) {
						Method setter = getSetter(pd[i]);
						setter.invoke(this, new Object[] { b });
						b._parent = this;
						return true;
					}
				}
				break;
			}
		}
		return false;
	}

	XMLBean[] addChild(Class c, XMLBean[] children, XMLBean bean) {
		int l = children != null ? children.length : 0;
		Object a = Array.newInstance(c, l + 1);
		for (int i = 0; children != null && i < l; i++) {
			Array.set(a, i, children[i]);
		}
		Array.set(a, l, bean);
		return (XMLBean[]) a;
	}

	public Method getSetter(PropertyDescriptor pd) {
		// setter/getter may be defined by the bean info class
		Method m = pd.getWriteMethod();
		if (m == null) {
			Method[] method = this.getClass().getMethods();
			String setter = "set" + pd.getName();
			for (int i = 0; i < method.length; i++) {
				if (method[i].getName().toLowerCase().equals(setter))
					return method[i];
			}
		}
		return m;
	}

	public Method getGetter(PropertyDescriptor pd) {
		// setter/getter may be defined by the bean info class
		Method m = pd.getReadMethod();
		if (m == null) return getGetter(pd.getName());
		return m;
	}

	public Method getGetter(String name) {
		Method[] method = this.getClass().getMethods();
		String getter = "get" + name;
		for (int i = 0; i < method.length; i++) {
			if (method[i].getName().toLowerCase().equals(getter))
				return method[i];
		}
		return null;
	}

	public String toString() {
		StringWriter s = new StringWriter();
		try {
			XMLUtility.factory.setNamespaceAware(true);
			Document doc = XMLUtility.factory.newDocumentBuilder().newDocument();
			XMLUtility.write(getElement(doc), new PrintWriter(s));
		} catch (Exception x) {
			Gloze.logger.error(x.getMessage());
			return "";
		}
		return s.toString();
	}

	// true if the attribute equals the default
	public boolean isDefault(String attribute, String value) {
		return false;
	}

	public Element getElement(Document doc) throws Exception {
		Element e;
		String _namespace = get_namespace();
		if (_namespace != null) {
			if (_xmlns != null && _namespace.equals(_xmlns.get("")))
				// unqualified element, namespace
				e = doc.createElementNS(_namespace, localName);
			else if (_prefix == null)
				// no namespace
				e = doc.createElement(localName);
			else
				// qualified element
				e = doc.createElementNS(_namespace, _prefix + ":" + localName);
		} 
		// no namespace
		else e = doc.createElement(name);
		
		ContentIFace content = getContentModel();

		// add attributes

		NamedNodeMap attributes = content.getAttributes(this, doc);
		for (int i = 0; i < attributes.getLength(); i++) {
			Attr a = (Attr) attributes.item(i);
			if (!isDefault(a.getName(), a.getValue()))
				e.setAttributeNodeNS(a);
		}
		NodeList l = content.getChildNodes(this, doc);
		for (int i = 0; i < l.getLength(); i++) {
			e.appendChild(l.item(i));
		}
		return e;
	}

	public boolean hasChildren() {
		try {
			PropertyDescriptor[] pd = info.getPropertyDescriptors();
			for (int i = 0; i < pd.length; i++) {

				if (pd[i].getPropertyType().isArray()
						&& XMLBean.class.isAssignableFrom(pd[i]
								.getPropertyType().getComponentType())) {
					// multiple elements
					Method getter = getGetter(pd[i]);
					XMLBean[] children = (XMLBean[]) getter.invoke(this, (Object[]) null);
					return children != null && children.length > 0;
				} else if (!pd[i].getPropertyType().isArray()
						&& XMLBean.class.isAssignableFrom(pd[i]
								.getPropertyType())) {
					// solo element
					Method getter = getGetter(pd[i]);
					XMLBean child = (XMLBean) getter.invoke(this, new Object[] {});
					return child != null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		return false;
	}

	// this differs from getPublicName in that the 'xml' prefix is not expanded

	public String getDisplayName(PropertyDescriptor pd) {
		String name = pd.getDisplayName();
		// use name as default if display name undefined
		if (name == null) name = pd.getName();
		return name;
	}

	/*
	 * Expand local name with namespace
	 * 
	 * @throws Exception
	 */
	
	public static String concatName(String namespace, String symbol, String localName) {
		if (localName==null) return null;
		return concatName(namespace, symbol==null?localName:symbol+localName);
	}
	
	public static String terminateNS(String namespace) {
		if (namespace==null) return null;
		// if the last character of the namespace is alphanumeric insert a '#'
		if (Character.isLetterOrDigit(namespace.charAt(namespace.length()-1)))
			return namespace + "#";
		else return namespace;
	}

	public static String concatName(String namespace, String localName) {
		if (namespace==null) return localName;
		return terminateNS(namespace) + localName;
	}

	public static URI addFragment(URI uri, String fragment) throws Exception {
		return new URI(uri.getScheme(),uri.getAuthority(),uri.getPath(),fragment);
	}
			
	public static String addPrefix(String prefix, String ns, Model model) {
		if (prefix==null || ns==null || prefix.equals("")) return prefix; // the default prefix isn't useful
		if (ns.startsWith(schema.XSI)) return prefix;
		Map m = model.getNsPrefixMap();
		if (!m.containsKey(prefix)) model.setNsPrefix(prefix,terminateNS(ns));
		return prefix;
	}
	
	public static void addPrefixes(String prefix, String ns, Model model) {
		if (prefix==null || prefix.equals("") || prefix.startsWith("xml")) return; // the default prefix isn't useful
		if (ns.startsWith(schema.XSI)) return;
		Map m = model.getNsPrefixMap();
		if (!m.containsKey(prefix)) model.setNsPrefix(prefix,ns);
		if (Character.isLetterOrDigit(ns.charAt(ns.length()-1)) && !m.containsKey(prefix+"_"))
			model.setNsPrefix(prefix+"_",ns+"#");
	}

	public static String lookupNamespace(String prefix, Model model) {
		Map m = model.getNsPrefixMap();
		return (String) m.get(prefix);
	}
	
	@SuppressWarnings("unchecked")
	public static String lookupPrefix(String ns, Model model, Context ctx) {
		Map<String,String> m = model.getNsPrefixMap();
		if (ns.endsWith("#")) ns = ns.substring(ns.length()-1);
		String prefix = null;
		for (String p: m.keySet()) {
			if (m.get(p).equals(ns) 
			|| (prefix==null && m.get(p).equals(ns+"#"))) prefix = p;
		}
		if (prefix!=null) return prefix;
		// otherwise create one
		else return addPrefix(ctx.createNSPrefix(),ns,model);			
	}

	/** return prefix for the longest matching namespace */
	
	public static String findPrefix(String name, Model model) {
		Map m = model.getNsPrefixMap();
		Iterator keys = m.keySet().iterator();
		String p=null, namespace=null;
		int length=0;
		while (keys.hasNext()) {
			String k = (String) keys.next();
			String ns = (String) m.get(k);
			int l = ns.length();
			if (ns.endsWith("#")) l -= 1;
			if ((l>length && name.startsWith(ns)) ||
				(l==length && namespace.endsWith("#") && namespace.substring(0,l).equals(ns))) {
				p = k;
				length = l;
				namespace = ns;
			}
		}
		return p;		
	}
	
	/** expand a prefix defined wrt the given node */
	/** this may side-effect the model, adding a prefix */
	/** the default ns is required for no-namespace schema */

	public static String expandPrefix(String prefix, Node node, String defaultNS, Model model) {
		String ns = expandPrefix(prefix, node, model);
		return ns==null?defaultNS:ns;
	}

	/** expand a prefix defined wrt the given node */
	/** this may side-effect the model, adding a prefix */

	public static String expandPrefix(String prefix, Node node, Model model) {
		// the XML namespace is predefined and need not be explicit in the document
		if (prefix.equals("xml")) return XML;

		// can we get the prefix and ns directly from the node
		if (prefix.equals(node.getPrefix())) {
			String ns = node.getNamespaceURI();
			// side-effect the model prefix-mapping (unless XSI namespace)
			if (!ns.equals(schema.XSI)) addPrefixes(prefix,ns,model);
			return ns;
		}
		// if this node has a namespace and no prefix, it must be the default
		if (prefix.equals("") && node.getNamespaceURI()!=null && node.getPrefix()==null)
			return node.getNamespaceURI();

		// consult xmlns declarations for this node
		if (node.getNodeType()==Document.ELEMENT_NODE) {
			Element e = (Element) node;
			String a = prefix.equals("")?"xmlns":"xmlns:"+prefix;
			if (e.hasAttribute(a)) {
				String ns = e.getAttribute(a);
				if (!ns.equals(schema.XSI)) addPrefixes(prefix,ns,model);
				return ns;
			}
		}

		// declared in the parent
		if (node.getNodeType()!=Document.DOCUMENT_NODE) {
			Node parent = node instanceof Attr?((Attr) node).getOwnerElement():node.getParentNode();
			String p = expandPrefix(prefix, parent, model);
			if (p!=null) return p;
		}

		return null;
	}

	public String get_namespace(String prefix) {
		String ns = null;
		if (_xmlns != null) ns = (String) _xmlns.get(prefix);
		if (ns == null) {
			XMLBean owner = this.get_owner();
			if (owner != null) ns = owner.get_namespace(prefix);
		}
		return ns;
	}

	/** expand and contract QNames */
		
	/** expand a QName in the context of an XML schema */

	public String expandQName(String defaultNS, String qname, Model model) {
		return expandQName(defaultNS, null, qname, get_node(), model);
	}
	
	/** expand a symbol/QName in the context of a schema */
	
	public String expandQName(String defaultNS, String symbol, String qname, Model model) {
		return expandQName(defaultNS, symbol, qname, get_node(), model);
	}

	/** expand QName in the context of an XML node */

	public static String expandQName(String defaultNS, String name, Node node, Model model) {
		return expandQName(defaultNS,null, name,node,model);
	}
	
	/** expand a QName, this may side-effect the model adding a new prefix */

	public static String expandQName(String defaultNS, String symbol, String qname, Node node, Model model) {
		// URIs are invalid QNames
		if (qname == null || isValidURI(qname)) return null;
		String prefix = "";
		int i = qname.indexOf(':');
		if (i >= 0) {
			prefix = qname.substring(0, i);
			qname = qname.substring(i + 1);
		}
		return concatName(expandPrefix(prefix, node, defaultNS, model), symbol, qname);
	}

	/** expand an element QName in an XML instance */

	static public String expandQName(Element elem, Model model, Context ctx) {
		if (elem.getNamespaceURI()!=null)
			return concatName(elem.getNamespaceURI(), ctx.getElementSymbol(), elem.getLocalName());

		// the element is unqualified
		return concatName(ctx.getDefaultNS(),ctx.getElementSymbol(), elem.getNodeName());
	}

	/** expand an attribute QName in an XML instance */

	public static String expandQName(Attr attr, Context ctx) {
		if (attr.getNamespaceURI()!=null)
			return concatName(attr.getNamespaceURI(), ctx.getAttributeSymbol(), attr.getLocalName());
		
		// the attribute is unqualified
		return concatName(ctx.getDefaultNS(), ctx.getAttributeSymbol(), attr.getNodeName());		
	}

	/** contract a QName in the context of the instance document (base relative) */
	
	public String contractQName(String name, Element e, Model model, URI base) {
		try {
		
		// does the document base match
		if (base!=null && name.startsWith(base.toString()))
			return base.relativize(new URI(name)).toString();
		
		// any matching namespaces defined in the XML instance
		String contraction = contractPrefix(name, e);
		if (contraction!=null) return contraction; 
		
		// any matching namespaces defined in the model?
		String p = findPrefix(name, model);
		if (p!=null) {
			String ns = lookupNamespace(p,model);
			name = name.substring(ns.length());
			if (name.startsWith("#")) name = name.substring(1);
			// add prefix if required (the default prefix is the empty string)
			if (!p.equals("")) name = p + ":" + name;
			if (!declares(e, p)) e.setAttribute("xmlns:"+p,ns);
			return name;
		}
		
		// any matching namespaces defined in the schema?
		for (Iterator i = _xmlns.keySet().iterator(); i.hasNext() ; ) {
			String prefix = (String) i.next();
			String ns = (String) _xmlns.get(prefix);
			if (!prefix.equals("") && name.startsWith(ns)) {
				name = name.substring(ns.length());
				if (name.startsWith("#")) name = name.substring(1);
				// add prefix if required (the default prefix is the empty string)
				if (!prefix.equals("")) name = prefix + ":" + name;
				if (!declares(e, prefix)) e.setAttribute("xmlns:"+prefix,ns);
				return name;
			}
		}
		// define a new namespace
		String prefix = NS_PREFIX + _nsCounter++;
		String ns = null;
		if (name.lastIndexOf("#")>=0) ns = name.substring(0,name.lastIndexOf("#"));
		
		if (ns!=null) {
			_xmlns.put(prefix,ns);
			e.setAttribute("xmlns:"+prefix,ns);
			name = name.substring(ns.length());
			if (name.startsWith("#")) name = name.substring(1);
			return prefix + ":" + name;			
		}
		// can't split the name
		return name;
		} catch (Exception x) {
			return null;
		}
	}
			
	public String contractPrefix(String name, Element e) {
		// same namespace as the element?
		if (e.getNamespaceURI()!=null && name.startsWith(e.getNamespaceURI())) {
			name = name.substring(e.getNamespaceURI().length());
			if (name.startsWith("#")) name=name.substring(1);
			if (e.getPrefix()!=null) return e.getPrefix()+":"+name;
			else return name; // element in the default ns
		}
		// in the locally defined default ns
		if (e.hasAttribute("xmlns")) {
			String ns = e.getAttribute("xmlns");
			if (name.startsWith(ns)) {
				name = name.substring(ns.length());
				if (name.startsWith("#")) name=name.substring(1);
				return name; // in the default namespace
			}
		}
		// any matching declarations?
		NamedNodeMap m = e.getAttributes();
		for (int i=0; i<m.getLength(); i++) {
			Attr a = (Attr) m.item(i);
			if (a.getName().startsWith("xmlns:")) {
				String ns = a.getValue();
				if (name.startsWith(ns)) {
					String p = a.getName().substring("xmlns:".length());
					name = name.substring(ns.length());
					if (name.startsWith("#")) name=name.substring(1);
					return p + ":" +name; // in the default namespace
				}
			}
		}
		// check the parent
		if (e.getParentNode().getNodeType()!=Document.DOCUMENT_NODE) {
			return contractPrefix(name, (Element) e.getParentNode());
		}
		return null;
	}
		
	boolean declares(Element e, String prefix) {
		if (prefix==null || prefix.equals("")) return true;
		if (e.hasAttribute("xmlns:"+prefix)) return true;
		Node parent = e.getParentNode();
		if (parent!=null && parent.getNodeType()==Document.ELEMENT_NODE) return declares((Element) parent, prefix);
		else return false;
	}


	protected static String getValue(Element element) {
		NodeList l = element.getChildNodes();
		StringBuffer s = null;
		for (int i = 0; i < l.getLength(); i++) {
			switch (l.item(i).getNodeType()) {
			case Node.TEXT_NODE:
			case Node.CDATA_SECTION_NODE:
				if (s==null) s = new StringBuffer();
				s.append(l.item(i).getNodeValue());
			}
		}
		return s==null?null:s.toString();
	}

	/* properties of the rdf node that are not in sequence */
	/* this includes type information */
	
	protected static Set<Statement> unsequenced(Resource rdf) {
		Set<Statement> s = new HashSet<Statement>();
		StmtIterator si = rdf.listProperties();
		while (si.hasNext()) {
			Statement stmt = si.nextStatement();
			// don't add ordinals
			if (stmt.getPredicate().getOrdinal()==0) s.add(stmt);
		}
		NodeIterator ni = rdf.getModel().getSeq(rdf).iterator();
		while (ni.hasNext()) {
			Statement stmt = asStatement((Resource) ni.nextNode());
			if (s.contains(stmt)) s.remove(stmt);
		}
		return s;
	}

	/** return a reified statement as a statement */
	
	protected static Statement asStatement(Resource r) {
		Statement stmt;
		if (r==null) return null;
		try {		
			stmt = ((ReifiedStatement) r.as(ReifiedStatement.class)).getStatement();
		} catch (Exception e1) {
			Gloze.logger.warn("ill-formed reification");
			StmtIterator si = r.listProperties();
			while (si.hasNext()) Gloze.logger.warn(si.next());
			stmt = null;
		}
		return stmt;
	}

	/* consume mixed values in an rdf sequence */

	protected int consumeMixed(NodeList l, int index, Resource subject, Seq seq, Context ctx) {
		while (index < l.getLength()) {
			switch (l.item(index).getNodeType()) {
			case Node.TEXT_NODE:
				schema.textToRDF(subject, seq, l.item(index), ctx);
			case Node.COMMENT_NODE:
				index++;
				break;
			default:
				return index;
			}
		}
		return index;
	}

	protected static int produceMixed(Seq seq, int index, Element elem) {
		Document doc = elem.getOwnerDocument();
		while (seq!=null && index<seq.size()) {
			Statement stmt = (Statement) asStatement((Resource) seq.getObject(index+1));
			if (stmt.getPredicate().equals(RDF.value)) {
				elem.appendChild(doc.createTextNode(stmt.getString()));
				index++;
			} else break;
		}
		return index;
	}
	
	/**
	 * @param type
	 * @return
	 */
	protected static RDFDatatype getDatatype(String type) {
		if (type==null) return null;
		TypeMapper tm = TypeMapper.getInstance();
		RDFDatatype dt = tm.getTypeByName(type);
		if (dt == null) {
			// we've got a new (derived) datatype here
			dt = new BaseDatatype(type);
			tm.registerDatatype(dt);
		}
		return dt;
	}
	
	public String get_pcdata() {
		return _pcdata;
	}

	public void set_pcdata(String string) {
		_pcdata = string;
	}

	public String get_prefix() {
		return _prefix;
	}

	public void set_prefix(String string) {
		_prefix = string;
	}

	public XMLBean get_owner() {
		return _owner;
	}

	public void set_owner(XMLBean bean) {
		_owner = bean;
	}

	public URL get_location() {
		return _location;
	}

	public void set_location(File file) throws MalformedURLException {
		_location = file.toURL();
	}

	public void set_location(URL uri) {
		_location = uri;
	}

	/**
	 * @return Returns the _xmlns.
	 */
	public Properties get_xmlns() {
		return _xmlns;
	}

	/**
	 * @param _xmlns The _xmlns to set.
	 */
	public void set_xmlns(Properties _xmlns) {
		this._xmlns = _xmlns;
	}
	
	protected static URI relativize(URI base, URI uri) throws Exception {
		
		// scheme or authority mismatch
		if ((base.getScheme()!=null && !base.getScheme().equals(uri.getScheme()))
		 || (base.getAuthority()!=null && !base.getAuthority().equals(uri.getAuthority()))) return uri;	
		
		// for empty base path use the uri path in its entirety
		if (base.getPath().equals(""))
			return new URI(null,null,uri.getPath(),uri.getQuery(),uri.getFragment());
		// can't recreate empty path in URI resolution
		else if (uri.getPath().equals("")) return uri;
		
		// if the base includes a query then so must the relativization
		if (base.getQuery()!=null && uri.getQuery()==null) return uri;
		
		int n = base.getPath().lastIndexOf('/');
		if (n>=0) base = new URI(base.getScheme(),base.getHost(),base.getPath().substring(0,n),null);
		String dots = "";
		while(true) {
			if (uri.getPath().startsWith(base.getPath()+"/")) {
				URI rel = base.relativize(uri);
				return new URI(rel.getScheme(),rel.getHost(),dots+rel.getPath(),rel.getQuery(),rel.getFragment());
			}
			else if ((n = base.getPath().lastIndexOf('/'))>=0) {
				base = new URI(base.getScheme(),base.getHost(),base.getPath().substring(0,n),null);
				dots += "../";					
			} 
			else break;
		}
		return uri;
	}
	
	public static boolean isValidURI(String value) {
		try {
			String scheme = new URI(value).getScheme();
			return scheme!=null && (
					scheme.equals("http") || 
					scheme.equals("file") ||
					scheme.equals("gopher") ||
					scheme.equals("https") ||
					scheme.equals("imap") ||
					scheme.equals("ldap") ||
					scheme.equals("mailto") ||
					scheme.equals("mid") ||
					scheme.equals("news") ||
					scheme.equals("nfs") ||
					scheme.equals("nntp") ||
					scheme.equals("pop") ||
					scheme.equals("rtsp") ||
					scheme.equals("tel") ||
					scheme.equals("telnet") ||
					scheme.equals("urn") ||
					scheme.equals("ftp"));
		}
		catch (Exception e) {
			return false;
		}
	}

	public Node get_node() {
		return _node;
	}

	public void set_node(Node _node) {
		this._node = _node;
	}

	public XMLBean get_parent() {
		return _parent;
	}

	public static boolean equalNS(String n1, String n2) {
		if(n1==null || n2==null) return false;
		if (n1.endsWith("#")) n1 = n1.substring(0,n1.length()-1);
		if (n2.endsWith("#")) n2 = n2.substring(0,n2.length()-1);
		return n1.equals(n2);
	}
	
	protected static URI prune(URI uri) {
		if (uri==null) return null;
		try {
			int n = -1;
			if (uri.getPath()!=null) n = uri.getPath().lastIndexOf("/");
			String path;
			if (n<0) path = "/" ;
			else path = uri.getPath().substring(0,n+1);
			return new URI(uri.getScheme(),uri.getAuthority(),path,uri.getQuery(),uri.getFragment());
		}
		catch (Exception e) {
			return null;
		}
	}
	
	protected String getName(String uri) {
		if (uri==null) return null;
		int n = uri.lastIndexOf("/");
		if (n<0) return uri;
		return uri.substring(n+1);
	}
	
	public static String changeSuffix(String name, String suffix) {
		if (name==null) return null;
		int n = name.lastIndexOf('.');
		if (n<0 && suffix==null) return name;
		if (n<0) return name+"."+suffix;
		if (suffix==null) return name.substring(0, n);
		return name.substring(0,n+1) + suffix;
	}

	/** append filename to base if it ends with '/' */

	public static URI resolveBase(String base, File file) {
		try {
			if (base==null) return file.toURI();
			URI b = new URI(base);
			if (file==null) return b;
			if (base.toString().endsWith("/")) return b.resolve(new URI(file.getName()));
		} catch (URISyntaxException e) { // not fatal 
		}
		return null;
	}

 	public String getLang() {
		return lang;
	}

	public void setLang(String string) {
		lang = string;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSpace() {
		return space;
	}

	public void setSpace(String space) {
		this.space = space;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

} 
