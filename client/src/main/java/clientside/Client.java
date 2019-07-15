package clientside;

import clientside.bean.ServerInfo;

import java.io.IOException;

public class Client {
    public static void main(String[] args) throws IOException {
        System.out.println("流程说明：");
        System.out.println("步骤1- 客户端 监听自己的UDP端口");
        System.out.println("步骤2- 客户端 发送广播");
        System.out.println("步骤3- 客户端 搜索服务器");

        ServerInfo serverInfo=ClientSearcher.searchServer(20000);//1min
        System.out.println("ServerInfo"+serverInfo);
        System.out.println("here1");
        if (serverInfo!=null){
            ClientByTCP.LinkWith(serverInfo);
            System.out.println("here2");
        }

    }
}
