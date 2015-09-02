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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author stebat
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DTDElement extends Content {

	/* These attributes represent the element content */
	protected String name, minOccurs = "1", maxOccurs = "1";
	
	public DTDElement() throws IntrospectionException {
	}

	public DTDElement(String name, String minOccurs, String maxOccurs)
		throws IntrospectionException {
		this.name = name;
		this.minOccurs = minOccurs;
		this.maxOccurs = maxOccurs;
	}

	public DTDElement(String namespace,String localName,String minOccurs,String maxOccurs)
	throws IntrospectionException {
		this(concatName(namespace, localName), minOccurs, maxOccurs);
	}
	
	public DTDElement(String namespace, String localName)
		throws IntrospectionException {
		this(namespace, localName, "1", "1"); // single occurrence default
	}

	static private ContentIFace newInstance(
		String name,
		String minOccurs,
		String maxOccurs)
		throws Exception {
		return new DTDElement(name, minOccurs, maxOccurs);
	}
	
	public boolean needSeq(Set<String> names, Context ctx) {
		return !maxOccurs.equals("0") && !maxOccurs.equals("1") && ctx.isSequenced();
	}

	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the minOccurs.
	 * @return String
	 */
	public String getMinOccurs() {
		return minOccurs;
	}

	/**
	 * Sets the minOccurs.
	 * @param minOccurs The minOccurs to set
	 */
	public void setMinOccurs(String minOccurs) {
		this.minOccurs = minOccurs;
	}

	/**
	 * Returns the maxOccurs.
	 * @return String
	 */
	public String getMaxOccurs() {
		return maxOccurs;
	}

	/**
	 * Sets the maxOccurs.
	 * @param maxOccurs The maxOccurs to set
	 */
	public void setMaxOccurs(String maxOccurs) {
		this.maxOccurs = maxOccurs;
	}

	/** methods required by the Content model */

	public ContentIFace addElement(XMLBean bean, Element element) throws Exception {
		if (maxOccurs.equals("0"))
			// invalid transition, we can only stop from here
			return null;
		
		// the bean may be set to ignore this element
		String namespace = element.getNamespaceURI();
		String localName = element.getLocalName();
		String uri = XMLBean.concatName(namespace, localName);
		
		// are we expecting this element?
		if (!name.equals(uri)) return null;
		
		String[] ignore = (String[]) bean.info.getBeanDescriptor().getValue("ignore");
		if (ignore==null || !Arrays.asList(ignore).contains(uri)) {
			// don't ignore the content

			XMLBean b = XMLBean.newInstance(bean,element);
//			XMLBean b = XMLBean.newShallowInstance(element);
			if (b==null) {
				Gloze.logger.warn("cannot create " + element.getNodeName());
				return null;
			}
			
			// check the name
			//if (!name.equals(b.name)) return null;
			
			// the new bean gets a copy of its parents owner
//			b.set_owner(bean.get_owner());
			
//			b.elaborateInstance(element);
			if (!bean.addContent(b)) return null;	
		} 
		// otherwise accept (but don't save) the content

		// we can't accept any more
		if (maxOccurs.equals("1"))
			return newInstance(name, "0", "0");
		// we can now accept zero or more
		if (minOccurs.equals("1"))
			return newInstance(name, "0", maxOccurs);
		// else no state-change
		return this;
	}

	public boolean stop() {
		return minOccurs.equals("0");
	}
	
	public String needs() {
		return stop()?"":name;
	}

	public NodeList getChildNodes(XMLBean bean, Document doc)
		throws Exception {
		ContentNodeList nl = new ContentNodeList();
		PropertyDescriptor pd[] = bean.info.getPropertyDescriptors();
		for (int i = 0; i < pd.length; i++) {
			// The property display name is fully qualified, unlike name which refers to the java attribute
			if (pd[i].getDisplayName() == null
				|| !pd[i].getDisplayName().equals(name))
				continue;

			if (pd[i].getPropertyType().isArray()) {
				Class c = pd[i].getPropertyType().getComponentType();
				if (XMLBean.class.isAssignableFrom(c)) {
					// multiple elements
					Method getter = bean.getGetter(pd[i]);
					XMLBean[] children;
					children = (XMLBean[]) getter.invoke(bean, (Object[]) null);
					if (children == null)
						continue;
					for (int j = 0; j < children.length; j++) {
						nl.add(children[j].getElement(doc));
					}
				}
			} else if (!pd[i].getPropertyType().isArray()) {
				Class c = pd[i].getPropertyType();
				if (XMLBean.class.isAssignableFrom(c)) {
					// solo element
					Method getter = bean.getGetter(pd[i]);
					XMLBean child =
						(XMLBean) getter.invoke(bean, new Object[] {
					});
					if (child != null) {
						nl.add(child.getElement(doc));
					}
				}
			}
		}
		return nl;
	}

}

