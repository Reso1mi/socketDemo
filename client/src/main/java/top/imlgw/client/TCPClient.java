package top.imlgw.client;



import top.imlgw.client.bean.ServerInfo;
import top.imlgw.common.utils.CloseUtils;

import java.io.*;
import java.net.*;

/**
 * @author imlgw.top
 * @date 2019/7/15 22:10
 */
public class TCPClient {
    public static void linkStart(ServerInfo serverInfo) throws IOException {
        Socket socket = creatSocket();
        initSocket(socket);
        //这里直接getByName获取不到,很迷,这个ip前有一个/ 需要去掉。。。
        socket.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getAddress().substring(1)), serverInfo.getPort()), 3000);
        //socket.connect(new InetSocketAddress(Inet4Address.getLocalHost(), serverInfo.getPort()));
        try {
            ClientReadHandler readHandler =new ClientReadHandler(socket.getInputStream());
            //启动读取服务端线程的Handle
            new Thread(readHandler).start();
            //这里try-cache是为了保证在服务端掉线或者其他情况也能正常关闭
            //发送数据
            sendMsg(socket);
            //handle stop
            readHandler.stopRead();
        } catch (Exception e) {
            System.out.println("异常关闭");
        } finally {
            socket.close();
            System.out.println("客户端已经退出");
        }
    }


    private static void initSocket(Socket socket) throws SocketException {
        //socket.setSoTimeout(3000); 这里不要加超时时间，服务端也会想客户端发送信息
        //是否复用未完全关闭后的端口(TIME_WAIT状态)，必须在bind前，所以就不能通过构造器来绑定本地端口
        socket.setReuseAddress(false);
        //是否开启Nagle算法(默认开启) https://baike.baidu.com/item/Nagle%E7%AE%97%E6%B3%95
        socket.setTcpNoDelay(false);
        //长时间无数据相应的时候发送确认数据（心跳包）时间大约两个小时
        socket.setKeepAlive(true);
        //设置紧急数据是否内敛
        socket.setOOBInline(true);
        //close关闭后的处理 直接返回
        socket.setSoLinger(true, 0);
        //设置收发缓冲器大小默认时32K
        socket.setReceiveBufferSize(64 * 1024);
        socket.setSendBufferSize(64 * 1024);
        //设置性能参数的 优先级  短链接 延迟 带宽
        socket.setPerformancePreferences(1, 1, 1);
    }

    private static Socket creatSocket() {
        Socket socket = new Socket();
        return socket;
    }

    private static void sendMsg(Socket socket) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        // 得到Socket输出流，并转换为打印流，向服务端发送消息
        OutputStream outputStream = socket.getOutputStream();
        PrintStream socketPrintStream = new PrintStream(outputStream);

        do {
            // 键盘读取一行
            String str = input.readLine();
            // 发送到服务器
            socketPrintStream.println(str);
            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }
        } while (true);
        // 资源释放
        socketPrintStream.close();
    }

    static class ClientReadHandler implements Runnable {
        private boolean done = false;
        private final InputStream inputStream;


        public ClientReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public void run() {
            try {
                //输入流获取信息
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                do{
                    String str;
                    try {
                        str = reader.readLine();
                    } catch (SocketTimeoutException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (str == null) {
                        System.out.println("客户端已经无法读取数据");
                        break;
                    }
                    //打印到屏幕
                    System.out.println(str);
                }while (!done);
            } catch (IOException e) {
                //是否正常关闭
                if (!done) {
                    System.out.println("连接异常断开 "+e.getMessage());
                }
            } finally {
                CloseUtils.close(inputStream);
            }
        }

        public void stopRead() {
            done = true;
            CloseUtils.close(inputStream);
        }
    }
}
