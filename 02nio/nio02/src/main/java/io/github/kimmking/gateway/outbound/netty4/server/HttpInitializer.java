package io.github.kimmking.gateway.outbound.netty4.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;

public class HttpInitializer extends ChannelInitializer<SocketChannel> {
	private final SslContext sslCtx;

	public HttpInitializer(SslContext sslCtx) {
		this.sslCtx = sslCtx;
	}

	@Override
	public void initChannel(SocketChannel ch) {
		ChannelPipeline p = ch.pipeline();
//		if (sslCtx != null) {
//			p.addLast(sslCtx.newHandler(ch.alloc()));
//		}
//		p.addLast(new HttpServerCodec());
//		//p.addLast(new HttpServerExpectContinueHandler());
//		p.addLast(new HttpObjectAggregator(1024 * 1024));


		// 版本2 客户端用
		// server端发送的是httpResponse，所以要使用HttpResponseEncoder进行编码
		ch.pipeline().addLast(new HttpResponseEncoder());
		// server端接收到的是httpRequest，所以要使用HttpRequestDecoder进行解码
		ch.pipeline().addLast(new HttpRequestDecoder());

		p.addLast(new HttpHandler());
	}
}
