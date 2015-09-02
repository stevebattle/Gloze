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

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * @author stebat
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DTDAny extends Content {
	
	String property;
	boolean cdata = false;

	private DTDAny(String property, boolean cdata) throws IntrospectionException {
		this.property = property;
		this.cdata = cdata;		
	}

	public DTDAny(String property) throws IntrospectionException {
		this(property, false);
	}

	public DTDAny() throws IntrospectionException {
		this("_pcdata"); // default
	}
	
	public ContentIFace addText(XMLBean bean, CharacterData data)
		throws Exception {
		bean.addProperty(property, data.getNodeValue());
		return this;
	}

	public ContentIFace addCData(XMLBean bean, CharacterData data)
		throws Exception {
		bean.addProperty(property, data.getNodeValue());
		return new DTDAny(property,true);
	}

	public NamedNodeMap getAttributes(XMLBean bean, Document doc)
		throws Exception {
		NamedNodeMap map = super.getAttributes(bean, doc);
		// pcdata properties don't appear as element attributes
		map.removeNamedItem(property);
		return map;
	}

	public NodeList getChildNodes(XMLBean bean, Document doc)
		throws Exception {
		ContentNodeList list = new ContentNodeList();
		String[] values = bean.getPropertyValues(property);
		for (int i=0; values!=null && i<values.length; i++) {
			if (cdata)
				list.add(doc.createCDATASection(values[i]));
			else
				list.add(doc.createTextNode(values[i]));		
		}
		return list;
	}

}
