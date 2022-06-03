package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import lombok.extern.slf4j.Slf4j;



@Slf4j
public class NettyServer {

    public NettyServer(){

        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {

            ServerBootstrap server = new ServerBootstrap();
            server.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {


                            socketChannel.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                                            new ObjectEncoder(),
                                                            new ClientConnectionHandler());
                            log.debug("Connection is initialized.");
                        }
                    });

            ChannelFuture future = server.bind(ServerSettings.SERVER_PORT).sync();
            log.debug("Server is ready.");
            future.channel().closeFuture().sync();

        } catch (Exception e) {
            log.error("Exception: " + e.getMessage());
        } finally {
            auth.shutdownGracefully();
            worker.shutdownGracefully();
        }

    }


}
