/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.proxy.frontend;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.infra.config.props.ConfigurationPropertyKey;
import org.apache.shardingsphere.proxy.backend.communication.vertx.VertxBackendDataSource;
import org.apache.shardingsphere.proxy.backend.context.BackendExecutorContext;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.frontend.netty.ServerHandlerInitializer;
import org.apache.shardingsphere.proxy.frontend.protocol.FrontDatabaseProtocolTypeFactory;

/**
 * ShardingSphere-Proxy.
 */
@Slf4j
public final class ShardingSphereProxy {
    
    private EventLoopGroup bossGroup;
    
    private EventLoopGroup workerGroup;
    
    /**
     * Start ShardingSphere-Proxy.
     *
     * @param port port
     */
    @SneakyThrows(InterruptedException.class)
    public void start(final int port) {
        try {
            ChannelFuture future = startInternal(port);
            accept(future);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            BackendExecutorContext.getInstance().getExecutorEngine().close();
        }
    }
    
    private ChannelFuture startInternal(final int port) throws InterruptedException {
        createEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        initServerBootstrap(bootstrap);
        return bootstrap.bind(port).sync();
    }
    
    private void accept(final ChannelFuture future) throws InterruptedException {
        log.info("ShardingSphere-Proxy {} mode started successfully", ProxyContext.getInstance().getContextManager().getInstanceContext().getModeConfiguration().getType());
        future.channel().closeFuture().sync();
    }
    
    private void createEventLoopGroup() {
        bossGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(1) : new NioEventLoopGroup(1);
        workerGroup = getWorkerGroup();
    }
    
    private EventLoopGroup getWorkerGroup() {
        String driverType = ProxyContext.getInstance().getContextManager().getMetaDataContexts().getProps().getValue(ConfigurationPropertyKey.PROXY_BACKEND_DRIVER_TYPE);
        boolean reactiveBackendEnabled = "ExperimentalVertx".equalsIgnoreCase(driverType);
        if (reactiveBackendEnabled) {
            return VertxBackendDataSource.getInstance().getVertx().nettyEventLoopGroup();
        }
        int workerThreads = ProxyContext.getInstance().getContextManager().getMetaDataContexts().getProps().<Integer>getValue(ConfigurationPropertyKey.PROXY_FRONTEND_EXECUTOR_SIZE);
        return Epoll.isAvailable() ? new EpollEventLoopGroup(workerThreads) : new NioEventLoopGroup(workerThreads);
    }
    
    private void initServerBootstrap(final ServerBootstrap bootstrap) {
        Integer backLog = ProxyContext.getInstance().getContextManager().getMetaDataContexts().getProps().<Integer>getValue(ConfigurationPropertyKey.PROXY_NETTY_BACKLOG);
        bootstrap.group(bossGroup, workerGroup)
                .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024 * 1024, 16 * 1024 * 1024))
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, backLog)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ServerHandlerInitializer(FrontDatabaseProtocolTypeFactory.getDatabaseType()));
    }
}
