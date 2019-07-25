package serverside.handle;

import ByteUtils.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientTCPHandler {
    private final Socket client;
    private final ClientTCPHandlerCallback clientTCPHandlerCallback;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final String clientInfo;

    public ClientTCPHandler(Socket client, ClientTCPHandlerCallback clientTCPHandlerCallback) throws IOException {
        this.client = client;
        this.clientTCPHandlerCallback = clientTCPHandlerCallback;
        this.readHandler = new ClientReadHandler(client.getInputStream());
        this.writeHandler = new ClientWriteHandler(client.getOutputStream());
        this.clientInfo = "ip[" + client.getInetAddress().getHostAddress() + "] Port[" + client.getPort() + "]";
        System.out.println("新客户端连接：" + clientInfo);
    }

    public void exit() {
        readHandler.exit();
        writeHandler.exit();
        CloseUtils.close(client);
        System.out.println("客户端已退出：ip" + client.getInetAddress() + " Port:" + client.getPort());
        //由于ClientTCPHandler(Socket client, ClientTCPHandlerCallback clientTCPHandlerCallback) 构造函数传递的是ServerByTCP对象
        //所以实际执行时这里的clientTCPHandlerCallback对应的就是ServerByTCP
        //回调通知ClientTCPHandler已经关闭，实际调用的是ServerByTCP的实现的接口
        clientTCPHandlerCallback.onSelfClosed(this);

    }

    public void send(String str) {
        //ClientWriteHandler使用线程池
        writeHandler.write(str);
    }

    public String getClientInfo() {

        return clientInfo;
    }

    public void readAndPrint() {
        //启动readHandler线程进行阅读和打印
        readHandler.start();
    }


    private class ClientReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;

        public ClientReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }


        public void exit() {
            done = true;
            CloseUtils.close(inputStream);
        }

        @Override
        public void run() {
            super.run();


            try {
                // 得到输入流，用于接收数据
                BufferedReader clientInput = new BufferedReader(new InputStreamReader(inputStream));
                while (!done){
                    // 客户端拿到一条数据
                    String str= clientInput.readLine();
                    if (str == null) {
                        System.out.println("客户端已无法读取数据！");
                        // 退出当前客户端
                        ClientTCPHandler.this.exit();
                        break;
                    }
                    // 通知到ServerByTCP，由ServerByTCP来进行打印client的信息以及接收到的消息
                    //ClientTCPHandler.this代表把自身传递回去,同时把收到的消息str传递回去
                    clientTCPHandlerCallback.onNewMessageArrived(ClientTCPHandler.this, str);
                }

            } catch (IOException e) {
                if (!done) {
                    System.out.println("连接异常断开");
                    ClientTCPHandler.this.exit();
                }
            }finally {
                // 连接关闭
                CloseUtils.close(inputStream);
            }

        }
    }

    private class ClientWriteHandler {
        private boolean done = false;
        private final PrintStream printStream;
        private final ExecutorService executorService;

        ClientWriteHandler(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);
            this.executorService = Executors.newSingleThreadExecutor();
        }
        public void exit() {
            done = true;
            CloseUtils.close(printStream);
            executorService.shutdownNow();
        }


        public void write(String writeStr) {
            executorService.execute(new WriteRunnable(writeStr));
        }


        private class WriteRunnable implements Runnable {
            private final String msg;
            public WriteRunnable(String writeStr) {
                this.msg=writeStr;
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }

                try {
                    ClientWriteHandler.this.printStream.println(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public interface ClientTCPHandlerCallback {
        // 自身关闭通知
        void onSelfClosed(ClientTCPHandler clientTCPHandler);
        // 收到消息时通知
        void onNewMessageArrived(ClientTCPHandler clientTCPHandler, String msg);

    }
}
