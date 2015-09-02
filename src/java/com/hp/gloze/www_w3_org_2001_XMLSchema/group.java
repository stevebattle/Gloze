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

import com.hp.gloze.Content;
import com.hp.gloze.Context;
import com.hp.gloze.Gloze;
import com.hp.gloze.Restrictions;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;

/*! \page group group
 
 The group component allows groups of elements to be combined into reusable groups.
 From a modelling perspective we regard them as syntactic sugar with no counterpart
 in OWL. However, group references act like compositors in that they allow the schema
 designer to indicate how many times the group may occur.
 
 The minimum and maximum occurrences of the group reference are multiplied by the cardinalities of
 the group members. In the example below, the element 'barfoo' references a group that has no
 occurrences.
 
 \include group.xsd
 \include group.owl
 
       \section groupChildren Child components

	- \ref sequence
	- \ref choice
	- \ref all

 */

public class group extends Content {

	private String id, maxOccurs = "1", minOccurs="1", ref;
	private String name;
	private all all;
	private choice choice;
	private sequence sequence;
	
	public group() throws IntrospectionException {
	}

	public boolean needSeq(Set<String> names, Context ctx) {
		group g = get_ref(ctx.getModel(),ctx);
		if (g!=null && g.needSeq(names, ctx)) return true;
		if (!maxOccurs.equals("1") && !maxOccurs.equals("0")) return true;
		return super.needSeq(names, ctx);
	}
	
	String createURI(Model model, Context ctx) {
		schema xs = (schema) this.get_owner();
		if (getName()!=null) return xs.expandName(getName(),model,ctx);
		else return expandQName(ctx.getDefaultNS(),null,getRef(),get_node(),model);
	}

	/** resolve references to simple and complex types, groups and attribute groups */
	
	public void resolve(Model model, Context ctx) {
		ctx.putRef(this,get_ref(model,ctx));
		super.resolve(model, ctx);
	}
	
	private group get_ref(Model model, Context ctx) {
		group g = (group) ctx.getRef(this);
		if (g!=null) return g;
		if (ref!=null) {
			g = ctx.getGroup(createURI(model,ctx));
			if (g!=null) ctx.putRef(this,g);
			else Gloze.logger.warn("no such group: "+ref);
		}
		return g;
	}
	
	public int toRDF(Resource subject, Node node, int index, Seq seq, boolean mixed, Context ctx) 
	throws Exception {	
		group g = get_ref(ctx.getModel(), ctx);
		if (g!=null) {
			// a referenced group may have multiple occurrences
			int max = maxOccurs.equals("unbounded")? Integer.MAX_VALUE : Integer.parseInt(maxOccurs);
			if (max==0) return index;
			int i = index, occurs = 0;
			do {
				index = i;
				i = g.toRDF(subject,node,index,seq,mixed,ctx);
				if (i>index) occurs++;
			} while (i>index && occurs<max);
			return i;
		}
		else if (all != null) return all.toRDF(subject, node, index, seq, mixed,ctx);
		else if (sequence != null) return sequence.toRDF(subject, node, index, seq, mixed,ctx);
		else if (choice != null) return choice.toRDF(subject, node, index, seq, mixed,ctx);
		return index;
		
	}
	
	/*
	 * done includes consumed statements
	 * names includes the element names in this content 
	 * both super-content (inherited) and sub-content (sub-choice, sub-seq, sub-all)
	 */
	public int toXML(Element e, Resource rdf, int index, Set<Statement> pending, Context ctx) {
		group g = get_ref(ctx.getModel(),ctx);
		if (g!=null) {
			int max = maxOccurs.equals("unbounded")? Integer.MAX_VALUE : Integer.parseInt(maxOccurs);
			if (max==0) return index;
			int i = index, occurs = 0;
			do {
				index = i;
				i = g.toXML(e,rdf,index,pending,ctx); // group reference
				if (i>index) occurs++;
			} while (i>index && occurs<max);
			return i;
		}
		else if (all != null) index = all.toXML(e, rdf, index, pending,ctx);
		else if (sequence != null) index =  sequence.toXML(e, rdf, index, pending,ctx);
		else if (choice != null) index = choice.toXML(e, rdf, index, pending,ctx);		
		return index;
	}

	public void toOWL(Restrictions rest, int min, int max, Context ctx) {
		schema xs = (schema) this.get_owner();
		min = Restrictions.product(getMinOccurs(),min);
		max = Restrictions.product(getMaxOccurs(),max);
		
		group g = get_ref(xs.ont,ctx);
		if (g!=null) g.toOWL(rest, min, max, ctx);
		else {
			if (sequence != null) sequence.toOWL(rest, min, max, ctx);		
			if (all != null) all.toOWL(rest, min, max, ctx);		
			if (choice != null) choice.toOWL(rest, min, max, ctx);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public sequence getSequence() {
		return sequence;
	}

	public void setSequence(sequence sequence) {
		this.sequence = sequence;
	}

	public all getAll() {
		return all;
	}

	public void setAll(all all) {
		this.all = all;
	}

	public choice getChoice() {
		return choice;
	}

	public void setChoice(choice choice) {
		this.choice = choice;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMaxOccurs() {
		return maxOccurs;
	}

	public void setMaxOccurs(String maxOccurs) {
		this.maxOccurs = maxOccurs;
	}

	public String getMinOccurs() {
		return minOccurs;
	}

	public void setMinOccurs(String minOccurs) {
		this.minOccurs = minOccurs;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

}
