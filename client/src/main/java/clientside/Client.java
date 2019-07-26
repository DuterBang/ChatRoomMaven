package clientside;

import clientside.bean.ServerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Client {
    public static void main(String[] args) throws IOException {
        //System.out.println("流程说明：");
        //System.out.println("步骤1- 客户端 监听自己的UDP端口");
        //System.out.println("步骤2- 客户端 发送广播");
        //System.out.println("步骤3- 客户端 搜索服务器");

        ServerInfo serverInfo=ClientSearcher.searchServer(20000);//1min
        System.out.println("ServerInfo"+serverInfo);

        if (serverInfo!=null){
            ClientByTCP client=null;
            try {
                client= ClientByTCP.startWith(serverInfo);

                if (client==null){
                    return;
                }

                write(client);

            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (client != null) {
                    client.exit();
                }
            }

        }

    }
    private static void write(ClientByTCP tcpClient) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 键盘读取一行
            String str = input.readLine();
            // 发送到服务器
            tcpClient.send(str);

            if ("byebyeli".equalsIgnoreCase(str)) {
                break;
            }
        } while (true);
    }
}
