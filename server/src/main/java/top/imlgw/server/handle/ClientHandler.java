package top.imlgw.server.handle;

import top.imlgw.common.utils.CloseUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * 用来处理客户端的类
 *
 * @author imlgw.top
 * @date 2019/7/17 11:48
 */
public class ClientHandler {

    private final SocketChannel socketChannel;
    private final ServerReadHandler serverReadHandler;
    private final ServerWriterHandler serverWriterHandle;

    private final ClientHandleCallBack clientHandleCallBack;

    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandleCallBack clientHandleCallBack) throws IOException {
        this.socketChannel = socketChannel;
        //设置非阻塞模式
        socketChannel.configureBlocking(false);
        Selector readSelector = Selector.open();
        socketChannel.register(readSelector, SelectionKey.OP_READ);

        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);

        this.serverReadHandler = new ServerReadHandler(readSelector);
        this.serverWriterHandle = new ServerWriterHandler(writeSelector);
        this.clientHandleCallBack = clientHandleCallBack;
        //构造客户端的信息(远端)
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端连接：" + clientInfo);
    }

    public void send(String str) {
        serverWriterHandle.send(str);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    //从外界关闭
    public void stop() {
        serverReadHandler.stopRead();
        serverWriterHandle.stopWriter();
        //关闭通道
        CloseUtils.close(socketChannel);
        System.out.println("客户端已经退出");
        System.out.println("address:" +clientInfo);
    }

    //自我关闭--->自闭(客户端自己退出会触发这个方法)
    private void stopByMyself() {
        stop();
        clientHandleCallBack.onSelfClosed(this);
    }


    //读取并打印到屏幕(启动ClientReadHandle线程)
    public void read2Print() {
        new Thread(serverReadHandler).start();
    }


    /**
     * 回调接口
     */
    public interface ClientHandleCallBack {
        /**
         * 将已经关闭的handle暴露给TCPServer然后从list中移除
         */
        void onSelfClosed(ClientHandler clientHandler);

        /**
         * 将消息交给服务器转发
         *
         * @param clientHandler
         * @param msg
         */
        void onNewMessageArrived(ClientHandler clientHandler, String msg);
    }


    /**
     * 处理服务端用于读取客户端消息的 Handle
     */
    class ServerReadHandler implements Runnable {
        private boolean done = false;

        private final Selector selector;
        private final ByteBuffer byteBuffer;

        public ServerReadHandler(Selector readSelector) {
            this.selector = readSelector;
            this.byteBuffer = ByteBuffer.allocate(256);
        }

        public void run() {
            try {
                do {
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }
                        SelectionKey selectionKey = iterator.next();
                        iterator.remove();
                        //可读的
                        if (selectionKey.isReadable()) {
                            //获取注册的Channel
                            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                            //清空
                            byteBuffer.clear();
                            int read = socketChannel.read(byteBuffer);
                            if (read > 0) {
                                //减的是换行符
                                String str = new String(byteBuffer.array(), 0, read - 1);
                                //将自己和接收到的信息暴露给TCPServer用于转发
                                clientHandleCallBack.onNewMessageArrived(ClientHandler.this, str);
                            } else {
                                System.out.println("服务端无法读取数据");
                                //退出当前客户端
                                //结束当前Handle
                                ClientHandler.this.stopByMyself();
                                break;
                            }
                        }
                    }
                } while (!done);
            } catch (IOException e) {
                if (!done) {
                    //非正常关闭(客户端断开连接)
                    System.err.println("连接异常断开" + e.getMessage());
                    ClientHandler.this.stopByMyself();
                }
            } finally {
                CloseUtils.close(selector);
            }
        }

        public void stopRead() {
            done = true;
            selector.wakeup();
            CloseUtils.close(selector);
        }
    }


    /**
     * 处理服务端向客户端发送消息的Handle
     */
    class ServerWriterHandler {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        //线程池
        private final ExecutorService executorService;

        public ServerWriterHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
            //单例线程池
            executorService = Executors.newSingleThreadExecutor();
        }

        public void send(String str) {
            //因为是使用的异步线程,所以有可能在准备发送的时候已经下线了
            if (done) {
                return;
            }
            //这里如果不用线程池可以用线程通信机制来完成
            executorService.submit(new WriteRunnable(str));
        }

        //线程池的Runnable
        class WriteRunnable implements Runnable {
            private final String msg;

            public WriteRunnable(String msg) {
                //手动加上换行符
                this.msg = msg+"\n";
            }

            @Override
            public void run() {
                if (ServerWriterHandler.this.done) {
                    return;
                }

                byteBuffer.clear();
                byteBuffer.put(msg.getBytes());
                //反转 指针回到初始位置
                byteBuffer.flip();
                while (!done&&byteBuffer.hasRemaining()) {
                    try {
                        //这里一开始是用的PrintStream.println(msg)所以会加上换行符
                        //差点又被视频误导了,这里确实readLine会去掉换行符,但是后面println又加上来了
                        //之前如果用print()其实就接受不到消息了,同样这里的write是不会加上换行符号的，需要我们游动加上
                        //返回的write是写入的字节数
                        int write = socketChannel.write(byteBuffer);
                        //write=0是合法的,因为采用的异步的线程池并没有用Selector,所以它执行的时候有可能当前并不能发送数据
                        if(write<0){
                            System.out.println("服务端无法发送数据");
                            ClientHandler.this.stopByMyself();
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void stopWriter() {
            done = true;
            CloseUtils.close(selector);
            executorService.shutdownNow();
        }
    }
}
