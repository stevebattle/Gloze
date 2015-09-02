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

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.gloze.www_w3_org_2001_XMLSchema.element;

/**
 * @author steven.a.battle@googlemail.com
 *
 * This is the default content model, it allows any content to be added at any time.
 * (it permits any content state transition)
 */
public class Content extends XMLBean implements ContentIFace {
	
	/**
	 * @throws IntrospectionException
	 */
	public Content() throws IntrospectionException {
		super();
	}

	public boolean stop() {
		// vanilla machine can be stopped at any time
		return true;
	}

	public String needs() {
		return "";
	}

	public ContentIFace addNode(XMLBean bean, Node node) throws Exception {
		switch (node.getNodeType()) {

			case Node.ELEMENT_NODE :
				return addElement(bean, (Element) node);

			case Node.TEXT_NODE :
				return addText(bean, (CharacterData) node);

			case Node.CDATA_SECTION_NODE :
				return addCData(bean, (CharacterData) node);

			case Node.PROCESSING_INSTRUCTION_NODE :
				// accept PI but do nothing
				return this;

			case Node.DOCUMENT_NODE :
			case Node.ATTRIBUTE_NODE :
			case Node.ENTITY_REFERENCE_NODE :
				throw new Exception(
					"ignored "
						+ node.getLocalName()
						+ " in "
						+ bean.info.getBeanDescriptor().getDisplayName());
		}
		return this;
	}

	public ContentIFace addElement(XMLBean bean, Element element)
		throws Exception {
		XMLBean b = XMLBean.newShallowInstance(bean, element);
		if (b == null) return this;
		
		// lazy expansion of xs:element
		if (!(b instanceof element)) b.populate();
		
		if (!bean.addContent(b))
			Gloze.logger.warn("ignored element "+element.getLocalName()+" in "+bean.localName);
		return this;
	}

	public ContentIFace addText(XMLBean bean, CharacterData data)
		throws Exception {
		// accepts text but doesn't record it
		return this;
	}

	public ContentIFace addCData(XMLBean bean, CharacterData data)
		throws Exception {
		// accepts cdata but doesn't record it
		return this;
	}

	public boolean addAttributes(XMLBean bean, NamedNodeMap map)
		throws Exception {
		for (int i = 0; i < map.getLength(); i++) {
			if (!bean.addAttribute((Attr) map.item(i)))
				Gloze.logger.warn(
					"ignored attribute "
						+ ((Attr) map.item(i)).getLocalName()
						+ " in "
						+ bean.localName);
		}
		return true;
	}

	public boolean addAttribute(XMLBean bean, Attr attribute)
		throws Exception {
		return false;
	}

	public boolean addChildNodes(XMLBean bean, NodeList list)
		throws Exception {
		ContentIFace state = this;
		for (int i = 0; state != null && i < list.getLength(); i++) {
			state = state.addNode(bean, list.item(i));
			if (state == null) {
				Gloze.logger.warn(
					"ignored "
						+ list.item(i).getNodeName()
						+ " in "
						+ bean.info.getBeanDescriptor().getDisplayName());
				return false;
			}
		}
		if (!state.stop()) {
			Gloze.logger.warn(
				"cannot terminate "
					+ bean.info.getBeanDescriptor().getDisplayName()
					+ " needs "
					+ state.needs());
		}
		return state == null || state.stop();
	}

	public NodeList getChildNodes(XMLBean bean, Document doc)
		throws Exception {
		ContentNodeList nl = new ContentNodeList();
		PropertyDescriptor pd[] = bean.info.getPropertyDescriptors();
		for (int i = 0; i < pd.length; i++) {
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

	public NamedNodeMap getAttributes(XMLBean bean, Document doc)
		throws Exception {
		NamedNodeMap map = new ContentNamedNodeMap();
		PropertyDescriptor pd[] = bean.info.getPropertyDescriptors();
		for (int i = 0; i < pd.length; i++) {
			if (!pd[i].getPropertyType().isArray()
				&& pd[i].getPropertyType() == String.class) {
				Method getter = bean.getGetter(pd[i]);
				String value = (String) getter.invoke(bean, new Object[] {
				});
				if (value != null) {
					Attr attribute = doc.createAttribute(getDisplayName(pd[i]));
					attribute.setValue(value);
					map.setNamedItem(attribute);
				}
			} else if (
				pd[i].getPropertyType().isArray()
					&& pd[i].getPropertyType().getComponentType()
						== String.class) {
				// array attribute
			}
		}
		return map;
	}

}
