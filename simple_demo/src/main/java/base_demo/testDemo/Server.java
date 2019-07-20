package base_demo.testDemo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author imlgw.top
 * @date 2019/7/20 10:30
 */
public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(30000);
        Socket accept = serverSocket.accept();
        OutputStream outputStream = accept.getOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        printStream.print("你好");
        System.in.read();
    }
}
