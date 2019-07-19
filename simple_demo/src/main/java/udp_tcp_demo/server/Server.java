package udp_tcp_demo.server;

import udp_tcp_demo.constants.TCPConstants;

import java.io.IOException;

/**
 * @author imlgw.top
 * @date 2019/7/14 18:23
 */
public class Server {

    /**这里TCP和UDP的代码格式有点不统一,不太好
     * @param args
     */
    public static void main(String[] args) {
        //启动TCP的服务
        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        boolean isSucessed = tcpServer.startTCPServer();
        if(!isSucessed){
            System.out.println("TCP Server start fail");
        }
        //启动UDP接收广播
        ServerProvider.startUdpServer(TCPConstants.PORT_SERVER);
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ServerProvider.stopUdpServer();
        tcpServer.stopTcpServer();
    }
}
