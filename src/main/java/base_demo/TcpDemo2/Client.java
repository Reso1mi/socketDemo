package base_demo.TcpDemo2;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

/**
 * @author imlgw.top
 * @date 2019/7/7 9:45
 */
@SuppressWarnings("all")
public class Client {
    private static final int REMOTE_PORT = 20000;

    private static final int LOCAL_PORT = 30000;

    public static void main(String[] args) throws IOException {
        Socket socket = creatSocket();
        initSocket(socket);
        //初始化后
        socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), LOCAL_PORT));
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
        socket.setReuseAddress(false);
        //是否开启Nagle算法(默认开启) https://baike.baidu.com/item/Nagle%E7%AE%97%E6%B3%95
        socket.setTcpNoDelay(false);
        //长时间无数据相应的时候发送确认数据（心跳包）时间大约两个小时
        socket.setKeepAlive(true);
        //close关闭后的处理 直接返回
        socket.setSoLinger(true, 0);
        //设置紧急数据是否内敛
        socket.setOOBInline(true);
        //设置收发缓冲器大小默认时32K
        socket.setReceiveBufferSize(64 * 1024);
        socket.setSendBufferSize(64 * 1024);
        //设置性能参数的 优先级  短链接 延迟 带宽
        socket.setPerformancePreferences(1, 1, 1);
    }

    private static Socket creatSocket() throws IOException {
        Socket socket = new Socket();
        return socket;
    }


    private static void sendMsg(Socket socket) throws IOException {
        //拿到socket的输出流
        OutputStream socketOutputStream = socket.getOutputStream();
        //socket的输入流
        InputStream socketInputStream = socket.getInputStream();
        //客户端发送消息 (利用工具类)
        /*byte []ints=ByteTools.int2byte(1242322);
        socketOutputStream.write(ints);*/
        //为什么不用String?
        //int 永远是32位4个字节 但是String如果int比较大就不只4位消耗较大
        byte[] buffer=new byte[256];
        //包装buffer (装饰器模式？
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        //byte  1
        byteBuffer.put((byte) 126);
        //int 类型 4
        byteBuffer.putInt(123);
        //char 2(unicode)
        byteBuffer.putChar('A');
        //long 8
        byteBuffer.putLong(323333231234124321L);
        boolean isOk=true;
        //byte 1
        byteBuffer.put((byte) (isOk?1:0));
        //float 4
        byteBuffer.putFloat(123.2132F);
        //double 8 =28
        byteBuffer.putDouble(231.1412421321);
        //String 10
        byteBuffer.put("HelloWorld".getBytes());
        //发送
        socketOutputStream.write(buffer,0,byteBuffer.position());
        //服务端的响应
        int readCount = socketInputStream.read(buffer);
        if (readCount>0) {
            System.out.println("接收到server响应的数据长度:"+readCount);
            //同server
            //System.out.println("server响应数据:"+new String(buffer,0,readCount));
            //System.out.println("server响应数据:"+ Array.getByte(buffer,0));
        } else {
            System.out.println("没有收到服务端响应：" + readCount);
        }
        socketOutputStream.close();
        socketInputStream.close();
    }
}