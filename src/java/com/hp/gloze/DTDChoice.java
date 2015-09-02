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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author stebat
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DTDChoice extends Content {

	private String minOccurs, maxOccurs;
	XMLBean[] content;
	ContentIFace substate;

	public DTDChoice() throws IntrospectionException {
	}

	/* methods required by the content model */

	public DTDChoice(
		XMLBean[] content,
		String minOccurs,
		String maxOccurs,
		ContentIFace substate)
		throws IntrospectionException {
		this.content = content;
		this.minOccurs = minOccurs;
		this.maxOccurs = maxOccurs;
		this.substate = substate;
	}

	public DTDChoice(XMLBean[] content, String minOccurs, String maxOccurs)
		throws IntrospectionException {
		this(content, minOccurs, maxOccurs, null);
	}

	public DTDChoice(XMLBean[] content) throws IntrospectionException {
		// single occurrence by default
		this(content, "1", "1");
	}

	public ContentIFace addElement(XMLBean bean, Element element) throws Exception {
		if (maxOccurs.equals("0"))
			return null;

		String min = minOccurs, max = maxOccurs;
		ContentIFace state, nextState ;
		
		// can the current substate take it?
		if (substate!=null) {
			state = substate.addNode(bean, element);
			if (state!=null)
				return new DTDChoice(content, min, max, state);
			if (!substate.stop()) return null;
			// otherwise we've stopped it, can we make another choice?
			if (max.equals("1"))
				return null;
			if (min.equals("1"))
				min = "0";			
		}

		// try each choice in turn
		for (int i = 0; i < content.length; i++) {
			// begin with start-substate
			state = (ContentIFace) content[i];

			// attempt to add the node
			nextState = state.addNode(bean, element);

			// return the next state if accepted
			if (nextState != null)
				return new DTDChoice(content, min, max, nextState);
		}
		return null;
	}

	public boolean stop() {
		// we may be out of the loop
		if (substate == null) {
			if (minOccurs.equals("0"))
				return true;
		} else
			// we have made a choice
				return substate.stop();
		return false;
	}

	public String needs() {
		if (stop())
			return "";
		if (substate != null) {
			if (!substate.stop())
				return substate.needs();
		}
		else {
			StringBuffer s = new StringBuffer();
			for (int i = 0; i < content.length; i++) {
			ContentIFace c = (ContentIFace) content[i];
			if (!c.stop())
				s.append(c.needs());
				s.append(", ");
			}
			return s.toString();
		}
		return "nothing";
	}

	public NodeList getChildNodes(XMLBean bean, Document doc)
		throws Exception {
		ContentNodeList nl = new ContentNodeList();
		for (int i = 0; i < content.length; i++) {
			NodeList l = ((ContentIFace) content[i]).getChildNodes(bean, doc);
			for (int j = 0; j < l.getLength(); j++)
				nl.add(l.item(j));
		}
		return nl;
	}
}