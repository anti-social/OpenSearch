/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.transport.nio;

import org.opensearch.common.settings.Settings;
import org.opensearch.nio.ChannelFactory;
import org.opensearch.nio.Config;
import org.opensearch.nio.NioGroup;
import org.opensearch.nio.NioSelector;
import org.opensearch.nio.NioServerSocketChannel;
import org.opensearch.nio.NioSocketChannel;
import org.opensearch.nio.ServerChannelContext;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

public class NioGroupFactoryTests extends OpenSearchTestCase {

    public void testSharedGroupStillWorksWhenOneInstanceClosed() throws IOException {
        NioGroupFactory groupFactory = new NioGroupFactory(Settings.EMPTY, logger);

        InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        NioGroup httpGroup = groupFactory.getHttpGroup();
        try {
            NioGroup transportGroup = groupFactory.getTransportGroup();
            transportGroup.close();
            expectThrows(IllegalStateException.class, () -> transportGroup.bindServerChannel(inetSocketAddress, new BindingFactory()));

            httpGroup.bindServerChannel(inetSocketAddress, new BindingFactory());
        } finally {
            httpGroup.close();
        }
        expectThrows(IllegalStateException.class, () -> httpGroup.bindServerChannel(inetSocketAddress, new BindingFactory()));
    }

    private static class BindingFactory extends ChannelFactory<NioServerSocketChannel, NioSocketChannel> {

        private BindingFactory() {
            super(false, false, -1, -1, -1, false, -1, -1);
        }

        @Override
        public NioSocketChannel createChannel(NioSelector selector, SocketChannel channel, Config.Socket socketConfig) throws IOException {
            throw new IOException("boom");
        }

        @Override
        public NioServerSocketChannel createServerChannel(NioSelector selector, ServerSocketChannel channel,
                                                          Config.ServerSocket socketConfig) {
            NioServerSocketChannel nioChannel = new NioServerSocketChannel(channel);
            Consumer<Exception> exceptionHandler = (e) -> {};
            Consumer<NioSocketChannel> acceptor = (c) -> {};
            ServerChannelContext context = new ServerChannelContext(nioChannel, this, selector, socketConfig, acceptor, exceptionHandler);
            nioChannel.setContext(context);
            return nioChannel;
        }
    }
}
