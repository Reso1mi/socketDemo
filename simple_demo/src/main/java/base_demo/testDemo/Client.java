package base_demo.testDemo;


import java.io.*;
import java.net.*;

/**
 * @author imlgw.top
 * @date 2019/7/20 10:21
 */
public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), 30000));
        InputStream inputStream = socket.getInputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        int read = -1;
        byte[] buffer = new byte[128];
        char[] b=new char[128];
        read = reader.read(b);
        System.out.println("读取到的字符数"+read);
        //转换为String
        String string = new String(b, 0, read);
        System.out.println(string);
    }
}
