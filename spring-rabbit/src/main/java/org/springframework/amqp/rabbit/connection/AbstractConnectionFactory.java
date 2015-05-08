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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.rabbit.support.RabbitExceptionTranslator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.rabbitmq.client.Address;

/**
 * @author Dave Syer
 * @author Gary Russell
 *
 */
public abstract class AbstractConnectionFactory implements ConnectionFactory, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private final com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory;

	private final CompositeConnectionListener connectionListener = new CompositeConnectionListener();

	private final CompositeChannelListener channelListener = new CompositeChannelListener();

	private volatile ExecutorService executorService;

	private volatile Address[] addresses;

	public static final int DEFAULT_CLOSE_TIMEOUT = 30000;

	private volatile int closeTimeout = DEFAULT_CLOSE_TIMEOUT;

	/**
	 * Create a new SingleConnectionFactory for the given target ConnectionFactory.
	 * @param rabbitConnectionFactory the target ConnectionFactory
	 */
	public AbstractConnectionFactory(com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory) {
		Assert.notNull(rabbitConnectionFactory, "Target ConnectionFactory must not be null");
		this.rabbitConnectionFactory = rabbitConnectionFactory;
	}

	public void setUsername(String username) {
		this.rabbitConnectionFactory.setUsername(username);
	}

	public void setPassword(String password) {
		this.rabbitConnectionFactory.setPassword(password);
	}

	public void setHost(String host) {
		this.rabbitConnectionFactory.setHost(host);
	}

	@Override
	public String getHost() {
		return this.rabbitConnectionFactory.getHost();
	}

	public void setVirtualHost(String virtualHost) {
		this.rabbitConnectionFactory.setVirtualHost(virtualHost);
	}

	@Override
	public String getVirtualHost() {
		return rabbitConnectionFactory.getVirtualHost();
	}

	public void setPort(int port) {
		this.rabbitConnectionFactory.setPort(port);
	}

	public void setRequestedHeartBeat(int requestedHeartBeat) {
		this.rabbitConnectionFactory.setRequestedHeartbeat(requestedHeartBeat);
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.rabbitConnectionFactory.setConnectionTimeout(connectionTimeout);
	}

	@Override
	public int getPort() {
		return this.rabbitConnectionFactory.getPort();
	}

	/**
	 * Set addresses for clustering.
	 * This property overrides the host+port properties if not empty.
	 * @param addresses list of addresses with form "host[:port],..."
	 */
	public void setAddresses(String addresses) {
		if (StringUtils.hasText(addresses)) {
			Address[] addressArray = Address.parseAddresses(addresses);
			if (addressArray.length > 0) {
				this.addresses = addressArray;
				return;
			}
		}
		logger.info("setAddresses() called with an empty value, will be using the host+port properties for connections");
		this.addresses = null;
	}

	protected Address[] getAddresses() {
		return addresses;
	}

	/**
	 * A composite connection listener to be used by subclasses when creating and closing connections.
	 *
	 * @return the connection listener
	 */
	protected ConnectionListener getConnectionListener() {
		return connectionListener;
	}

	/**
	 * A composite channel listener to be used by subclasses when creating and closing channels.
	 *
	 * @return the channel listener
	 */
	protected ChannelListener getChannelListener() {
		return channelListener;
	}

	public void setConnectionListeners(List<? extends ConnectionListener> listeners) {
		this.connectionListener.setDelegates(listeners);
	}

	@Override
	public void addConnectionListener(ConnectionListener listener) {
		this.connectionListener.addDelegate(listener);
	}

	@Override
	public boolean removeConnectionListener(ConnectionListener listener) {
		return this.connectionListener.removeDelegate(listener);
	}

	@Override
	public void clearConnectionListeners() {
		this.connectionListener.clearDelegates();
	}

	public void setChannelListeners(List<? extends ChannelListener> listeners) {
		this.channelListener.setDelegates(listeners);
	}

	public void addChannelListener(ChannelListener listener) {
		this.channelListener.addDelegate(listener);
	}

	/**
	 * Provide an Executor for
	 * use by the Rabbit ConnectionFactory when creating connections.
	 * Can either be an ExecutorService or a Spring
	 * ThreadPoolTaskExecutor, as defined by a &lt;task:executor/&gt; element.
	 * @param executor The executor.
	 */
	public void setExecutor(Executor executor) {
		boolean isExecutorService = executor instanceof ExecutorService;
		boolean isThreadPoolTaskExecutor = executor instanceof ThreadPoolTaskExecutor;
		Assert.isTrue(isExecutorService || isThreadPoolTaskExecutor);
		if (isExecutorService) {
			this.executorService = (ExecutorService) executor;
		}
		else {
			this.executorService = ((ThreadPoolTaskExecutor) executor).getThreadPoolExecutor();
		}
	}

	protected synchronized ExecutorService getExecutorService() {
		return executorService;
	}

	/**
	 * How long to wait (milliseconds) for a response to a connection close
	 * operation from the broker; default 30000 (30 seconds).
	 *
	 * @param closeTimeout the closeTimeout to set.
	 */
	public void setCloseTimeout(int closeTimeout) {
		this.closeTimeout = closeTimeout;
	}

	public int getCloseTimeout() {
		return closeTimeout;
	}

	protected final Connection createBareConnection() {
		try {
			return createBareConnection(false);
		}
		catch (IOException e) {
			throw RabbitExceptionTranslator.convertRabbitAccessException(e);
		}
	}

	protected Connection createBareConnection(boolean primaryOnly) throws IOException {
		Connection connection = null;
		if (this.addresses != null) {
			IOException lastException = null;
			for (Address address : this.addresses) {
				try {
					connection = new SimpleConnection(this.rabbitConnectionFactory.newConnection(this.executorService, this.addresses),
									this.closeTimeout, address);
				}
				catch (IOException e) {
					lastException = e;
				}
				if (primaryOnly) {
					break;
				}
			}
			if (connection == null) {
				throw new IOException("Cannot connect to any of " + Arrays.asList(this.addresses), lastException);
			}
		}
		else {
			connection = new SimpleConnection(this.rabbitConnectionFactory.newConnection(this.executorService),
								this.closeTimeout);
		}
		if (logger.isInfoEnabled()) {
			logger.info("Created new connection: " + connection);
		}
		return connection;
	}

	protected final  String getDefaultHostName() {
		String temp;
		try {
			InetAddress localMachine = InetAddress.getLocalHost();
			temp = localMachine.getHostName();
			logger.debug("Using hostname [" + temp + "] for hostname.");
		} catch (UnknownHostException e) {
			logger.warn("Could not get host name, using 'localhost' as default value", e);
			temp = "localhost";
		}
		return temp;
	}

	@Override
	public void destroy() {
	}
}
