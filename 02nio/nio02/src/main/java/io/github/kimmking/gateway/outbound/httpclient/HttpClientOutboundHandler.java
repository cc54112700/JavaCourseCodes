package io.github.kimmking.gateway.outbound.httpclient;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpClientOutboundHandler {

    private CloseableHttpClient httpClient;
    private String backendUrl;

    //
    public HttpClientOutboundHandler(String backendUrl) {

        this.backendUrl = backendUrl.endsWith("/") ? backendUrl.substring(0, backendUrl.length() - 1) : backendUrl;
        httpClient = HttpClients.createDefault();
    }

    //
    public void handle(FullHttpRequest fullRequest, ChannelHandlerContext ctx) {

        final String url = this.backendUrl + fullRequest.uri();
        doHttpGet(fullRequest, ctx, url);
    }

    private void doHttpGet(FullHttpRequest inbound, ChannelHandlerContext ctx, String url) {

        HttpGet httpGet = new HttpGet(url);
        //httpGet.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
        httpGet.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_KEEP_ALIVE);

        try {

            //
//            handleEndResponse(inbound, ctx, httpClient.execute(httpGet));

            httpClient.execute(httpGet, new ResponseHandler<Object>() {
                @Override
                public Object handleResponse(HttpResponse endpointResponse) throws ClientProtocolException, IOException {

                    return handleEndResponse(inbound, ctx, endpointResponse);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Object handleEndResponse(FullHttpRequest fullRequest, ChannelHandlerContext ctx, HttpResponse endpointResponse) {

        FullHttpResponse response = null;
        try {

            byte[] body = EntityUtils.toByteArray(endpointResponse.getEntity());


//            body = EntityUtils.toString(endpointResponse.getEntity()).getBytes();

            response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(body));
            response.headers().set("Content-Type", "application/json");
            response.headers().setInt("Content-Length", Integer.parseInt(endpointResponse.getFirstHeader("Content-Length").getValue()));
        } catch (Exception e) {
            e.printStackTrace();
            response = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
            exceptionCaught(ctx, e);
        } finally {
            if (fullRequest != null) {
                if (!HttpUtil.isKeepAlive(fullRequest)) {
                    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                } else {
                    //response.headers().set(CONNECTION, KEEP_ALIVE);
                    ctx.write(response);
                }
            }
            ctx.flush();
            //ctx.close();
        }

        return null;
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
