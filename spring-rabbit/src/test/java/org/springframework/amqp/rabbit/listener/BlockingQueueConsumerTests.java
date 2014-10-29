/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.beans.DirectFieldAccessor;

import com.rabbitmq.client.Channel;

/**
 * @author Gary Russell
 * @since 1.0.1
 *
 */
public class BlockingQueueConsumerTests {

	@Test
	public void testRequeue() throws Exception {
		Exception ex = new RuntimeException();
		testRequeueOrNotDefaultYes(ex, true);
	}

	@Test
	public void testRequeueNullException() throws Exception {
		Exception ex = null;
		testRequeueOrNotDefaultYes(ex, true);
	}

	@Test
	public void testDontRequeue() throws Exception {
		Exception ex = new AmqpRejectAndDontRequeueException("fail");
		testRequeueOrNotDefaultYes(ex, false);
	}

	@Test
	public void testDontRequeueNested() throws Exception {
		Exception ex = new RuntimeException(
				new RuntimeException(new AmqpRejectAndDontRequeueException(
						"fail")));
		testRequeueOrNotDefaultYes(ex, false);
	}

	@Test
	public void testRequeueDefaultNot() throws Exception {
		Exception ex = new RuntimeException();
		testRequeueOrNotDefaultNo(ex, false);
	}

	@Test
	public void testRequeueNullExceptionDefaultNot() throws Exception {
		Exception ex = null;
		testRequeueOrNotDefaultNo(ex, false);
	}

	@Test
	public void testDontRequeueDefaultNot() throws Exception {
		Exception ex = new AmqpRejectAndDontRequeueException("fail");
		testRequeueOrNotDefaultNo(ex, false);
	}

	@Test
	public void testDontRequeueNestedDefaultNot() throws Exception {
		Exception ex = new RuntimeException(
				new RuntimeException(new AmqpRejectAndDontRequeueException(
						"fail")));
		testRequeueOrNotDefaultNo(ex, false);
	}

	/**
	 * We should always requeue if the exception is a
	 * {@link MessageRejectedWhileStoppingException}.
	 */
	@Test
	public void testDoRequeueStoppingDefaultNot() throws Exception {
		Exception ex = new MessageRejectedWhileStoppingException();
		testRequeueOrNotDefaultNo(ex, true);
	}

	private void testRequeueOrNotDefaultYes(Exception ex, boolean expectedRequeue)
			throws Exception, IOException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Channel channel = mock(Channel.class);
		RabbitConsumer blockingQueueConsumer = new BlockingQueueConsumer(connectionFactory,
				new DefaultMessagePropertiesConverter(), new ActiveObjectCounter<BlockingQueueConsumer>(),
				AcknowledgeMode.AUTO, true, 1, "testQ");
		testRequeueOrNotGuts(ex, expectedRequeue, channel, blockingQueueConsumer);
	}

	private void testRequeueOrNotDefaultNo(Exception ex, boolean expectedRequeue)
			throws Exception, IOException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Channel channel = mock(Channel.class);
		boolean defaultRequeueRejected = false;
		RabbitConsumer blockingQueueConsumer = new BlockingQueueConsumer(connectionFactory,
				new DefaultMessagePropertiesConverter(), new ActiveObjectCounter<BlockingQueueConsumer>(),
				AcknowledgeMode.AUTO, true, 1, defaultRequeueRejected, "testQ");
		testRequeueOrNotGuts(ex, expectedRequeue, channel, blockingQueueConsumer);
	}

	private void testRequeueOrNotGuts(Exception ex, boolean expectedRequeue,
			Channel channel, RabbitConsumer blockingQueueConsumer)
			throws Exception, IOException {
		DirectFieldAccessor dfa = new DirectFieldAccessor(blockingQueueConsumer);
		dfa.setPropertyValue("channel", channel);
		Set<Long> deliveryTags = new HashSet<Long>();
		deliveryTags.add(1L);
		dfa.setPropertyValue("deliveryTags", deliveryTags);
		blockingQueueConsumer.rollbackOnExceptionIfNecessary(ex);
		Mockito.verify(channel).basicReject(1L, expectedRequeue);
	}

}
