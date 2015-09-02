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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import org.w3c.dom.Node;

public class BeanLoader {
	private static final String APPINFO = "http://www.w3.org/2001/XMLSchema#appinfo";

	static final String PACKAGE = "com.hp.gloze";

	static HashMap<String,Class> classes = new HashMap<String,Class>();

	static Class load(Node node) throws IOException {
		Class c = null;

		// look up uri locally first
		String namespace = node.getNamespaceURI();
		String localName = node.getLocalName();
		String uri = XMLBean.concatName(namespace, localName);
		c = (Class) classes.get(uri);
		if (c != null)
			return c;

		// strip protocol and "//"
		String ns = namespace.substring(namespace.indexOf(':') + 3);
		ns = ns.replace('.', '_').replace('/', '_').replace('#', '_');
		while (ns.endsWith("_"))
			ns = ns.substring(ns.length() - 1);
		try {
			c = Class.forName(PACKAGE + "." + ns + "." + localName);
			classes.put(uri, c);
			return c;
		} catch (NoClassDefFoundError e1) {
		} catch (ClassNotFoundException e2) {
		}

		// try initializing the local name
		String iName = Character.toUpperCase(localName.charAt(0))
				+ localName.substring(1);
		try {
			c = Class.forName(PACKAGE + "." + ns + "." + iName);
			classes.put(uri, c);
			return c;
		} catch (NoClassDefFoundError e1) {
		} catch (ClassNotFoundException e2) {
		}

		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		String className = localName + ".class";
		// try case insensitive match over contents of parent folder
		try {
			URL parent = classLoader.getResource(PACKAGE + "." + ns);
			File[] contents = new File(parent.getFile()).listFiles();
			for (int i = 0; i < contents.length; i++) {
				String fName = contents[i].getName();
				if (fName.equalsIgnoreCase(className)) {
					// strip .class
					fName = fName.substring(0, fName.length() - 6);
					c = Class.forName(PACKAGE + "." + ns + "." + fName);
					classes.put(uri, c);
					return c;
				}
			}
		} catch (Exception x) {
			if (uri.equals(APPINFO))
				return null;
			Gloze.logger.warn("cannot load " + uri);
			//return XMLBean.class;
		}
		return null;
	}

}
