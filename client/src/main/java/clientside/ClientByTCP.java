package clientside;

import ByteUtils.CloseUtils;
import clientside.bean.ServerInfo;

import java.io.*;
import java.net.*;

public class ClientByTCP {
    //将ClientByTCP 实例化,以便于外部可控,可调用

    private final Socket socket;
    private final ClientTCPHandler readHandler;
    private final PrintStream printStream;

    public ClientByTCP(Socket socket, ClientTCPHandler readHandler) throws IOException {
        this.socket = socket;
        this.readHandler = readHandler;
        printStream=new PrintStream(socket.getOutputStream());
    }

    public void exit() {
        readHandler.exit();
        CloseUtils.close(printStream);
        CloseUtils.close(socket);
    }


    public void send(String msg) {
        printStream.println(msg);
    }

    public static ClientByTCP startWith(ServerInfo serverInfo) throws IOException {
        Socket socket = new Socket();
        //连接超时时间
        socket.setSoTimeout(6000);
        //连接信息，连接本地，端口
        try {
            socket.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getIp()), serverInfo.getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("客户端已发起向服务器的连接");
        System.out.println("客户端ip：" + socket.getLocalAddress() + " Port:" + socket.getLocalPort());
        System.out.println("服务器ip：" + socket.getInetAddress() + " Port:" + socket.getPort());

        try {
            //在clientTCPHandler线程中，从socket的InputStream读取数据
            ClientTCPHandler clientTCPHandler=new ClientTCPHandler(socket.getInputStream());

            clientTCPHandler.start();



            /**由键盘输入改为自动输入
             *             //客户端读取键盘一行，向服务器写数据
             *             write(socket);
             *             //退出操作
             *             clientTCPHandler.exit();
             */

            return new ClientByTCP(socket,clientTCPHandler);


        } catch (IOException e) {

            System.out.println("连接异常关闭");
        }
        return null;
    }



    private static class ClientTCPHandler extends Thread{
        private boolean done=false;
        private final InputStream inputStream;

        private ClientTCPHandler( InputStream inputStream) {
            this.inputStream = inputStream;
        }


        @Override
        public void run() {
            //inputStream转换为BufferedReader，用于读取数据
            BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));
            try {

                while (!done){
                    String str;
                    try {
                        // 客户端拿到一条数据
                        str = socketInput.readLine();
                    } catch (IOException e) {
                        continue;
                    }
                    if (str == null) {
                        System.out.println("连接已关闭，无法读取数据！");
                        break;
                    }
                    // 打印到屏幕
                    System.out.println("客户端读取到的数据为："+str);
                }
            }catch (Exception e){
                System.out.println("TCP连接异常:"+e.getMessage());
            }finally {
                CloseUtils.close(inputStream);
            }

        }

        void exit() {
            done = true;
            CloseUtils.close(inputStream);
        }

    }


}
