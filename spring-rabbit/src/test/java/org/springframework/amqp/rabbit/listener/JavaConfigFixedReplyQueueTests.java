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

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.JavaConfigFixedReplyQueueTests.FixedReplyQueueConfig;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.test.BrokerRunning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * <b>NOTE:</b> This class is referenced in the reference documentation; if it is changed/moved, be
 * sure to update that documentation.
 *
 * @author Gary Russell
 * @since 1.3
 */

@ContextConfiguration(classes=FixedReplyQueueConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class JavaConfigFixedReplyQueueTests {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Rule
	public BrokerRunning brokerRunning = BrokerRunning.isRunning();

	/**
	 * Sends a message to a service that upcases the String and returns as a reply
	 * using a {@link RabbitTemplate} configured with a fixed reply queue and
	 * reply listener, configured with JavaConfig.
	 */
	@Test
	public void test() {
		assertEquals("FOO", rabbitTemplate.convertSendAndReceive("foo"));
	}

	@Configuration
	public static class FixedReplyQueueConfig {

		@Bean
		public ConnectionFactory rabbitConnectionFactory() {
			CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
			connectionFactory.setHost("localhost");
			return connectionFactory;
		}

		/**
		 * @return Rabbit template with fixed reply queue.
		 */
		@Bean
		public RabbitTemplate fixedReplyQRabbitTemplate() {
			RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory());
			template.setExchange(ex().getName());
			template.setRoutingKey("test");
			template.setReplyQueue(new Queue(ex().getName() + "/bar"));
			return template;
		}

		/**
		 * @return The reply listener container - the rabbit template is the listener.
		 */
		@Bean
		public SimpleMessageListenerContainer replyListenerContainer() {
			SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
			container.setConnectionFactory(rabbitConnectionFactory());
			container.setQueues(replyQueue());
			container.setMessageListener(fixedReplyQRabbitTemplate());
			return container;
		}

		/**
		 * @return The listener container that handles the request and returns the reply.
		 */
		@Bean
		public SimpleMessageListenerContainer serviceListenerContainer() {
			SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
			container.setConnectionFactory(rabbitConnectionFactory());
			container.setQueues(requestQueue());
			container.setMessageListener(new MessageListenerAdapter(new PojoListener()));
			return container;
		}

		/**
		 * @return a non-durable auto-delete exchange.
		 */
		@Bean
		public DirectExchange ex() {
			return new DirectExchange(UUID.randomUUID().toString(), false, true);
		}

		@Bean
		public Binding binding() {
			return BindingBuilder.bind(requestQueue()).to(ex()).with("test");
		}

		@Bean
		public Binding replyBinding() {
			return BindingBuilder.bind(replyQueue()).to(ex()).with("bar");
		}

		/**
		 * @return an anonymous (auto-delete) queue.
		 */
		@Bean
		public Queue requestQueue() {
			return new AnonymousQueue();
		}

		/**
		 * @return an anonymous (auto-delete) queue.
		 */
		@Bean
		public Queue replyQueue() {
			return new AnonymousQueue();
		}

		/**
		 * @return an admin to handle the declarations.
		 */
		@Bean
		public RabbitAdmin admin() {
			return new RabbitAdmin(rabbitConnectionFactory());
		}

		/**
		 * Listener that upcases the request.
		 */
		public static class PojoListener {

			public String handleMessage(String foo) {
				return foo.toUpperCase();
			}
		}
	}

}
