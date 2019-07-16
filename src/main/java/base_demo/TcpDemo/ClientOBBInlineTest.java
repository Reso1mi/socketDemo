package base_demo.TcpDemo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.Socket;

/**
 * @author imlgw.top
 * @date 2019/7/10 18:00
 */
public class ClientOBBInlineTest {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("127.0.0.1", 20000);
        socket.setOOBInline(true);
        OutputStream out = socket.getOutputStream();
        OutputStreamWriter outWriter = new OutputStreamWriter(out);
        PrintStream printStream=new PrintStream(out);
        printStream.print(65);
        outWriter.write(67);              // 向服务器发送字符"C"
        outWriter.write("hello world\r\n");
        socket.sendUrgentData(65);        // 向服务器发送字符"A"
        socket.sendUrgentData(322);        // 向服务器发送字符"B"
        outWriter.flush();
        socket.sendUrgentData(214);       // 向服务器发送汉字”中”
        socket.sendUrgentData(208);
        socket.sendUrgentData(185);       // 向服务器发送汉字”国”
        socket.sendUrgentData(250);
        socket.close();
    }
}
