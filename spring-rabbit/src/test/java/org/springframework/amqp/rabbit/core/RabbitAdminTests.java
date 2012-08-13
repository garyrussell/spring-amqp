package org.springframework.amqp.rabbit.core;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.context.support.GenericApplicationContext;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

public class RabbitAdminTests {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testSettingOfNullRabbitTemplate() {
		ConnectionFactory connectionFactory = null;
		try {
			new RabbitAdmin(connectionFactory);
			fail("should have thrown IllegalStateException when RabbitTemplate is not set.");
		} catch (IllegalArgumentException e) {

		}
	}

	@Test
	public void testNoFailOnStartupWithMissingBroker() throws Exception {
		SingleConnectionFactory connectionFactory = new SingleConnectionFactory("foo");
		connectionFactory.setPort(434343);
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.getBeanFactory().registerSingleton("foo", new Queue("queue"));
		RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
		rabbitAdmin.setApplicationContext(applicationContext);
		rabbitAdmin.setAutoStartup(true);
		rabbitAdmin.afterPropertiesSet();
	}

	@Test
	public void testFailOnFirstUseWithMissingBroker() throws Exception {
		SingleConnectionFactory connectionFactory = new SingleConnectionFactory("foo");
		connectionFactory.setPort(434343);
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.getBeanFactory().registerSingleton("foo", new Queue("queue"));
		RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
		rabbitAdmin.setApplicationContext(applicationContext);
		rabbitAdmin.setAutoStartup(true);
		rabbitAdmin.afterPropertiesSet();
		exception.expect(IllegalArgumentException.class);
		rabbitAdmin.declareQueue();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFailureDuringDeclarations() throws Exception {
		com.rabbitmq.client.ConnectionFactory mockConnectionFactory = mock(com.rabbitmq.client.ConnectionFactory.class);
		Connection mockConnection = mock(Connection.class);
		Channel mockChannel = mock(Channel.class);

		when(mockConnectionFactory.newConnection((ExecutorService) null)).thenReturn(mockConnection);
		when(mockConnection.isOpen()).thenReturn(true);
		when(mockConnection.createChannel()).thenReturn(mockChannel);

		final AtomicInteger count = new AtomicInteger();
		doAnswer(new Answer<Object>(){
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				System.out.println(invocation.getArguments()[0]);
				if (invocation.getArguments()[0].equals("foo") && count.incrementAndGet() == 1) {
					throw new RuntimeException("failDeclare");
				}
				return null;
			}
		}).when(mockChannel).queueDeclare(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean(),
				Mockito.anyMap());

		CachingConnectionFactory connectionFactory = new CachingConnectionFactory(mockConnectionFactory);
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.getBeanFactory().registerSingleton("foo", new Queue("foo"));
		applicationContext.getBeanFactory().registerSingleton("bar", new Queue("bar"));
		RabbitAdmin admin = new RabbitAdmin(connectionFactory);
		admin.setApplicationContext(applicationContext);
		admin.setAutoStartup(true);
		admin.afterPropertiesSet();
		doAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return count.get() > 1;
			}
		}).when(mockChannel).isOpen();
		connectionFactory.createConnection();
		// Expect 1 failure and 2 successful declarations
		Mockito.verify(mockChannel, times(3)).queueDeclare(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean(),
				Mockito.anyMap());
	}
}
