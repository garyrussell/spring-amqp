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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.amqp.protonj.listener.ProtonMessageListenerContainer;

/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class ProtonTemplateTests {

	@Test
	public void testSend() throws Exception {
		ProtonMessageListenerContainer container = new ProtonMessageListenerContainer();
		final CountDownLatch latch = new CountDownLatch(2);
		final List<String> received = new ArrayList<>();
		container.setConsumer(a -> {
			received.add(a);
			latch.countDown();
		});
		container.start();
		ProtonTemplate template = new ProtonTemplate("amqp://localhost/foo");
		template.start();
		template.send("foo", "bar");
		template.send("foo", "baz");
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertThat(received).contains("bar", "baz");
		template.stop();
		container.stop();
	}

}
