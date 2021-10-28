package socket;
  
import java.util.concurrent.TimeUnit;

import enums.ConfigEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rfid.ConfigMsg;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import utils.PropertiesUtil;

/**
 * 主入口
 * @author 52456
 *
 */
public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	public static void main(String[] args) throws InterruptedException {
        logger.info("启动服务");
		initclient();
	}
	/**
	 * 注册设备
	 * @throws InterruptedException
	 */
	public static void initclient() throws InterruptedException {
		String ip=PropertiesUtil.getUrlValue(ConfigEnum.SOCKET_IP.getCode());
		int port=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigEnum.SOCLET_PORT.getCode()));
        logger.info("开始注册设备连接服务端");
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ByteBuf delimiter = Unpooled.copiedBuffer(ConfigEnum.DELIMITER.getCode().getBytes());
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024*4, delimiter));
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new NettyClientHandler());
                            ch.pipeline().addLast("ping", new IdleStateHandler(60, 20, 60 * 10, TimeUnit.SECONDS));
                        }
                    });
            ChannelFuture f = b.connect(ip, port).sync(); 
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error(String.format("连接服务端失败：%s", e.getMessage()));
            int reconnect=Integer.parseInt(PropertiesUtil.getUrlValue(ConfigEnum.RECONNECT_TIME.getCode()));
            Thread.sleep(reconnect);
            initclient();
		} finally {
            group.shutdownGracefully();
        }
	}
}
