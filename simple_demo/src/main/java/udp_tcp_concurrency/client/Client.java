package udp_tcp_concurrency.client;

import udp_tcp_concurrency.client.bean.ServerInfo;

import java.io.IOException;

/**
 * @author imlgw.top
 * @date 2019/7/14 19:59
 * 搜索服务器端
 */
public class Client {
    public static void main(String[] args) {
        //UDP广播搜索TCPServer
        ServerInfo serverInfo = ClientSearcher.searchServer(10000);
        System.out.println("Server:" + serverInfo);
        //开启TCP连接客户端
        if (serverInfo != null) {
            try {
                TCPClient.linkStart(serverInfo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
