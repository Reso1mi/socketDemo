package udp_tcp_demo.constants;

/**
 * @author imlgw.top
 * @date 2019/7/14 18:06
 */
public class UDPConstants {
    //公用头部前8个字节都为7
    public static byte[] HEADER =new byte[]{7,7,7,7,7,7,7,7};
    //服务端接收广播的端口
    public static int PORT_SERVER =30201;
    //服务端接收到数据后回送给客户端的端口
    public static int PORT_CLIENT_RESPONSE =30202;
}
