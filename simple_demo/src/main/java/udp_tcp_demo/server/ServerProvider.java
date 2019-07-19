package udp_tcp_demo.server;

import udp_tcp_demo.constants.ByteUtils;
import udp_tcp_demo.constants.UDPConstants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @author imlgw.top
 * @date 2019/7/14 18:27
 * 接收端
 */
public class ServerProvider {
    private static Provider PROVIDER_INSTANCE;

    static void startUdpServer(int port) {
        //确保上一次的服务关闭
        stopUdpServer();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn, port);
        new Thread(provider).start();
        PROVIDER_INSTANCE = provider;
    }

    static void stopUdpServer() {
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.shutdown();
            PROVIDER_INSTANCE = null;
        }
    }

    public static class Provider implements Runnable {
        private final byte[] sn;
        private final int port;
        private volatile boolean isDone = false;
        private DatagramSocket ds = null;
        private final int minLen =UDPConstants.HEADER.length+2+4;
        //存储消息的buffer
        final byte[] buffer = new byte[128];

        public Provider(String sn, int port) {
            this.sn = sn.getBytes();
            this.port = port;
        }

        public void run() {
            System.out.println("UDPProvide  Started....");
            try {
                //服务端接收UDP广播的端口 30201
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);
                //接受消息的Packet
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                while (!isDone) {
                    ds.receive(receivePacket);
                    //获取发送人的SocketAddress
                    InetSocketAddress socketAddress = (InetSocketAddress) receivePacket.getSocketAddress();
                    //获取发送过来的数据
                    int dataLen = receivePacket.getLength();
                    byte[] receiveData = receivePacket.getData();
                    //校验数据 1.长度>= 基本公共头+cmd指令(short 2个字节)+port端口(int 4个字节)
                    boolean isValid = dataLen >= minLen && ByteUtils.startWith(receiveData, UDPConstants.HEADER);
                    if (!isValid) {
                        continue;
                    }
                    //解析命令和客户端的回送端口
                    int index = UDPConstants.HEADER.length;
                    short cmd = (short) ((receiveData[index++] << 8) | (receiveData[index++] & 0xff));
                    int responsePort = ((receiveData[index++] & 0xff) << 24) | ((receiveData[index++] & 0xff) << 16) | ((receiveData[index++] & 0xff) << 8) | ((receiveData[index++] & 0xff));

                    //判断合法性
                    if (cmd == 1 && responsePort > 0) {
                        //构建一份回送数据
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPConstants.HEADER);
                        byteBuffer.putShort((short) 2);
                        //回送TCP的端口号
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);
                        int len = byteBuffer.position();
                        //直接根据发送者构建一份回送消息
                        //回送端口为客户端发送过来的
                        DatagramPacket respDatagramPacket = new DatagramPacket(buffer, len, socketAddress.getAddress(),responsePort);
                        ds.send(respDatagramPacket);
                        System.out.println("ServerProvider response to:" +socketAddress);
                    }else {
                        System.out.println("ServerProvider receive cmd nonsupport; cmd:"+cmd);
                    }
                }
            } catch (IOException e) {
                // e.printStackTrace();
            } finally {
                closeRes();
            }
            //结束
            System.out.println("UDPProvide Finished.");
        }

        public void shutdown() {
            isDone = true;
            //这里仅仅isDone=false 远远不够,因为socket.receive是一个永久阻塞的方法
            //所以下面还要close这个socket这样就会捕获到一个异常然后结束
            closeRes();
        }

        private void closeRes() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }
    }
}
