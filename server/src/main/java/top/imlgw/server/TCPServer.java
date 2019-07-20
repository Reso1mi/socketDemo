package top.imlgw.server;


import top.imlgw.common.utils.CloseUtils;
import top.imlgw.server.handle.ClientHandler;

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author imlgw.top
 * @date 2019/7/15 21:01
 */
public class TCPServer implements ClientHandler.ClientHandleCallBack {

    private final int tcpServerPort;

    private static ClientListen CLIENT_LISTEN;

    //用于转发客户端信息的线程池
    private final ExecutorService forwardThreadPool;

    private Selector selector;

    private ServerSocketChannel serverChannel;
    //底层好像就是把这个list本身的方法加了synchronized 但是遍历的时候不能保证线程安全
    //private List<ClientHandler> clientHandlerList = Collections.synchronizedList( new ArrayList<>());;

    //处理客户端的List 一个客户端一个handler
    private List<ClientHandler> clientHandles = new ArrayList<>();

    public TCPServer(int port) {
        this.tcpServerPort = port;
        //单例线程池
        forwardThreadPool = Executors.newSingleThreadExecutor();
    }


    /**
     * 启动TCPServer
     *
     * @return
     */
    public boolean startTCPServer() {
        try {
            selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            this.serverChannel = serverChannel;
            //设置为非阻塞
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(tcpServerPort));
            //注册客户端到达的监听事件
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务端信息" + serverChannel.getLocalAddress());
            //启动客户端监听
            ClientListen clientListen = CLIENT_LISTEN = new ClientListen();
            new Thread(clientListen).start();
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
        CloseUtils.close(serverChannel);
        CloseUtils.close(selector);
        //加锁
        synchronized (TCPServer.this) {
            for (ClientHandler clientHandle : clientHandles) {
                clientHandle.stop();
            }
            clientHandles.clear();
        }
        forwardThreadPool.shutdownNow();
    }

    /**
     * 初始化ServerSocket
     *
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

    /**
     * 给所有的客户端发送消息
     *
     * @param str
     */
    public synchronized void boardCast(String str) {
        for (ClientHandler clientHandle : clientHandles) {
            clientHandle.send(str);
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler clientHandler) {
        clientHandles.remove(clientHandler);
    }

    @Override
    public void onNewMessageArrived(ClientHandler clientHandler, String msg) {
        System.out.println("Receive from:" + clientHandler.getClientInfo() + " msg:" + msg);
        forwardThreadPool.submit(() -> {
            for (ClientHandler clientHandle : clientHandles) {
                //跳过自己
                if (clientHandle.equals(clientHandler)) {
                    continue;
                }
                //对其他客户端发送消息
                clientHandle.send(msg);
            }
        });
    }

    /**
     * 监听客户端的连接，并且交由异步线程去处理客户端
     */
    private class ClientListen implements Runnable {
        private boolean done = false;


        public void run() {
            //转换为局部变量
            Selector selector = TCPServer.this.selector;

            System.out.println("服务器准备就绪");
            //监听客户端的消息
            do {
                try {
                    if (selector.select() == 0) {
                        //被唤醒wakeUp的就是0
                        if (done) {
                            break;
                        }
                        continue;
                    }
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }
                        SelectionKey key = iterator.next();
                        //移除当前Key
                        iterator.remove();
                        //检查当前key的状态是不是我们关注的 客户端到达状态
                        if (key.isAcceptable()) {
                            //就是上面注册用的Channel
                            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                            //一定是非阻塞状态的
                            SocketChannel clientSocket = serverChannel.accept();
                            //交给异步线程去处理
                            try {
                                //构建异步线程处理客户端,同时实现CloseNotify接口在自闭的时候移除handle
                                //这里好像是《观察者模式》
                                ClientHandler clientHandle = new ClientHandler(clientSocket, TCPServer.this);
                                //读取数据并且打印
                                clientHandle.read2Print();
                                synchronized (TCPServer.this) {
                                    //别忘了将handle加到list中不然无法广播
                                    clientHandles.add(clientHandle);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("客户端连接异常" + e.getMessage());
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (!done);
            System.out.println("服务端已经关闭");
        }

        public void stopListen() {
            //这里是一定会停止监听线程的
            done = true;
            //唤醒当前的阻塞
            selector.wakeup();
        }

    }
}
