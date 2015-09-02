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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.gloze.www_w3_org_2001_XMLSchema.schema;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.impl.UnionClassImpl;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

public class Restrictions {
	
	Map<String,Integer> minCard = new HashMap<String,Integer>();
	Map<String,Integer> maxCard = new HashMap<String,Integer>();
	Map<String,Set<Resource>> range = new HashMap<String,Set<Resource>>();
	// additional occurrences of any element
	Map<String,Integer> any = new HashMap<String,Integer>();
	Map<String,OntModel> model = new HashMap<String,OntModel>();

	public Map<String, Integer> getMinCard() {
		return minCard;
	}

	public Map<String, Integer> getMaxCard() {
		return maxCard;
	}

	public Map<String, Set<Resource>> getRange() {
		return range;
	}
	
	// sum any matching
	public int getAny(String uri, String tns) {
		int n=0;
		for (String ns: any.keySet()) {
			// anything at all
			if (ns.equals("##any") || (ns.equals("##other") && !uri.startsWith(tns))
					|| uri.startsWith(ns)) n += any.get(ns);
		}
		return n;
	}

	public Set<Resource> getRange(String uri) {
		Set<Resource> r = range.get(uri);
		if (r==null) range.put(uri, r = new HashSet<Resource>());
		return r;
	}
	
	public void addRange(OntModel ont, String uri, String type) {
		if (type==null) return;
		Set<Resource> s = getRange(uri);
		for (Resource r: s) {
			if (r.equals(type)) return;
		}
		s.add(ont.getResource(type));
	}

	public void addRange(String uri, Resource type) {
		if (type!=null) getRange(uri).add(type);
	}

	static public int product(int m, int n) {
		if (m==0 || n==0) return 0;
		if (m==Integer.MAX_VALUE || n==Integer.MAX_VALUE) return Integer.MAX_VALUE;
		int p = m*n;
		if (p<0) return Integer.MAX_VALUE;
		else return p;
	}
	
	static public int product(String m, int n) {
		if (m.equals("unbounded")) return product(Integer.MAX_VALUE,n);
		else return product(Integer.parseInt(m),n);
	}
	
	public static int sum(Integer m, int n) {
		if (m==null || n==Integer.MAX_VALUE) return n;
		if (m==Integer.MAX_VALUE) return m;
		int s = m + n ;
		if (s<0) return Integer.MAX_VALUE;
		else return s;
	}
	
	public void addMin(String uri, int min, boolean simple, OntModel mod) {
		min = sum(minCard.get(uri), min);
		// simple types (including QNames, IDREFs) have minCard of at most 1 because of possible duplication */
		if (simple) min = min>1?1:min;
		minCard.put(uri, min);
		model.put(uri, mod);
	}
	
	public void addMax(String uri, int max, OntModel mod) {
		maxCard.put(uri, sum(maxCard.get(uri), max));				
		model.put(uri, mod);
	}
	
	public void addMaxAny(String uri, int max) {
		any.put(uri, sum(any.get(uri), max));				
	}
	
	/** remove ID property restrictions (xs:ID is mapped to the resource URI) */
	
	boolean removeID(OntModel ont, Set<Resource> range) {
		boolean removed = false;
		Resource id = ont.getResource(schema.ID);
		if (range.contains(id)) removed = range.remove(id);
		for (Resource r: range) {
			if (schema.isID(r)) {
				schema.cleanup(r);
				removed = range.remove(r);
			}
		}
		return removed;
	}
	
	public void cleanup(OntModel ont, String uri) {
		Set<Resource> s = getRange().get(uri);
		if (s==null) return;
		for (Resource r: s) {
			if (r.isAnon()) cleanup(r, new HashSet<Resource>());
		}
	}
	
	public void cleanup(Resource rez, Set<Resource> visited) {
		visited.add(rez);
		Set<Resource> clean = new HashSet<Resource>();
		for (StmtIterator i = rez.listProperties(); i.hasNext(); ) {
			RDFNode obj = i.nextStatement().getObject();
			if (obj.isAnon() && obj instanceof Resource && !visited.contains(obj)) 
				clean.add((Resource) obj);
		}
		for (Resource r: clean) cleanup(r,visited);
		rez.removeProperties();
	}

	public void adjust(OntModel ont) {
		// remove all restrictions on xs:ID properties 
		Set<String> removeRange = new HashSet<String>();
		Set<String> removeProps = new HashSet<String>();
		for (String key: getRange().keySet()) {
			Set<Resource> s = getRange().get(key);
			if (removeID(ont,s) && s.size()==0) {
				removeRange.add(key);
				removeProps.add(key);
				minCard.remove(key);
				maxCard.remove(key);
			}
		}
		// remove anonymous, empty range restrictions
		for (String key: getRange().keySet()) {
			Set<Resource> s = getRange().get(key);
			Set<Resource> s1 = new HashSet<Resource>();
			for (Resource r: s) {
				if (voidClass(r)) {
					s1.add(r);
					schema.cleanup(r);
				}
			}
			for (Resource r1: s1) s.remove(r1);
			if (s.isEmpty()) removeRange.add(key);
		}
		for (String key: removeRange) range.remove(key);
		for (String key: removeProps) {
			// we can delete the property if it's not being used elsewhere
			Resource r = ont.getResource(key);
			ResIterator i1 = ont.listSubjectsWithProperty(OWL.onProperty,r);
			ResIterator i2 = ont.listSubjectsWithProperty(RDFS.subPropertyOf,r);
			// r should not be used in an existing restriction or subPropertyOf relation
			// ignore case where r is a subPropertyOf itself
			Resource s = null;
			while (i2.hasNext() && (s==null || s.equals(r))) 
				s = i2.nextResource();
			if (!i1.hasNext() && (s==null || s.equals(r))) 
				r.removeProperties();
		}
	}
	
	public static boolean voidClass(Resource type) {
		return type==null ||
		!( !type.isAnon()
		|| subClassOther(type) 
		|| type.hasProperty(OWL.oneOf)
		|| type.hasProperty(OWL.intersectionOf)
		|| type.hasProperty(OWL.equivalentClass)
		|| type.hasProperty(OWL.onProperty)
		|| type instanceof UnionClassImpl);
	}
	
	static boolean subClassOther(Resource cls) {
		for (StmtIterator si = cls.listProperties(RDFS.subClassOf); si.hasNext(); ) {
			RDFNode c = si.nextStatement().getObject();
			if (cls!=c) return true;
		}
		return false;
	}

	public OntModel getModel(String key) {
		return model.get(key);
	}

}
