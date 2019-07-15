package serverside;

import cons.TCPParas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerByTCP serverByTCP=new ServerByTCP(TCPParas.SERVER_TCP_PORT);
        //服务器开始监听自己的TCP端口
        boolean isSucceed = serverByTCP.start();
        if (!isSucceed) {
            System.out.println("服务器TCP端口启动监听失败!");
            return;
        }

        //服务器开始监听自己的UDP端口，并在收到消息后回送TCPParas.SERVER_TCP_PORT端口
        ServerProvider.start();
        //获取键盘输入流
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            //读取一行
            str = bufferedReader.readLine();
            //通过TCP发送给各个客户端（不算是broadcast）
            serverByTCP.LinkWithEachClient(str);
        } while (!"beybeylj".equalsIgnoreCase(str));

        //释放资源
        ServerProvider.checkAndStop();
        serverByTCP.stop();
    }
}
