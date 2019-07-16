package udp_tcp_demo.client;

import udp_tcp_demo.client.bean.ServerInfo;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * @author imlgw.top
 * @date 2019/7/15 22:10
 */
public class TCPClient {
    public static void linkStart(ServerInfo serverInfo) throws IOException {
        Socket socket = creatSocket();
        initSocket(socket);
        //这里因为有虚拟机的网卡 直接getByName获取不到 serverInfo.getAddress拿到的是虚拟机的网卡地址
        //socket.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getAddress()), serverInfo.getPort()), 3000);
        socket.connect(new InetSocketAddress(Inet4Address.getLocalHost(),serverInfo.getPort()));
        try {
            //这里try-cache是为了保证在服务端掉线或者其他情况也能正常关闭
            //发送和接收数据
            sendMsg(socket);
        } catch (Exception e) {
            System.out.println("异常关闭");
        } finally {
            socket.close();
            System.out.println("客户端已经退出");
        }
    }


    private static void initSocket(Socket socket) throws SocketException {
        socket.setSoTimeout(3000);
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

    private static Socket creatSocket()  {
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


        // 得到Socket输入流，并转换为BufferedReader 接收服务端的长度的回送
        InputStream inputStream = socket.getInputStream();
        BufferedReader socketBufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        boolean flag = true;
        do {
            // 键盘读取一行
            String str = input.readLine();
            // 发送到服务器
            socketPrintStream.println(str);
            // 从服务器读取一行,阻塞方法
            String echo = socketBufferedReader.readLine();
            if ("bye".equalsIgnoreCase(echo)) {
                flag = false;
            } else {
                System.out.println(echo);
            }
        } while (flag);
        // 资源释放
        socketPrintStream.close();
        socketBufferedReader.close();
    }
}
