package clientside;

import clientside.bean.ServerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ClientForceTest {
    private static boolean done=false;
    public static void main(String[] args) throws IOException {

        //通过UDP拿到服务器的连接信息
        ServerInfo serverInfo=ClientSearcher.searchServer(20000);//1min
        System.out.println("ServerInfo"+serverInfo);
        if (serverInfo==null)
            return;

        //创建1000个客户端和服务器之间的TCP连接
        //当前连接数量
        int num=0;
        List<ClientByTCP> clientByTCPs= new ArrayList<>();
        for (int i=0;i<1000;i++){
            try {
                ClientByTCP clientByTCP=ClientByTCP.startWith(serverInfo);
                if (clientByTCP==null){
                    System.out.println("客户端TCP创建失败,连接异常");
                    continue;
                }
                clientByTCPs.add(clientByTCP);
                System.out.println("连接成功客户端数量: "+(++num));
            } catch (IOException e) {
                System.out.println("连接异常");
            }

            //给服务器一个处理连接的时间
            //服务器当同时连接的数量大于50,会拒接后续的同时连接
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        //按下键盘后,1000个客户端开始发送消息
        //只按回车触发
        System.in.read();

        Runnable runnable = () -> {

            while (!done) {
                for (ClientByTCP tcpClient : clientByTCPs) {

                    tcpClient.send("Hello~ljb");
                }
                try {
                    //每个客户端隔1s发送一次Hello~ljb,直到done=true
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();


        //再次按下键盘,结束发送消息
        System.in.read();

        done = true;

        try {
            // 等待1000个发送hello~的线程完成发送操作
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 客户端结束操作
        for (ClientByTCP tcpClient : clientByTCPs) {
            tcpClient.exit();
        }


    }
}
