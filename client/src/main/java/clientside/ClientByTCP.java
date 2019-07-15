package clientside;

import ByteUtils.CloseUtils;
import clientside.bean.ServerInfo;

import java.io.*;
import java.net.*;

public class ClientByTCP {
    public static void LinkWith(ServerInfo serverInfo) throws IOException {
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
            Thread clientTCPReadThread = new Thread(clientTCPHandler);
            clientTCPReadThread.start();

            //客户端读取键盘一行，向服务器写数据
            write(socket);
            //退出操作
            clientTCPHandler.exit();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("异常关闭");
        }finally {
            socket.close();
            System.out.println("socket连接断开");
        }

    }

    private static void write(Socket socket) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader keymapInput = new BufferedReader(new InputStreamReader(in));

        //得到Socket输出流，并转换为打印流
        OutputStream outputStream = socket.getOutputStream();
        PrintStream socketPrintStream = new PrintStream(outputStream);

        while(true) {
            // 键盘读取一行
            String str = keymapInput.readLine();
            // 发送到服务器
            socketPrintStream.println(str);

            if ("byebyelj".equalsIgnoreCase(str)) {
                break;
            }
        }

        socketPrintStream.close();

    }

    private static class ClientTCPHandler implements Runnable{
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
