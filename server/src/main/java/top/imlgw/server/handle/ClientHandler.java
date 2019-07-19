package top.imlgw.server.handle;
import top.imlgw.common.utils.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用来处理客户端的类
 *
 * @author imlgw.top
 * @date 2019/7/17 11:48
 */
public class ClientHandler {

    private final Socket socket;
    private final ServerReadHandler serverReadHandler;
    private final ServerWriterHandler serverWriterHandle;

    private final ClientHandleCallBack clientHandleCallBack;

    private final String clientInfo;

    public ClientHandler(Socket socket, ClientHandleCallBack clientHandleCallBack) throws IOException {
        this.socket = socket;
        this.serverReadHandler = new ServerReadHandler(socket.getInputStream());
        this.serverWriterHandle = new ServerWriterHandler(socket.getOutputStream());
        this.clientHandleCallBack = clientHandleCallBack;
        //构造客户端的信息
        this.clientInfo="[ip]:"+socket.getInetAddress()+"[port]:"+socket.getPort();
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
        CloseUtils.close(socket);
        System.out.println("客户端已经退出");
        System.out.println("address:" + socket.getInetAddress() + ",port:" + socket.getPort());
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
         * @param clientHandler
         * @param msg
         */
        void onNewMessageArrived(ClientHandler clientHandler,String msg);
    }


    /**
     * 处理服务端用于读取客户端消息的 Handle
     */
    class ServerReadHandler implements Runnable {
        private boolean done = false;
        private final InputStream inputStream;

        public ServerReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public void run() {
            try {
                //输入流获取信息
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                do {
                    String str = reader.readLine();
                    if (str == null) {
                        System.out.println("客户端已经无法发送数据");
                        //结束当前Handle
                        ClientHandler.this.stopByMyself();
                        break;
                    }
                    //打印到屏幕
                    //System.out.println(s);
                    //将自己和接收到的信息暴露给TCPServer用于转发
                    clientHandleCallBack.onNewMessageArrived(ClientHandler.this,str);
                } while (!done);
            } catch (IOException e) {
                if (!done) {
                    //非正常关闭(客户端断开连接)
                    System.err.println("连接异常断开" + e.getMessage());
                    ClientHandler.this.stopByMyself();
                }
            } finally {
                CloseUtils.close(inputStream);
            }
        }

        public void stopRead() {
            done = true;
            CloseUtils.close(inputStream);
        }
    }


    /**
     * 处理服务端向客户端发送消息的Handle
     */
    class ServerWriterHandler {
        private boolean done = false;
        private final PrintStream printStream;
        //线程池
        private final ExecutorService executorService;

        public ServerWriterHandler(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);
            //单例线程池
            executorService = Executors.newSingleThreadExecutor();
        }

        public void send(String str) {
            //因为是使用的异步线程,所以有可能在准备发送的时候已经下线了
            if(done){
                return;
            }
            //这里如果不用线程池可以用线程通信机制来完成
            executorService.submit(new WriteRunnable(str));
        }

        //线程池的Runnable
        class WriteRunnable implements Runnable {
            private final String msg;

            public WriteRunnable(String msg) {
                this.msg = msg;
            }

            @Override
            public void run() {
                if (ServerWriterHandler.this.done) {
                    return;
                }
                try {
                    ServerWriterHandler.this.printStream.println(msg);
                } catch (Exception e) {
                    System.out.println("write 异常退出：" + e.getMessage());
                }
            }
        }

        public void stopWriter() {
            done = true;
            CloseUtils.close(printStream);
            executorService.shutdownNow();
        }
    }
}
