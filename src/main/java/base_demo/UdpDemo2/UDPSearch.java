package base_demo.UdpDemo2;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author imlgw.top
 * @date 2019/7/7 21:31
 */

public class UDPSearch {
    private static final int SEARCH_LISTEN_PORT = 30000;
    private static DatagramSocket socket = null;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("UDPSearch is start...");
        Listener listen = listen();
        sendBoard();
        System.in.read();
        List<Device> devices = listen.closeAndGetDeviceList();
        for (Device device : devices) {
            System.out.println(device);
        }
    }

    private static Listener listen() throws InterruptedException {
        System.out.println("UDPSearch Listener is start");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(SEARCH_LISTEN_PORT, countDownLatch);
        new Thread(listener).start();
        //阻塞等待listener启动 其实用join也可以 主要是为了保证不会listen要先于广播启动，避免丢失消息
        countDownLatch.await();
        return listener;
    }

    public static void sendBoard() throws IOException {
        //系统自动分配的端口
        DatagramSocket socket = new DatagramSocket();
        //构建socket
        byte[] buff = MessageCreator.buildWithPort(SEARCH_LISTEN_PORT).getBytes();
        //构建发送段
        DatagramPacket udp_send = new DatagramPacket(buff, buff.length);
        //广播地址
        udp_send.setAddress(InetAddress.getByName("255.255.255.255"));
        //接收方的端口
        udp_send.setPort(UDPProvide.PROVIDE_LISTEN_PORT);
        socket.send(udp_send);
        socket.close();
        System.out.println("UDPSearch Board is over");
    }

    private static class Device {
        int port;
        String ip;
        String sn;

        public Device(int port, String ip, String sn) {
            this.port = port;
            this.ip = ip;
            this.sn = sn;
        }

        @Override
        public String toString() {
            return "Device{" +
                    "port=" + port +
                    ", ip='" + ip + '\'' +
                    ", sn='" + sn + '\'' +
                    '}';
        }
    }

    public static class Listener implements Runnable {
        private final int listenPort;
        private final CountDownLatch countDownLatch;
        private final List<Device> deviceList = new ArrayList<Device>();
        //private static DatagramSocket socket = null;

        private boolean isDone = false;

        public Listener(int listenPort, CountDownLatch countDownLatch) {
            this.listenPort = listenPort;
            this.countDownLatch = countDownLatch;
        }

        public void run() {
            //通知已经启动
            countDownLatch.countDown();
            try {
                socket = new DatagramSocket(listenPort);
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
                    String sn = new String(udp_receive.getData(), 0, datalen);
                    System.out.println("back from the：" + socketAddress);
                    System.out.println("back data：" + sn);
                    //解析出 UUID
                    sn = MessageCreator.parseSn(sn);
                    if (sn != null) {
                        deviceList.add(new Device(socketAddress.getPort(), socketAddress.getAddress().toString(), sn));
                    }
                }
            } catch (IOException e) {
                //e.printStackTrace();
            } finally {
                closeRes();
            }
            System.out.println("UDPSearch Listener is Finished...");
        }

        public void closeRes() {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        }

        List<Device> closeAndGetDeviceList() {
            isDone = true;
            closeRes();
            return deviceList;
        }
    }
}
