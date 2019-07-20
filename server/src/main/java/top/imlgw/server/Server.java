package top.imlgw.server;


import top.imlgw.common.constants.TCPConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author imlgw.top
 * @date 2019/7/14 18:23
 */
public class Server {

    /**
     * 这里TCP和UDP的代码格式有点不统一,不太好
     * @param args
     */
    public static void main(String[] args) throws IOException {
        //启动TCP的服务
        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        boolean isSucessed = tcpServer.startTCPServer();
        if (!isSucessed) {
            System.out.println("TCP Server start fail");
        }
        //启动UDP接收广播
        ServerProvider.startUdpServer(TCPConstants.PORT_SERVER);
        //发送接收并行,其实也没有并行,只是被阻塞了
        BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            //键盘的输入
            str=reader.readLine();
            //发送数据给所有客户端
            tcpServer.boardCast(str);
        } while (!"00bye00".equalsIgnoreCase(str));
        ServerProvider.stopUdpServer();
        tcpServer.stopTcpServer();
    }
}
