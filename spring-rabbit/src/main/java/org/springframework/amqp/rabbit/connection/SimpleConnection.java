/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.amqp.rabbit.connection;

import java.io.IOException;

import org.springframework.amqp.rabbit.support.RabbitExceptionTranslator;
import org.springframework.util.ObjectUtils;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;

/**
 * Simply a Connection.
 * @author Dave Syer
 * @author Gary Russell
 * @since 1.0
 *
 */
public class SimpleConnection implements Connection {

	private final com.rabbitmq.client.Connection delegate;

	private final int closeTimeout;

	private final Address address;

	public SimpleConnection(com.rabbitmq.client.Connection delegate,
			int closeTimeout) {
		this(delegate, closeTimeout, null);
	}

	public SimpleConnection(com.rabbitmq.client.Connection delegate,
			int closeTimeout, Address address) {
		this.delegate = delegate;
		this.closeTimeout = closeTimeout;
		this.address = address;
	}

	@Override
	public Channel createChannel(boolean transactional) {
		try {
			Channel channel = delegate.createChannel();
			if (transactional) {
				// Just created so we want to start the transaction
				channel.txSelect();
			}
			return channel;
		} catch (IOException e) {
			throw RabbitExceptionTranslator.convertRabbitAccessException(e);
		}
	}

	@Override
	public void close() {
		try {
			// let the physical close time out if necessary
			delegate.close(closeTimeout);
		} catch (IOException e) {
			throw RabbitExceptionTranslator.convertRabbitAccessException(e);
		}
	}

	@Override
	public boolean isOpen() {
		return delegate != null
				&& (delegate.isOpen() || this.delegate.getClass().getSimpleName().contains("AutorecoveringConnection"));
	}

	@Override
	public Address getAddress() {
		return this.address;
	}

	@Override
	public String toString() {
		return "SimpleConnection@"
				+ ObjectUtils.getIdentityHexString(this)
				+ " [delegate=" + delegate
				+ " address=" + address + "]";
	}

}
