/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.amqp.protonj.core;

import java.io.IOException;

import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;

import org.springframework.context.Lifecycle;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class ProtonTemplate implements Lifecycle {

	private final Messenger messenger = Messenger.Factory.create(); // pool?

	private final String address;

	private volatile boolean running;

	public ProtonTemplate(String address) {
		this.address = address;
	}

	public synchronized void send(String subject, Object... bodies) throws IOException { // pool?
		Assert.state(this.running, "Template is not running");
		Message message = Message.Factory.create();
		message.setAddress(this.address);
		for (Object body : bodies) {
			 message.setBody(new AmqpValue(body));
			 messenger.put(message);
		}
		messenger.send();
	}

	@Override
	public void start() {
		try {
			this.messenger.start();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		this.running = true;
	}

	@Override
	public void stop() {
		this.messenger.stop();
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.isRunning();
	}

}
