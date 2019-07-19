package base_demo.TcpDemo2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * @author imlgw.top
 * @date 2019/7/7 9:46
 */
@SuppressWarnings("all")
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
                //输入流用来获取信息
                InputStream socketInputStream = socket.getInputStream();
                //输出流用来响应客户端
                OutputStream socketOutputStream = socket.getOutputStream();
                byte[] buffer = new byte[256];
                //这个方法返回的是读取到的字节数,另一个空参数的返回的是当前的那个字节0~255, -1代表结束
                int readByteCount = socketInputStream.read(buffer);
                //readByteCount为用到ByteBuffer的长度,应当小于 buffer.length-offset
                ByteBuffer byteBuffer=ByteBuffer.wrap(buffer,0,readByteCount);
                //byte
                byte b = byteBuffer.get();
                //int
                int anInt = byteBuffer.getInt();
                char aChar = byteBuffer.getChar();
                long aLong = byteBuffer.getLong();
                boolean bool= byteBuffer.get()==1;
                float aFloat = byteBuffer.getFloat();
                double aDouble = byteBuffer.getDouble();
                //最后的String
                System.out.println("当前下标"+byteBuffer.position());
                String str = new String(buffer, byteBuffer.position(), readByteCount-byteBuffer.position());

                if (readByteCount> 0) {
                    System.out.println("接受到Client数据长度(byte)：" + readByteCount);
                    //这里转换为String的时候转换失败了,具体可以debug
                    //System.out.println("Client发送的数据:" + new String(buffer, 0, readCount));
                    System.out.println("Client发送的数据:\n" + b+"\n"+anInt+"\n"+aChar+"\n"+aLong+"\n"+bool+"\n"+aFloat+"\n"+aDouble+"\n"+str);
                    //回送给客户端
                    socketOutputStream.write(buffer,0,readByteCount);
                } else {
                    System.out.println("未接受到Client的数据");
                    socketOutputStream.write(new byte[]{0});
                }
                socketInputStream.close();
                socketOutputStream.close();
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