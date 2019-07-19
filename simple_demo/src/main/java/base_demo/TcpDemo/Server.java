package base_demo.TcpDemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;

/**
 * @author imlgw.top
 * @date 2019/7/7 9:46
 */
public class Server {

    private static final int SERVER_PORT = 20000;

    public static void main(String[] args) throws IOException {
        ServerSocket server = creatServerSocket();
        initServerSocket(server);
        //初始化之后再绑定，不然一些设置会失效，比如 setReuseAddress
        server.bind(new InetSocketAddress(InetAddress.getLocalHost(), SERVER_PORT), 50);
        System.out.println("服务器准备就绪");
        System.out.println("服务端信息" + server.getInetAddress() + " port:" + server.getLocalPort());
        //监听客户端的消息
        while (true) {
            //阻塞方法
            Socket client = server.accept();
            ClientHandle clientHandle = new ClientHandle(client);
            new Thread(clientHandle).start();
        }
    }

    private static void initServerSocket(ServerSocket server) throws SocketException {
        //同client
        server.setReuseAddress(true);
        //设置accept的buffer
        server.setReceiveBufferSize(64 * 1024);
        //设置timeout
        //server.setSoTimeout(2000);
        //设置性能参数,连接前设置
        server.setPerformancePreferences(1, 1, 1);
    }

    private static ServerSocket creatServerSocket() throws IOException {
        ServerSocket server = new ServerSocket();
        //绑定端口 backlog:新连接队列的长度限制,不是链接的数量,是允许等待的队列长度
        //server.bind(new InetSocketAddress(InetAddress.getLocalHost(),SERVER_PORT),50);
        //server =new ServerSocket(SERVER_PORT,50); 等效方案
        //server =new ServerSocket(SERVER_PORT,50,InetAddress.getLocalHost());
        return server;
    }

    private static class ClientHandle implements Runnable {
        private Socket socket;

        ClientHandle(Socket client) {
            this.socket = client;
        }

        //接收消息
        public void run() {
            System.out.println("新客户端连接：" + socket.getInetAddress() + "port：" + socket.getPort());
            try {
                //输入流获取信息
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //输出流响应客户端
                PrintStream printStream = new PrintStream(socket.getOutputStream());
                boolean flag = true;
                do {
                    String s = reader.readLine();
                    if ("bye".equalsIgnoreCase(s)) {
                        flag = false;
                        System.out.println("客户端关闭了连接");
                        printStream.println("bye");
                    } else {
                        System.out.println(s);
                        printStream.println("字符串长度#" + s.length());
                    }
                } while (flag);
                reader.close();
                printStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}