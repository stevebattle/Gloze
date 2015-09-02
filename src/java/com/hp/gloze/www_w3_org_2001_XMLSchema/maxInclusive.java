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
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.hp.gloze.XMLBean;

public class maxInclusive extends XMLBean {

	private String fixed = "false";
	private String id, value;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public maxInclusive() throws IntrospectionException {
	}


	public boolean isValid(String value, String type) {
		try {
			if (type.equals(schema.XSD_URI+"#date") 
			 || type.equals(schema.XSD_URI+"#time")
			 || type.equals(schema.XSD_URI+"#dateTime")
			 || type.equals(schema.XSD_URI+"#gDay")
			 || type.equals(schema.XSD_URI+"#gMonth")
			 || type.equals(schema.XSD_URI+"#gMonthDay")
			 || type.equals(schema.XSD_URI+"#gYearMonth")) {
				XMLGregorianCalendar max = schema.getCalendar(this.value);
				XMLGregorianCalendar val = schema.getCalendar(value);			
				return max.compare(val)==DatatypeConstants.GREATER || 
					max.compare(val)==DatatypeConstants.EQUAL;
			}
			else if (type.equals(schema.XSD_URI+"#duration")) {
				Duration max = schema.getDuration(this.value);
				Duration val = schema.getDuration(value);
				return max.isLongerThan(val) || max.equals(val);
			}
			else if (type.equals(schema.XSD_URI+"#integer")
				  || type.equals(schema.XSD_URI+"#negativeInteger")
				  || type.equals(schema.XSD_URI+"#positiveInteger")
				  || type.equals(schema.XSD_URI+"#nonNegativeInteger")
				  || type.equals(schema.XSD_URI+"#nonPositiveInteger")
				  || type.equals(schema.XSD_URI+"#long")
				  || type.equals(schema.XSD_URI+"#unsignedInt")
				  || type.equals(schema.XSD_URI+"#unsignedLong")) {
				return new BigInteger(this.value).compareTo(new BigInteger(value))>=0;
			}
			else if (type.equals(schema.XSD_URI+"#decimal")) {
				return new BigDecimal(this.value).compareTo(new BigDecimal(value))>=0;
			}
			return Integer.parseInt(this.value) >= Integer.parseInt(value);
		}
		catch (Exception e) {
			// unable to parse values
			return false;
		}
	}


	public String getFixed() {
		return fixed;
	}

	public void setFixed(String fixed) {
		this.fixed = fixed;
	}

}
