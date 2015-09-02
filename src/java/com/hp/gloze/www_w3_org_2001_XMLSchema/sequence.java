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

import com.hp.gloze.ContentIFace;
import com.hp.gloze.Context;
import com.hp.gloze.DTDSequence;
import com.hp.gloze.Restrictions;
import com.hp.gloze.XMLBean;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;

/*! \page sequence sequence
 
 Compositors like xs:sequence are not represented in OWL because they are primarily concerned with the lexical form of a document.
 However, they are used to derive restrictions on the cardinality of individual properties appearing within a class.
 Cardinalities involving xs:sequence are derived by multiplying all nested cardinalities by the minimum and maximum number of occurences
 of that sequence. By default the minimum and maximum are 1, leaving the nested cardinalities unchanged.
 
 The example below demonstrates this multiplication at work. Element 'foo' has the default cardinality of 1. It's containing sequence
 has a minimum occurence of 0, so we derive a minimum cardinality of 1*0=0 on 'foo'. From the default maximum (1) we derive a maximum
 cardinality on 'foo' of 1*1=1. A minimum of 0 is no constraint at all, so only the maximum cardinality restriction appears in the OWL.
 
 \include sequence.xsd
 
 The element 'bar' is a little more interesting, having an unbounded number of occurrences and a default minimum
 cardinality of 1. The derived maximum cardinality is 1*unbounded=unbounded, and in effect unrestricted.
 Similarly, the derived minimum cardinality is 0*1=0, also unrestricted. There are therefore no cardinality
 restrictions on element 'bar'. The, possibly counter-intuitive, result is that 'bar' does not appear in the class definition.
 
 \include sequence.owl
 
 Note that the default type of elements 'foo' and 'bar' is xs:anyType, which is effectively unconstrained hence there are no
 ranges defined for either property. Also, because xs:anyType is a super-class of xs:anySimpleType, it is unknown whether or not
 'foo' and 'bar' are object or datatype properties (or both).
 
     \section sequenceChildren Child components

	- \ref element
	- \ref choice
	- \ref sequence
	- \ref any
	- \ref group

 */

public class sequence extends DTDSequence {

	private String id;
	private element[] element;
	private choice[] choice;
	private sequence[] sequence;
	private any[] any;
	private group[] group;
	
	public sequence() throws IntrospectionException {
	}

	/* methods required by the content model */

	public sequence(XMLBean[] content, String minOccurs, String maxOccurs,
			int index, ContentIFace substate) throws IntrospectionException {
		super(content, minOccurs, maxOccurs, index, substate);
	}

	public sequence(XMLBean[] content, String minOccurs, String maxOccurs)
			throws IntrospectionException {
		this(content, minOccurs, maxOccurs, 0, null);
	}

	public sequence(XMLBean[] content) throws IntrospectionException {
		// single occurrence by default
		this(content, "1", "1");
	}
	
	/* does this component require sequencing for disambiguation */
	
	public boolean needSeq(Set<String> names, Context ctx) {
		if (!maxOccurs.equals("0") && !maxOccurs.equals("1") && ctx.isSequenced()) return true;
		for (int i=0; _any!=null && i<_any.size(); i++)
			if (((XMLBean) _any.elementAt(i)).needSeq(names, ctx)) return true;
		return false;
	}

