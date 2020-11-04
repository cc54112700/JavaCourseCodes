package io.github.kimmking.gateway.outbound.netty4.server;


public class NettyServerApplication {

    public static void main(String[] args) {
        HttpServer server = new HttpServer(false,8288);
        try {
            server.run();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
