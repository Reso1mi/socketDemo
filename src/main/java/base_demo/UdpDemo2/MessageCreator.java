package base_demo.UdpDemo2;

/**
 * @author imlgw.top
 * @date 2019/7/8 8:48
 */
public class MessageCreator {
    private static final String SN_HEADER = "收到暗号,我是SN:";
    private static final String PORT_HEADER = "这是暗号,请回送到该端口:";

    public static String buildWithPort(int port) {
        return PORT_HEADER + port;
    }

    //解析出端口
    public static int parsePort(String sn) {
        if (sn.startsWith(PORT_HEADER)) {
            return Integer.parseInt(sn.substring(PORT_HEADER.length()));
        }
        return  -1;
    }

    public static String buildWithSn(String sn){
        return SN_HEADER+sn;
    }

    //解析出 UUID
    public static String parseSn(String sn){
        if(sn.startsWith(SN_HEADER)){
            return sn.substring(SN_HEADER.length());
        }
        return null;
    }
}
