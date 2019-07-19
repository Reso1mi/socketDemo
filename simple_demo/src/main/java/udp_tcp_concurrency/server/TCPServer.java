package udp_tcp_concurrency.server;

import udp_tcp_concurrency.server.handle.ClientHandler;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author imlgw.top
 * @date 2019/7/15 21:01
 */
public class TCPServer {

    private final int tcpServerPort;

    private static ClientListen CLIENT_LISTEN;

    private List<ClientHandler> clientHandles = new ArrayList<>();

    public TCPServer(int port) {
        this.tcpServerPort = port;
    }


    /**
     * 启动TCPServer
     * @return
     */
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

    /**
     * 停止TcpServer
     */
    public void stopTcpServer() {
        if (CLIENT_LISTEN != null) {
            CLIENT_LISTEN.stopListen();
        }

        for (ClientHandler clientHandle : clientHandles) {
            clientHandle.stop();
        }
        clientHandles.clear();
    }

    /**
     * 初始化ServerSocket
     * @param server
     * @throws SocketException
     */
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

    /**
     * @return 创建ServerSocket
     * @throws IOException
     */
    private static ServerSocket creatServerSocket() throws IOException {
        ServerSocket server = new ServerSocket();
        //绑定端口 backlog:新连接队列的长度限制,不是链接的数量,是允许等待的队列长度
        //server.bind(new InetSocketAddress(InetAddress.getLocalHost(),SERVER_PORT),50);
        //server =new ServerSocket(SERVER_PORT,50); 等效方案
        //server =new ServerSocket(SERVER_PORT,50,InetAddress.getLocalHost());
        return server;
    }

    /**给所有的客户端发送消息
     * @param str
     */
    public void boardCast(String str) {
        for (ClientHandler clientHandle : clientHandles) {
            clientHandle.send(str);
        }
    }

    /**
     * 监听客户端的连接，并且交由异步线程去处理客户端
     */
    private class ClientListen implements Runnable {
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
                Socket client = null;
                try {
                    //阻塞方法,等待获取连接过来的 client
                    client = server.accept();
                } catch (IOException e) {
                    if(!done){
                        System.err.println("accept异常:"+e.getMessage());
                    }
                    continue;
                }
                //交给异步线程去处理
                try {
                    //构建异步线程处理客户端的请求,同时实现CloseNotify接口在自闭的时候移除handle
                    //这里好像是《观察者模式》
                    ClientHandler   clientHandle = new ClientHandler(client, (handler) -> clientHandles.remove(handler));
                    //读取数据并且打印
                    clientHandle.read2Print();
                    //别忘了将handle加到list中不然无法广播
                    clientHandles.add(clientHandle);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("客户端连接异常"+e.getMessage());
                }
            } while (!done);
            System.out.println("服务端已经关闭");
        }

        public void stopListen() {
            //这里是一定会停止监听线程的
            done = true;
            try {
                //这个close会使accept报异常然后退出
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
