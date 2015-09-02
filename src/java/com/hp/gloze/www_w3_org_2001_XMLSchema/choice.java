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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.gloze.Content;
import com.hp.gloze.Context;
import com.hp.gloze.Restrictions;
import com.hp.gloze.XMLBean;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;

/*! \page choice choice
 The choice compositor determines how property cardinalities are derived.
 Because only one choice may occur, each has a minimum cardinality of 0. 
 The maximum is set by maxOccurs. These values are multiplied by the cardinalities
 of nested components to produce the derived cardinailities.

 In the following example, the minimum cardinailites of both 'foo' and 'bar' are multipled by 0, so
 there is no minimum restriction on either property. In addition, 'foo' has no maximum limit so foo is
 unconstrained in the range of 'foobar'. Property 'bar' retains it's default cardinality of 1.
 
 \include choice.xsd
 
 The minimum cardinality of 'foo' is 0*1.
 The maximum cardinality of 'foo' is 1*unbounded.
 This has the interesting, and perhaps counter-intuitive, effect that no restrictions on 'foo' are defined in the
 class definition.
 
 The minimum cardinality of 'bar' is 0*1.
 The maximum cardinality of 'bar' is 1*1.
 
 \include choice.owl
 
      \section sequenceChildren Child components

	- \ref element
	- \ref sequence
	- \ref choice
	- \ref group
	- \ref any

 */

public class choice extends Content {

	private String id, minOccurs = "1", maxOccurs = "1";

	private element[] element;
	private sequence[] sequence;
	private choice[] choice;
	private group[] group;
	private any[] any;

	public choice() throws IntrospectionException {
	}

	boolean isIteratedElement(element e) {
		return !e.getMaxOccurs().equals("1") && !e.getMaxOccurs().equals("0");
	}
	
	public boolean needSeq(Set<String> names, Context ctx) {
		if (!maxOccurs.equals("0") && !maxOccurs.equals("1")) return true;
		return super.needSeq(names, ctx);
	}
	
	/* the policy on mixed content is to add all text until a choice is made */

	public int toRDF(Resource subject, Node node, int index, Seq seq, boolean mixed, Context ctx) 
	throws Exception {
		// index over XML content
		NodeList l = node.getChildNodes();
		int max = maxOccurs.equals("unbounded")? Integer.MAX_VALUE : Integer.parseInt(maxOccurs);
		int occurs=0;
		int ind = -1;
		next: while (_any!=null && index < l.getLength() && ind<index && occurs<max) {
			ind = index;
			Node n = l.item(index);
			switch (n.getNodeType()) {
			case Node.TEXT_NODE:
				if (mixed) schema.textToRDF(subject, seq, n, ctx);
			case Node.COMMENT_NODE:
				index++;
				break;
			case Node.ELEMENT_NODE:
				// run through the choices
				for (int x=0; x < _any.size(); x++) {
					int i = index;
					XMLBean b = (XMLBean) _any.elementAt(x);
					index = b.toRDF(subject, node, index, seq, mixed, ctx);
					if (i<index) {
						occurs++;
						continue next;
					}
				}
			}
		}
		// consume trailing mixed content
		if (mixed) return consumeMixed(l, index, subject, seq, ctx);
		return index;
	}

	public int toXML(Element element, Resource rdf, int index, Set<Statement> pending, Context ctx) {
		int max = maxOccurs.equals("unbounded")? Integer.MAX_VALUE : Integer.parseInt(maxOccurs);
		int occurs=0;
		// consume mixed values, so this doesn't occur repeatedly in every branch
		while (occurs<max) {
			index = produceMixed(rdf.getModel().getSeq(rdf),index,element);
			int x = index;
			for (int i = 0; _any != null && i < _any.size() && x==index; i++) {
				XMLBean bean = (XMLBean) _any.elementAt(i);
				if (bean instanceof element) {
					index = ((element) bean).toXML(element, rdf, index, pending, ctx);
				} else if (bean instanceof any) { // any remaining
					index = ((any) bean).toXML(element, rdf, index, pending, ctx);
				} else if (bean instanceof choice) {
					index = ((choice) bean).toXML(element, rdf, index, pending, ctx);				
				} else if (bean instanceof sequence) {
					index = ((sequence) bean).toXML(element, rdf, index, pending, ctx);				
				} else if (bean instanceof group) {
					index = ((group) bean).toXML(element, rdf, index, pending, ctx);				
				}
			}
			if (x<index) occurs++;
			else break;
		}
		return index;
	}

	public void toOWL(Restrictions rest, int min, int max, Context ctx) {
		// choice items may not appear at all
		int choices = (element!=null?element.length:0) 
				+ (sequence!=null?sequence.length:0)
				+ (choice!=null?choice.length:0)
				+ (group!=null?group.length:0)
				+ (any!=null?any.length:0);
		if (choices>1) min = 0;
		else min = Restrictions.product(getMinOccurs(),min);
		max = Restrictions.product(getMaxOccurs(),max);
		
		for (int i=0; element!=null && i<element.length; i++)
			element[i].toOWL(rest, min, max, ctx);
		for (int i=0; sequence!=null && i<sequence.length; i++)
			sequence[i].toOWL(rest, min, max, ctx);
		for (int i=0; choice!=null && i<choice.length; i++)
			choice[i].toOWL(rest, min, max, ctx);
		for (int i=0; group!=null && i<group.length; i++)
			group[i].toOWL(rest, min, max, ctx);
		for (int i=0; any!=null && i<any.length; i++)
			any[i].toOWL(rest, min, max, ctx);
	}

	public String getId() {
		return id;
	}

	public String getMaxOccurs() {
		return maxOccurs;
	}

	public String getMinOccurs() {
		return minOccurs;
	}

	public void setId(String string) {
		id = string;
	}

	public void setMaxOccurs(String string) {
		maxOccurs = string;
	}

	public void setMinOccurs(String string) {
		minOccurs = string;
	}

	public element[] getElement() {
		return element;
	}

	public sequence[] getSequence() {
		return sequence;
	}

	public void setElement(element[] elements) {
		element = elements;
	}

	public void setSequence(sequence[] sequences) {
		sequence = sequences;
	}

	public choice[] getChoice() {
		return choice;
	}

	public void setChoice(choice[] choice) {
		this.choice = choice;
	}

	public any[] getAny() {
		return any;
	}

	public void setAny(any[] any) {
		this.any = any;
	}

	public group[] getGroup() {
		return group;
	}

	public void setGroup(group[] group) {
		this.group = group;
	}

}
