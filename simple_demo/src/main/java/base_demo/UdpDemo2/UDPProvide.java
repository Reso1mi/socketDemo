package base_demo.UdpDemo2;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * @author imlgw.top
 * @date 2019/7/7 21:31
 */
public class UDPProvide {
    public static int PROVIDE_LISTEN_PORT = 20000;

    public static void main(String[] args) throws IOException {
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn);
        new Thread(provider).start();
        System.in.read();
        provider.shutdown();
    }

    public static class Provider implements Runnable {
        public volatile boolean isDone = false;
        public DatagramSocket socket = null;
        public final String sn;

        public Provider(String sn) {
            this.sn = sn;
        }

        public void run() {
            System.out.println("UDPProvide is start....");
            try {
                socket = new DatagramSocket(PROVIDE_LISTEN_PORT);
                while (!isDone) {
                    final byte[] buf = new byte[512];
                    //构建接受的DatagramPacket
                    DatagramPacket udp_receive = new DatagramPacket(buf, buf.length);
                    //接受DatagramPacket (阻塞)
                    socket.receive(udp_receive);
                    //获取发送人的SocketAddress
                    InetSocketAddress socketAddress = (InetSocketAddress) udp_receive.getSocketAddress();
                    int datalen = udp_receive.getLength();
                    //获取发送过来的数据
                    String receive = new String(udp_receive.getData(), 0, datalen);
                    //打印获取到的数据
                    System.out.println("receive from the: " + socketAddress);
                    System.out.println("receive data: " + receive);
                    //解析sn,获取需要回送的端口
                    int port = MessageCreator.parsePort(receive);
                    if (port != -1) {
                        //构建回送的DatagramPacket 通过UUID构建消息
                        byte[] responseBody = MessageCreator.buildWithSn(sn).getBytes();
                        DatagramPacket udp_sendBack = new DatagramPacket(responseBody, 0, responseBody.length, socketAddress.getAddress(), port);
                        socket.send(udp_sendBack);
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
            //这里仅仅isDone=true 远远不够,因为socket.receive是一个永久阻塞的方法
            //所以下面还要close这个socket这样就会捕获到一个异常然后结束
            closeRes();
        }

        private void closeRes() {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        }
    }
}
