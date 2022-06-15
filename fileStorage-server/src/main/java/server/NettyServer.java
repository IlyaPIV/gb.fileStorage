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
import server.hibernate.DBConnector;


@Slf4j
public class NettyServer {

    private final FilesStorage filesStorageService;

    private DBConnector databaseConnector;

    public NettyServer(){

        filesStorageService = new FilesStorage();

        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {

            try {
                this.databaseConnector = new DBConnector();
                log.debug("Hibernate connection is created.");
            } catch (Exception e) {
                log.error("Failed create hibernate connection with Database.");
                throw new Exception("Failed create hibernate connection with Database.");
            }

            log.debug("Server is ready.");

            ServerBootstrap server = new ServerBootstrap();
            server.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {


                            socketChannel.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                                            new ObjectEncoder(),
                                                            new ClientConnectionHandler(filesStorageService, databaseConnector));
                            log.debug("Connection is initialized.");
                        }
                    });

            ChannelFuture future = server.bind(ServerSettings.SERVER_PORT).sync();
            future.channel().closeFuture().sync();


        } catch (Exception e) {
            log.error("Exception: " + e.getMessage());
        } finally {
            log.debug("Shooting down event loop groups.");
            auth.shutdownGracefully();
            worker.shutdownGracefully();
        }

    }


}
