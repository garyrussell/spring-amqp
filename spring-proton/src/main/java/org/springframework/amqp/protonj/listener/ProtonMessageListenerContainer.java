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

package org.springframework.amqp.protonj.listener;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;

import org.springframework.context.Lifecycle;

/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class ProtonMessageListenerContainer implements Lifecycle, Runnable {

	private final Messenger messenger = Messenger.Factory.create();

	private Consumer<String> consumer;

	private volatile boolean running;

	public void setConsumer(Consumer<String> consumer) {
		this.consumer = consumer;
	}

	@Override
	public void start() {
		try {
			this.messenger.setTimeout(1000);
			this.messenger.start();
			this.messenger.subscribe("amqp://localhost/foo");
			this.running = true;
			Executors.newSingleThreadExecutor().execute(this);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		this.running = false;
		this.messenger.stop();
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public void run() {
		while (this.running) {
			this.messenger.recv();
			while (this.messenger.incoming() > 0) {
				Message message = this.messenger.get();
				this.consumer.accept((String) ((AmqpValue) message.getBody()).getValue());
			}
		}
	}

}
