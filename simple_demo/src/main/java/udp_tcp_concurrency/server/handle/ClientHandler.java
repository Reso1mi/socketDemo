package udp_tcp_concurrency.server.handle;

import udp_tcp_concurrency.utils.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用来处理客户端的类
 * @author imlgw.top
 * @date 2019/7/17 11:48
 */
public  class ClientHandler {

    private final Socket socket;
    private final ServerReadHandler serverReadHandler;
    private final ServerWriterHandler serverWriterHandle;

    private final CloseNotify closeNotify;

    public ClientHandler(Socket socket, CloseNotify closeNotify) throws IOException {
        this.socket = socket;
        this.serverReadHandler = new ServerReadHandler(socket.getInputStream());
        this.serverWriterHandle = new ServerWriterHandler(socket.getOutputStream());
        this.closeNotify = closeNotify;
        System.out.println("新客户端连接：" + socket.getInetAddress() + "port：" + socket.getPort());
    }

    public void send(String str) {
        serverWriterHandle.send(str);
    }

    //从外界关闭
    public void stop() {
        serverReadHandler.stopRead();
        serverWriterHandle.stopWriter();
        CloseUtils.close(socket);
        System.out.println("客户端已经退出");
        System.out.println("address:" + socket.getInetAddress() + ",port:" + socket.getPort());
    }

    //自我关闭--->自闭
    private void stopByMyself() {
        stop();
        closeNotify.onSelfClosed(this);
    }


    //读取并打印到屏幕(启动ClientReadHandle线程)
    public void read2Print() {
        new Thread(serverReadHandler).start();
    }

    /**
     *  将已经关闭的handle暴露给TCPServer然后从list中移除
     */
    public interface CloseNotify{
        void onSelfClosed(ClientHandler clientHandler);
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
                    String s = reader.readLine();
                    if (s == null) {
                        System.out.println("客户端已经无法发送数据");
                        //结束当前Handle
                        ClientHandler.this.stopByMyself();
                        break;
                    }
                    //打印到屏幕
                    System.out.println(s);
                } while (!done);
            } catch (IOException e) {
                if (!done) {
                    //非正常关闭
                    System.err.println("连接异常断开"+e.getMessage());
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
    class ServerWriterHandler  {
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
            //这里如果不用线程池
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
                if(ServerWriterHandler.this.done){
                    return;
                }
                try {
                    ServerWriterHandler.this.printStream.println(msg);
                }catch (Exception e){
                    System.out.println("write 异常退出："+e.getMessage());
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
