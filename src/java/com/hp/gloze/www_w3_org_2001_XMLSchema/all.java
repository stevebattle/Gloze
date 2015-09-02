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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.gloze.Content;
import com.hp.gloze.Context;
import com.hp.gloze.Restrictions;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;

/*! \page all all

The 'all' compositor allows all combinations of its child elements. 
The ordering of these elements may be significant.
Child elements may be optional (with minimum occurrences of 0) as in the example below with an element 'bar', but a \em missing 'foo' element.. 

\include all.xml
\include all.n3

This example demonstrates that compositors like 'all' don't explicitly appear in the RDF model
nor in the OWL ontology. In the schema below, 'foo' and 'bar' are defined locally so are not included
in the (global elements and attributes) closure. They are described as datatype properties with unknown range.
The types that appear in the schema are translated as 'allValuesFrom' restrictions in the context of a class definition.

\include all.xsd

Because 'foo' is optional, it has a maximum cardinality of 1 but an implied minimum cardinality of 0.

\include all.owl

\section annotationChildren Child components

- \ref element

*/


public class all extends Content {

	private String id, minOccurs = "1", maxOccurs = "1";
	private element[] element;

	public all() throws IntrospectionException {
	}
	
	public boolean needSeq(Set<String> names, Context ctx) {
		if (!maxOccurs.equals("0") && !maxOccurs.equals("1")) return true;
		return super.needSeq(names, ctx);
	}

	public int toRDF(Resource subject, Node node, int index, Seq seq, boolean mixed, Context ctx) 
	throws Exception {
		NodeList l = node.getChildNodes();
		int max = maxOccurs.equals("unbounded")? Integer.MAX_VALUE : Integer.parseInt(maxOccurs);
		int start = index, occurs=0;

		while (occurs<max) {
			Set<element> matched = new HashSet<element>();
			next: while (_any!=null && index < l.getLength()) {
				Node n = l.item(index);
				switch (n.getNodeType()) {
				case Node.TEXT_NODE:
					if (mixed) schema.textToRDF(subject, seq, n, ctx);
				case Node.COMMENT_NODE:
					index++;
					break;
				case Node.ELEMENT_NODE:
					// run through the choices
					for (int x=0; x < element.length ; x++) {
						if (matched.contains(element[x])) continue;
						int i = index;
						index = element[x].toRDF(subject, node, index, seq, mixed, ctx);
						if (i<index) {
							matched.add(element[x]);
							continue next;
						}
					}
					// no match
					break next;
				}
			}
			// were all elements consumed
			for (int i=0; element!=null && i<element.length; i++)
				if (!matched.contains(element[i]) && !element[i].getMinOccurs().equals("0"))
					return start;
			
			// escape if nothing actually matched
			if (matched.size()==0) break;

			occurs++;
			start = index;
		}
		// consume trailing mixed content
		if (mixed) return consumeMixed(l, index, subject, seq, ctx);
		return index;
	}

	public int toXML(Element elem, Resource rdf, int index, Set<Statement> pending, Context ctx) {
		Seq seq = rdf.getModel().getSeq(rdf);
		int max = maxOccurs.equals("unbounded")? Integer.MAX_VALUE : Integer.parseInt(maxOccurs);
		int start = index, occurs=0;
		
		while (occurs<max) {
			// record child components that have already been matched
			Set<element> matched = new HashSet<element>();
			// 'all' contains only elements
			nextContent: while (index < seq.size()) {
				index = produceMixed(seq,index,elem);
				for (int i = 0; element != null && i < element.length; i++) {
					if (matched.contains(element[i])) continue;
					int x = element[i].toXML(elem, rdf, index, pending,ctx);
					if (x>index) {
						index = x;
						matched.add(element[i]);
						continue nextContent;
					}
				}
				// no element matched
				break nextContent;
			}
			// consume pending statements
			Set<Statement> done = new HashSet<Statement>();
			nextElement: for (int i = 0; !pending.isEmpty() && element != null && i < element.length; i++) {
				if (matched.contains(element[i])) continue;
				for (Statement s: pending) {
					element sub = element[i].substitute(s,ctx);
					if (sub!=null) {
						sub.toXML(elem, s.getObject(),ctx);
						matched.add(element[i]);
						done.add(s);
						continue nextElement;
					}
				}
				// no pending statement matched element i, abandon 'all' if mandatory
				if (!element[i].getMinOccurs().equals("0")) return start;
			}
			// only side-effect pending if we matched all
			pending.removeAll(done);
			
			// were all elements consumed
			for (int i=0; element!=null && i<element.length; i++)
				if (!matched.contains(element[i]) && !element[i].getMinOccurs().equals("0"))
					return start;
			
			// escape if nothing actually matched
			if (matched.size()==0) break;
			
			occurs++;
			start = index;
		}
		return index;
	}

	public void toOWL(Restrictions rest, int min, int max, Context ctx) {
		min = Restrictions.product(getMinOccurs(),min);
		max = Restrictions.product(getMaxOccurs(),max);
		
		for (int i=0; element!=null && i<element.length; i++)
			element[i].toOWL(rest, min, max, ctx);
	}
	
	public element[] getElement() {
		return element;
	}

	public void setElement(element[] elements) {
		element = elements;
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

}
