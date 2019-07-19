package udp_tcp_concurrency.client;

import udp_tcp_concurrency.client.bean.ServerInfo;
import udp_tcp_concurrency.constants.UDPConstants;
import udp_tcp_concurrency.utils.ByteUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author imlgw.top
 * @date 2019/7/14 20:02
 */
public class ClientSearcher {
    private static final int LISTEN_PORT=UDPConstants.PORT_CLIENT_RESPONSE;

    public static ServerInfo searchServer(int timeout){
        System.out.println("UDPSearcher Started.");
        //成功收到回送的栅栏
        CountDownLatch receiveLatch =new CountDownLatch(1);
        Listener listener=null;
        try {
            listener=listen(receiveLatch);
            sendBoard();
            //等待监听完成为止
            receiveLatch.await(timeout,TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("UDPSearcher Finished.");
        if(listener==null){
            return null;
        }
        List<ServerInfo> devices=listener.closeAndGetDeviceList();
        if(devices.size()>0){
            return  devices.get(0);
        }
        return null;
    }


    private static Listener listen(CountDownLatch receiveLatch) throws InterruptedException {
        System.out.println("UDPSearch Listener is start");
        CountDownLatch startLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT,startLatch,receiveLatch);
        new Thread(listener).start();
        //阻塞当前线程(主线程)等待listener启动 其实用join也可以 主要是为了保证不会listen要先于广播启动，避免丢失消息
        startLatch.await();
        return listener;
    }

    public static void sendBoard() throws IOException {
        System.out.println("UDPSearcher sendBoardCast started.");
        //系统自动分配的端口
        DatagramSocket ds = new DatagramSocket();
        //构建一份请求数据
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        //头部
        byteBuffer.put(UDPConstants.HEADER);
        //CMD
        byteBuffer.putShort((short)1);
        //回送端口信息
        byteBuffer.putInt(LISTEN_PORT);
        //构建发送段
        DatagramPacket udp_send = new DatagramPacket(byteBuffer.array(), byteBuffer.position()+1);
        //广播地址
        udp_send.setAddress(InetAddress.getByName("255.255.255.255"));
        //接收方的端口
        udp_send.setPort(UDPConstants.PORT_SERVER);
        ds.send(udp_send);
        ds.close();
        System.out.println("UDPSearch Board is over");
    }


    public static class Listener implements Runnable {
        private final int listenPort;
        private final CountDownLatch startDownLatch;
        private final  CountDownLatch receiveLatch;

        private List<ServerInfo> serverInfos=new ArrayList<ServerInfo>();
        private final byte[] buffer=new byte[128];
        private final int minLen =UDPConstants.HEADER.length+2+4;
        private boolean isDone =false;
        private static DatagramSocket ds = null;

        public Listener(int listenPort, CountDownLatch startDownLatch, CountDownLatch receiveLatch) {
            this.listenPort = listenPort;
            this.startDownLatch = startDownLatch;
            this.receiveLatch = receiveLatch;
        }

        //监听服务端的UDP回送
        public void run() {
            //通知已经启动 -1
            startDownLatch.countDown();
            try {
                ds = new DatagramSocket(listenPort);
                DatagramPacket udp_receive = new DatagramPacket(buffer, buffer.length);
                while (!isDone) {
                    //接受DatagramPacket (阻塞)
                    ds.receive(udp_receive);
                    //获取回送人的SocketAddress
                    InetSocketAddress socketAddress = (InetSocketAddress) udp_receive.getSocketAddress();
                    int datalen = udp_receive.getLength();
                    byte []data=udp_receive.getData();
                    boolean isValid=datalen>=minLen && ByteUtils.startWith(data,UDPConstants.HEADER);
                    System.out.println("back from the：" + socketAddress);
                    System.out.println("isValid:" + isValid);
                    if(!isValid){
                        continue;
                    }
                    //data == buffer ?? lets test
                    System.out.println(data.hashCode()+","+buffer.hashCode());

                    ByteBuffer byteBuffer=ByteBuffer.wrap(buffer,UDPConstants.HEADER.length,datalen);
                    final short cmd=byteBuffer.getShort();
                    //获取回送的TCP端口
                    final int serverPort=byteBuffer.getInt();
                    if(cmd!=2||serverPort<=0){
                        System.err.println("UDPSearch receive cmd:"+cmd);
                        System.err.println("serverPort:"+serverPort);
                        continue;
                    }
                    String sn=new String(buffer,minLen,datalen-minLen);
                    ServerInfo serverInfo=new ServerInfo(sn,serverPort,socketAddress.getAddress().toString());
                    serverInfos.add(serverInfo);
                    //通知接收完成
                    receiveLatch.countDown();
                }
            } catch (IOException e) {
                //e.printStackTrace();
            } finally {
                closeRes();
            }
            System.out.println("UDPSearch Listener is Finished...");
        }

        public void closeRes() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        List<ServerInfo> closeAndGetDeviceList() {
            isDone = true;
            closeRes();
            return serverInfos;
        }
    }
}
