package base_demo.UdpDemo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

/**
 * @author imlgw.top
 * @date 2019/7/7 21:31
 */
public class UDPProvide {
    public static void main(String[] args) throws IOException {
        //监听20000端口
        DatagramSocket socket=new DatagramSocket(20000);
        System.out.println("UDPProvide is start....");
        final byte[] buf=new byte[512];
        //构建接受的DatagramPacket
        DatagramPacket udp_receive=new DatagramPacket(buf,buf.length);
        //构建接受的DatagramPacket (阻塞)
        socket.receive(udp_receive);

        //获取发送人的SocketAddress
        SocketAddress socketAddress = udp_receive.getSocketAddress();
        int datalen = udp_receive.getLength();
        //获取发送的数据
        String receive=new String(udp_receive.getData(),0,datalen);
        System.out.println("receive from the: "+socketAddress);
        System.out.println("receive data: "+ receive);
        //构建响应的DatagramPacket
        byte[] bytes = ("provider receive the data success "+datalen).getBytes();
        DatagramPacket udp_sendBack=new DatagramPacket(bytes,bytes.length,socketAddress);
        socket.send(udp_sendBack);
        //结束
        System.out.println("UDPProvide Finished.");
        socket.close();
    }
}
