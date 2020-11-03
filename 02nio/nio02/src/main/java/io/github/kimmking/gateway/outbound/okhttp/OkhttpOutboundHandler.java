package io.github.kimmking.gateway.outbound.okhttp;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import okhttp3.*;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class OkhttpOutboundHandler {

    private String backendUrl;
    private OkHttpClient httpClient;

    private final static int READ_TIMEOUT = 20;

    private final static int CONNECT_TIMEOUT = 10;

    private final static int WRITE_TIMEOUT = 60;


    public OkhttpOutboundHandler(String backendUrl) {

        this.backendUrl = backendUrl.endsWith("/") ? backendUrl.substring(0, backendUrl.length() - 1) : backendUrl;
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    public void handle(FullHttpRequest fullRequest, ChannelHandlerContext ctx) {

        String url = this.backendUrl + fullRequest.uri();
        doOkHttpGet(fullRequest, ctx, url);
    }

    private void doOkHttpGet(FullHttpRequest inbound, ChannelHandlerContext ctx, String url) {


        httpClient.newCall(new Request.Builder().url(url).addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_KEEP_ALIVE).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                if (e.getCause().equals(SocketTimeoutException.class)) {

                    // 处理省略
                    // 比如可以加重试逻辑
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                handleResponse(inbound, ctx, response);
            }
        });
    }

    private void handleResponse(FullHttpRequest fullRequest, ChannelHandlerContext ctx, Response response) {

        FullHttpResponse fullHttpResponse = null;
        try {

            byte[] body = response.body().bytes();

            fullHttpResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(body));
            fullHttpResponse.headers().set("Content-Type", "application/json");
            fullHttpResponse.headers().setInt("Content-Length", Integer.parseInt(response.header("Content-Length")));

        } catch (Exception e) {
            e.printStackTrace();
            fullHttpResponse = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
            exceptionCaught(ctx, e);
        } finally {
            if (fullRequest != null) {
                if (!HttpUtil.isKeepAlive(fullRequest)) {
                    ctx.write(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
                } else {
                    //response.headers().set(CONNECTION, KEEP_ALIVE);
                    ctx.write(fullHttpResponse);
                }
            }
            ctx.flush();
            //ctx.close();
        }

    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
