package udp_tcp_concurrency.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;

/**
 * @author imlgw.top
 * @date 2019/7/15 21:01
 */
public class TCPServer {

    private final int tcpServerPort;

    private static ClientListen CLIENT_LISTEN;

    private static ClientHandle CLIENT_HANDLE;

    public TCPServer(int port) {
        this.tcpServerPort = port;
    }

    public boolean startTCPServer() {
        try {
            ClientListen clientListen = new ClientListen(tcpServerPort);
            new Thread(clientListen).start();
            CLIENT_LISTEN = clientListen;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void stopTcpServer() {
        if (CLIENT_LISTEN != null) {
            CLIENT_LISTEN.stopListen();
            //这里好像也没有停的必要,客户端关闭后readLine就会抛异常就会正常退出
            //实在要停可以加一个超时时间，关闭两个流就ok
            //CLIENT_HANDLE.stopHandle();
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

    //异步监听客户端的连接
    private static class ClientListen implements Runnable {
        private ServerSocket server;
        private boolean done = false;

        public ClientListen(int port) throws IOException {
            server = creatServerSocket();
            initServerSocket(server);
            //绑定本地端口
            server.bind(new InetSocketAddress(InetAddress.getLocalHost(), port), 50);
            System.out.println("服务端信息" + server.getInetAddress() + " port:" + server.getLocalPort());
        }

        public void run() {
            System.out.println("服务器准备就绪");
            //监听客户端的消息
            do {
                Socket client=null;
                ClientHandle clientHandle=null;
                try {
                    //阻塞方法,等待获取连接过来的 client
                    client = server.accept();
                    //交给异步线程去处理
                    clientHandle = new ClientHandle(client);
                    CLIENT_HANDLE=clientHandle;
                    new Thread(clientHandle).start();
                } catch (IOException e) {
                    System.out.println("事实证明 server.accept的时候close是会报异常的 ");
                    e.printStackTrace();
                    continue;
                }
            } while (!done);
            System.out.println("服务端已经关闭");
        }

        public void stopListen() {
            //这里是一定会停止线程的
            done = true;
            try {
                //这个close会使accept报异常然后就退出了
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    //异步处理每个客户端的线程
    private static class ClientHandle implements Runnable {
        private Socket socket;
        private boolean done = false;
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
                do {
                    //阻塞方法
                    System.out.println("print");
                    printStream.print(123);
                    System.out.println("readline");
                    String s = reader.readLine();
                    if ("bye".equalsIgnoreCase(s)) {
                        done = false;
                        System.out.println("客户端关闭了连接");
                        printStream.println("bye");
                    } else {
                        System.out.println(s);
                        printStream.println("长度:" + s.length());
                    }
                } while (!done);
                //关闭流
                reader.close();
                printStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void stopHandle(){
            //done变为true其实也不能停止线程,那个readLine也是阻塞的方法
            //需要把reader也close掉就可以退出了
            done = true;
        }
    }
}
