package base_demo.httpMock;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpMock {

    //利用单线程池去处理客户端请求,这样就不用每个连接创建一个线程了
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();


    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();//监听新进来的 TCP连接的通道，打开 ServerSocketChannel
        ssc.socket().bind(new InetSocketAddress(8080));//绑定8080端口
        ssc.configureBlocking(false);//设置非阻塞模式
        Selector selector = Selector.open();//创建选择器
        ssc.register(selector, SelectionKey.OP_ACCEPT);//给选择器注册通道
        System.out.println("服务端端口:" + 8080);
        while (true) { //监听新进来的连接
            int select = selector.select();
            if (select == 0) { //如果选择的通道为0，最长会阻塞 timeout毫秒
                System.out.println("等待请求超时......");
                continue;
            }
            //有就绪的事件来了
            System.out.println("开始处理请求.....");
            //遍历就绪事件的key统统交给线程池去处理
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();
                if (key.isAcceptable()) {
                    //拿到客户端的Channel
                    System.out.println("accept");
                    SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
                    socketChannel.configureBlocking(false);//设置非阻塞模式
                    //注册READ事件
                    socketChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(1024));
                    //buffer分配一个缓冲区 大小为1024
                } else if (key.isReadable()) {
                    System.out.println("readable");
                    executorService.submit(new HttpHandler(key));
                }
                keyIter.remove();
            }
        }
    }
}