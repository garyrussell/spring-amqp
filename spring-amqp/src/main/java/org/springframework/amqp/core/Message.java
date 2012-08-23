/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.amqp.core;

import java.nio.charset.Charset;

import org.springframework.amqp.utils.SerializationUtils;

/**
 * The 0-8 and 0-9-1 AMQP specifications do not define an Message class or interface. Instead, when performing an
 * operation such as basicPublish the content is passed as a byte-array argument and additional properties are passed in
 * as separate arguments. Spring AMQP defines a Message class as part of a more general AMQP domain model
 * representation. The purpose of the Message class is to simply encapsulate the body and properties within a single
 * instance so that the rest of the AMQP API can in turn be simpler.
 * 
 * @author Mark Pollack
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Dave Syer
 */
public class Message {

	private static final String ENCODING = Charset.defaultCharset().name();

	private final MessageProperties messageProperties;

	private final byte[] body;

	public Message(byte[] body, MessageProperties messageProperties) {
		this.body = body;
		this.messageProperties = messageProperties;
	}

	public byte[] getBody() {
		return this.body;
	}

	public MessageProperties getMessageProperties() {
		return this.messageProperties;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("(");
		buffer.append("Body:'" + this.getBodyContentAsString() + "'");
		if (messageProperties != null) {
			buffer.append("; ID:" + messageProperties.getMessageId());
			buffer.append("; Content:" + messageProperties.getContentType());
			buffer.append("; Headers:" + messageProperties.getHeaders());
			buffer.append("; Exchange:" + messageProperties.getReceivedExchange());
			buffer.append("; RoutingKey:" + messageProperties.getReceivedRoutingKey());
			buffer.append("; Reply:" + messageProperties.getReplyTo());
			buffer.append("; DeliveryMode:" + messageProperties.getDeliveryMode());
			buffer.append("; DeliveryTag:" + messageProperties.getDeliveryTag());
		}
		buffer.append(")");
		return buffer.toString();
	}

	private String getBodyContentAsString() {
		if (body == null) {
			return null;
		}
		try {
			String contentType = (messageProperties != null) ? messageProperties.getContentType() : null;
			if (MessageProperties.CONTENT_TYPE_SERIALIZED_OBJECT.equals(contentType)) {
				return SerializationUtils.deserialize(body).toString();
			}
			if (MessageProperties.CONTENT_TYPE_TEXT_PLAIN.equals(contentType)) {
				return new String(body, ENCODING);
			}
			if (MessageProperties.CONTENT_TYPE_JSON.equals(contentType)) {
				return new String(body, ENCODING);
			}
		} catch (Exception e) {
			// ignore
		}
		return body.toString()+"(byte["+body.length+"])"; // Comes out as '[B@....b' (so harmless)
	}

}
