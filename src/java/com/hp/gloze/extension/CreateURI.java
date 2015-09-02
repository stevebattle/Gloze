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

package com.hp.gloze.extension;

import java.net.URI;
import java.net.URLEncoder;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.reasoner.rulesys.BindingEnvironment;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
/**
 * @author steven.a.battle@googlemail.com
 */
public class CreateURI extends BaseBuiltin {

	/* (non-Javadoc)
	 * @see org.apache.jena.reasoner.rulesys.Builtin#getName()
	 */
	public String getName() {
		return "createURI";
	}
	/* (non-Javadoc)
	 * @see org.apache.jena.reasoner.rulesys.Builtin#getArgLength()
	 */
	public int getArgLength() {
		return 3;
	}
	/* (non-Javadoc)
	 * @see org.apache.jena.reasoner.rulesys.Builtin#getURI()
	 */
	public String getURI() {
		return "http://www.hp.com/gloze/extensions/createURI";
	}
	/* (non-Javadoc)
	 * @see org.apache.jena.reasoner.rulesys.Builtin#bodyCall(org.apache.jena.graph.Node[], int, org.apache.jena.reasoner.rulesys.RuleContext)
	 */
	public boolean bodyCall(Node[] args, int length, RuleContext context) {
		BindingEnvironment env = context.getEnv();
		Node namespace = env.getGroundVersion(args[0]);
		Node localName = env.getGroundVersion(args[1]);
		try {
			String ns = namespace.getLiteral().getValue().toString();
			String ln;
			try {
				ln = localName.getLiteral().getValue().toString();
			}
			// if the value is at odds with the datatype
			catch (Exception e) { ln = localName.getLiteral().toString(); }
			ln = URLEncoder.encode(ln,"UTF-8");
			URI uri = new URI(ns+(!ns.endsWith("#")&&!ns.endsWith("/")?"#":"")+ln);
			env.bind(args[2],NodeFactory.createURI(uri.toString()));
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