	public int toRDF(Resource subject, Node node, int index, Seq seq, boolean mixed, Context ctx) 
	throws Exception {
		NodeList l = node.getChildNodes();
		int max = maxOccurs.equals("unbounded")? Integer.MAX_VALUE : Integer.parseInt(maxOccurs);
		int start = index, occurs=0;
		
		while (occurs<max) {
			// index over children of the XML node
			if (index>=l.getLength()) break;
			
			// x runs over the sequence
			for (int x=0; _any!=null && index < l.getLength() && x<_any.size(); ) {
				Node n = l.item(index);
				// consume node n or exit
				switch (n.getNodeType()) {
				case Node.TEXT_NODE:
					if (mixed) schema.textToRDF(subject, seq, n, ctx);
				case Node.COMMENT_NODE:
					index++;
					break;
				case Node.ELEMENT_NODE:
					XMLBean b = (XMLBean) _any.elementAt(x);
					int i = index;
					if (b instanceof element) {
						element e = (element) b;
						i = e.toRDF(subject, node, index, seq, mixed,ctx);
						// stop if failure isn't optional
						if (i==index && !e.getMinOccurs().equals("0")) return start;
					}
					else if (b instanceof group) {
						i = ((group) b).toRDF(subject, node, index, seq, mixed,ctx);					
					}
					else if (b instanceof choice) {
						i = ((choice) b).toRDF(subject, node, index, seq, mixed,ctx);
					}
					else if (b instanceof any) {
						i = ((any) b).toRDF(subject, node, index, seq, mixed,ctx);
					}
					else if (b instanceof sequence) {
						i = ((sequence) b).toRDF(subject, node, index, seq, mixed,ctx);
					}
					index = i;
					x++;
				}
			}
			occurs++; // occurrence of the entire sequence
			start = index;
		}
		// consume trailing mixed content
		if (mixed) return consumeMixed(l, index, subject, seq, ctx);
		return index;
	}
	
	public int toXML(Element element, Resource rdf, int index, Set<Statement> pending, Context ctx) {
		int max = maxOccurs.equals("unbounded")? Integer.MAX_VALUE : Integer.parseInt(maxOccurs);
		int start = index, occurs=0;
		while (occurs<max) {
			// run through the schema sequence component
			for (int i = 0; _any != null && i < _any.size(); i++) {
				XMLBean bean = (XMLBean) _any.elementAt(i);	
				// subcomponents may consume both ordinal and non-ordinal statements
				if (bean instanceof element)
					index = ((element) bean).toXML(element, rdf, index, pending, ctx);
				else if (bean instanceof any) // any remaining
					index = ((any) bean).toXML(element, rdf, index, pending, ctx);
				else if (bean instanceof choice)
					index = ((choice) bean).toXML(element, rdf, index, pending, ctx);
				else if (bean instanceof group)
					index = ((group) bean).toXML(element, rdf, index, pending, ctx);
				else if (bean instanceof sequence)
					index = ((sequence) bean).toXML(element, rdf, index, pending, ctx);
			}
			if (index==start) break;
			occurs++;
			start = index;
		}
		return index;
	}
	
	public void toOWL(Restrictions rest, int min, int max, Context ctx) {
		min = Restrictions.product(getMinOccurs(),min);
		max = Restrictions.product(getMaxOccurs(),max);
		
		for (int i=0; sequence!=null && i<sequence.length; i++)
			sequence[i].toOWL(rest, min, max, ctx);
		for (int i=0; element!=null && i<element.length; i++)
			element[i].toOWL(rest, min, max, ctx);
		for (int i=0; choice!=null && i<choice.length; i++)
			choice[i].toOWL(rest, min, max, ctx);
		for (int i=0; group!=null && i<group.length; i++)
			group[i].toOWL(rest, min, max, ctx);		
		for (int i=0; any!=null && i<any.length; i++)
			any[i].toOWL(rest, min, max, ctx);		
	}
	
	/**
	 * Returns the element.
	 * 
	 * @return XMLBean[]
	 */
	public element[] getElement() {
		return element;
	}

	/**
	 * Sets the element.
	 * 
	 * @param element
	 *            The element to set
	 */
	public void setElement(element[] element) {
		this.element = element;
	}

	/**
	 * @return id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param set the id
	 */
	public void setId(String string) {
		id = string;
	}

	public any[] getAny() {
		return any;
	}

	public void setAny(any[] anies) {
		any = anies;
	}

	public sequence[] getSequence() {
		return sequence;
	}

	public void setSequence(sequence[] sequences) {
		sequence = sequences;
	}

	/**
	 * @return Returns the choice.
	 */
	public choice[] getChoice() {
		return choice;
	}
	/**
	 * @param choice The choice to set.
	 */
	public void setChoice(choice[] choice) {
		this.choice = choice;
	}

	public group[] getGroup() {
		return group;
	}

	public void setGroup(group[] group) {
		this.group = group;
	}
}
