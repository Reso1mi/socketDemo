package base_demo.UdpDemo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @author imlgw.top
 * @date 2019/7/7 21:31
 */

public class UDPSearch {
    public static void main(String[] args) throws IOException {
        System.out.println("UDPSearch is ready...");
        //构建socket
        DatagramSocket socket = new DatagramSocket();
        byte[] buff= "hello world".getBytes();
        //构建发送段
        DatagramPacket udp_send=new DatagramPacket(buff,buff.length);
        udp_send.setAddress(InetAddress.getLocalHost());
        udp_send.setPort(20000);
        socket.send(udp_send);

        //获取响应段
        final byte[] buf=new byte[512];
        DatagramPacket udp_receive=new DatagramPacket(buf,buf.length);
        socket.receive(udp_receive);
        String s = new String(udp_receive.getData(), 0, udp_receive.getLength());
        System.out.println(s);
        System.out.println("UDPSearch is over");
        socket.close();
    }
}
