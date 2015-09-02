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
public class DTDSequence extends Content {

	protected String minOccurs="1";
	protected String maxOccurs="1";
	XMLBean[] content;
	private int index;
	ContentIFace substate;

	public DTDSequence() throws IntrospectionException {
	}

	/* methods required by the content model */

	public DTDSequence(
		XMLBean[] content,
		String minOccurs,
		String maxOccurs,
		int index,
		ContentIFace substate)
		throws IntrospectionException {
		this.content = content;
		this.minOccurs = minOccurs;
		this.maxOccurs = maxOccurs;
		this.index = index;
		this.substate = substate;

	}

	public DTDSequence(XMLBean[] content, String minOccurs, String maxOccurs)
		throws IntrospectionException {
		this(content, minOccurs, maxOccurs, 0, null);
	}

	public DTDSequence(XMLBean[] content) throws IntrospectionException {
		// single occurrence by default
		this(content, "1", "1");
	}

	public ContentIFace addElement(XMLBean bean, Element element) throws Exception {
		if (maxOccurs.equals("0"))
			return null;

		int x = index;
		String min = minOccurs, max = maxOccurs;
		ContentIFace state = substate, nextState;

		// go maximum of once round potential loop
		for (int i = 0; i < content.length; i++) {
			// begin with start-substate
			if (state == null)
				state = (ContentIFace) content[x];

			// attempt to add the node
			nextState = state.addNode(bean, element);

			// return the next state if accepted
			if (nextState != null)
				return new DTDSequence(content, min, max, x, nextState);

			// we've finished with this sequence member
			if (!state.stop()) {
				return null;
			}

			// try next in sequence				
			if (++x >= content.length) {
				// we've run off the end of the sequence
				x = 0;
				if (max.equals("1"))
					return null;
				if (min.equals("1"))
					min = "0";
			}
			state = null;
		}
		return null;
	}

	public boolean stop() {
		int x = index;
		// we may be out of the loop
		if (substate == null) {
			if (minOccurs.equals("0"))
				return true;
		} else {
			// we are in the loop
			if (!substate.stop())
				return false;
			x++;
		}

		for (int i = x; i < content.length; i++) {
			if (!((ContentIFace) content[i]).stop())
				return false;
		}
		return true;
	}

	public String needs() {
		int x = index;
		if (stop())
			return "";
		if (substate != null) {
			if (!substate.stop())
				return substate.needs();
			x++;
		}
		for (int i = x; i < content.length; i++) {
			ContentIFace c = (ContentIFace) content[i];
			if (!c.stop())
				return c.needs();
		}
		return "a miracle";
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

	/**
	 * @return Returns the maxOccurs.
	 */
	public String getMaxOccurs() {
		return maxOccurs;
	}

	/**
	 * @param maxOccurs The maxOccurs to set.
	 */
	public void setMaxOccurs(String maxOccurs) {
		this.maxOccurs = maxOccurs;
	}

	/**
	 * @return Returns the minOccurs.
	 */
	public String getMinOccurs() {
		return minOccurs;
	}

	/**
	 * @param minOccurs The minOccurs to set.
	 */
	public void setMinOccurs(String minOccurs) {
		this.minOccurs = minOccurs;
	}

}

