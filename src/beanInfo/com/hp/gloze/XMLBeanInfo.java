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

import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

import com.hp.gloze.www_w3_org_2001_XMLSchema.schema;

/**
 * @author stebat
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class XMLBeanInfo extends SimpleBeanInfo {
	
	private static final Class BEAN = XMLBean.class;
	
	/**
	 * @see java.beans.BeanInfo#getBeanDescriptor()
	 */
	public BeanDescriptor getBeanDescriptor() {
		BeanDescriptor bd = new BeanDescriptor(BEAN);
		try {
		bd.setValue("content", new Content());
		} catch (Exception e) {
			Gloze.logger.error(e.getMessage());
		}
		return bd;
	}

	/**
	 * @see java.beans.BeanInfo#getPropertyDescriptors()
	 */
	public PropertyDescriptor[] getPropertyDescriptors() {
		PropertyDescriptor lang, pcdata; 
		try {
			lang = new PropertyDescriptor("lang", BEAN);
			lang.setDisplayName("xml:lang");
			pcdata = new PropertyDescriptor("_pcdata", BEAN);
			return new PropertyDescriptor[] { 
				lang, 
				pcdata 
			};
		} catch (IntrospectionException e) {
			Gloze.logger.error(e.getMessage());
			return null;
		}
	}

	protected PropertyDescriptor describeAttribute(String name, Class bean) throws IntrospectionException {
		return new PropertyDescriptor(name, bean);	
	}

	protected PropertyDescriptor describeAttributeReserved(String name, Class bean) throws IntrospectionException {
		PropertyDescriptor pd;
		pd = new PropertyDescriptor("_"+name, bean);	
		pd.setDisplayName(name);
		return pd;
	}
	
	protected PropertyDescriptor describeAttribute(String xmlName, String javaName, Class bean) throws IntrospectionException {
		PropertyDescriptor pd;
		pd = new PropertyDescriptor(javaName, bean);
		pd.setDisplayName(xmlName);
		return pd;
	}


	protected PropertyDescriptor describeElement(String name, Class bean) throws IntrospectionException {
		PropertyDescriptor pd;
		pd = new PropertyDescriptor(name, bean);
		pd.setDisplayName(XMLBean.concatName(schema.XSD_URI, name));
		return pd;
	}

	protected PropertyDescriptor describeElementReserved(String name, Class bean) throws IntrospectionException {
		PropertyDescriptor pd;
		pd = new PropertyDescriptor("_"+name, bean);
		pd.setDisplayName(XMLBean.concatName(schema.XSD_URI, name));
		return pd;
	}

}
