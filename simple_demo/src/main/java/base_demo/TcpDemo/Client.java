package base_demo.TcpDemo;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

/**
 * @author imlgw.top
 * @date 2019/7/7 9:45
 */
public class Client {
    private static final int REMOTE_PORT = 20000;

    private static final int LOCAL_PORT = 30000;

    public static void main(String[] args) throws IOException {
        Socket socket = creatSocket();
        initSocket(socket);
        //连接远程server
        socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), REMOTE_PORT), 3000);
        System.out.println("客户端已经发起连接");
        System.out.println("客户端信息:" + socket.getLocalAddress() + "port:" + socket.getLocalPort());
        System.out.println("服务端信息" + socket.getInetAddress() + "port:" + socket.getPort());
        try {
            sendMsg(socket);
        } catch (Exception e) {
            System.err.println("连接异常关闭！！！！");
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    private static void initSocket(Socket socket) throws SocketException {
        socket.setSoTimeout(3000);
        //是否复用未完全关闭后的端口(TIME_WAIT状态)，必须在bind前，所以就不能通过构造器来绑定本地端口
        socket.setReuseAddress(true);
        //是否开启Nagle算法(默认开启) https://baike.baidu.com/item/Nagle%E7%AE%97%E6%B3%95
        socket.setTcpNoDelay(false);
        //长时间无数据相应的时候发送确认数据（心跳包）时间大约两个小时
        socket.setKeepAlive(true);
        //close关闭后的处理
        /*这个Socket选项可以影响close方法的行为。
          false 0 默认情况 关闭后立即返回，底层系统接管输出流，将缓冲区的数据发送完成
          true 0 关闭后直接返回 缓冲区数据直接抛弃 直接发送RES结束命令到对方，无需经过2MSL等待
          true 200 关闭时最长阻塞200s 随后按第二情况处理
        */
        socket.setSoLinger(true, 1);
        //设置紧急数据是否内敛
        /*    如果这个Socket选项打开，
        可以通过Socket类的sendUrgentData方法
        向服务器发送一个单字节的数据
        。这个单字节数据并不经过输出缓冲区，而是立即发出。
        虽然在客户端并不是使用OutputStream向服务器发送数据，
        但在服务端程序中这个单字节的数据是和其它的普通数据混在一起的
        因此，在服务端程序中并不知道由客户
        端发过来的数据是由OutputStream
        还是由sendUrgentData发过来的*/
        socket.setOOBInline(true);
        //设置收发缓冲器大小默认时32K
        socket.setReceiveBufferSize(64*1024);
        socket.setSendBufferSize(64*1024);
        //设置性能参数的 优先级  短链接 延迟 带宽
        socket.setPerformancePreferences(1,1,1);
    }
    @SuppressWarnings("all")
    private static Socket creatSocket() throws IOException {
        /*
        //无代理模式, 相当于空构造函数
        Socket socket = new Socket(Proxy.NO_PROXY);
        //HTTP代理模式传输的数据将通过www.imlgw.top转发
        socket = new Socket(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Inet4Address.getByName("www.imlgw.top"), 80)));

        //下面两种方式回在创建的时候就去链接远程的服务器(具体看源码),然而一般情况下其实在连接之前我们还需要设置一些参数
        //新建一个套接字 链接到远程服务器和端口（本地端口为系统分配）
        socket = new Socket("imlgw.top", REMOTE_PORT);
        //新建套接字直接链接到远程端口 并绑定本地端口
        socket=new Socket("imlgw.top",REMOTE_PORT,InetAddress.getLocalHost(),LOCAL_PORT);
        */

        //新建socket然后绑定到本地端口
        Socket socket = new Socket();
        socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), LOCAL_PORT));
        return socket;
    }


    private static void sendMsg(Socket socket) throws IOException {
        //键盘的输入流
        Scanner scanner = new Scanner(System.in);
        //拿到socket的输出流
        OutputStream socketOutputStream = socket.getOutputStream();
        //转换为打印流
        PrintStream printStream = new PrintStream(socketOutputStream);

        //socket的输入流
        InputStream inputStream = socket.getInputStream();
        //转换位buffer流
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        boolean flag = true;
        do {
            //客户端发送消息
            printStream.println(scanner.nextLine());
            //服务端的响应
            String s = bufferedReader.readLine();
            if ("bye".equals(s)) {
                flag = false;
            } else {
                System.out.println("服务端响应：" + s);
            }
        } while (flag);
        bufferedReader.close();
        printStream.close();
        scanner.close();
    }
}