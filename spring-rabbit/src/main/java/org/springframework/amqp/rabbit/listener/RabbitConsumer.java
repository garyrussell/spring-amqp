/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.amqp.rabbit.listener;

import java.io.IOException;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;

import com.rabbitmq.client.Channel;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public interface RabbitConsumer {

	public abstract Channel getChannel();

	public abstract String getConsumerTag();

	/**
	 * Stop receiving new messages; drain the queue of any prefetched messages.
	 * @param shutdownTimeout how long (ms) to suspend the client thread.
	 */
	public abstract void setQuiesce(long shutdownTimeout);

	public abstract void start() throws AmqpException;

	public abstract void stop();

	/**
	 * Perform a rollback, handling rollback exceptions properly.
	 * @param ex the thrown application exception or error
	 * @throws Exception in case of a rollback error
	 */
	public abstract void rollbackOnExceptionIfNecessary(Throwable ex) throws Exception;

	/**
	 * Perform a commit or message acknowledgement, as appropriate.
	 *
	 * @param locallyTransacted Whether the channel is locally transacted.
	 * @throws IOException Any IOException.
	 * @return true if at least one delivery tag exists.
	 */
	public abstract boolean commitIfNecessary(boolean locallyTransacted) throws IOException;

	public void processNext(long timeout, Callback callback);

	interface Callback {

		void onMessage(Channel channel, Message message);

	}

}