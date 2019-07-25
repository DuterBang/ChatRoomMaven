package serverside;

import serverside.handle.ClientTCPHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerByTCP implements ClientTCPHandler.ClientTCPHandlerCallback {
    private final int port;
    private ClientTCPListener clientTCPListener;
    private List<ClientTCPHandler> clientTCPHandlers = new ArrayList<>();


    public ServerByTCP(int serverTcpPort) {
        this.port=serverTcpPort;
    }

    public boolean start() {
        //ClientListener服务器用来监听自身的TCP端口
        ClientTCPListener listener = null;
        try {
            listener = new ClientTCPListener(port);
            clientTCPListener=listener;
            /**
             *             listener.start();还是            clientTCPListener.start();
             *             以及先start()在赋值还是先赋值在start()
             */
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


    public void stop() {
        if (clientTCPListener!=null){
            clientTCPListener.exit();
        }
        //保证线程安全,因为既有遍历,又有clientTCPHandlers.remove(clientTCPHandler);
        synchronized (ServerByTCP.this){
            for (ClientTCPHandler clientTCPHandler : clientTCPHandlers) {
                clientTCPHandler.exit();
            }
            //清空列表
            clientTCPHandlers.clear();
        }
    }

    //synchronized加在方法上,默认同步的就是当前的实例,即ServerByTCP.this
    public synchronized void LinkWithEachClient(String str) {
        for (ClientTCPHandler clientTCPHandler : clientTCPHandlers) {
            clientTCPHandler.send(str);
        }

    }


    @Override
    public synchronized void onSelfClosed(ClientTCPHandler clientTCPHandler) {
        clientTCPHandlers.remove(clientTCPHandler);
    }

    @Override
    public void onNewMessageArrived(ClientTCPHandler clientTCPHandler, String msg) {
        System.out.println("服务器TCP收到客户端："+clientTCPHandler.getClientInfo()+"消息:"+msg);
    }


    private class ClientTCPListener extends Thread{
        private ServerSocket serverSocket;
        private boolean done=false;
        private ClientTCPListener(int port) throws IOException {
            serverSocket = new ServerSocket(port);
            System.out.println("服务器信息：" + serverSocket.getInetAddress() + " P:" + serverSocket.getLocalPort());
        }

        @Override
        public void run() {
            super.run();
            System.out.println("服务器TCP端口开始监听");

            //得到客户端连接
            Socket client;
            while (!done){
                try {
                    //拿到客户端
                    client= serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }

                // 客户端构建异步线程
                //这里相当于ServerByTCP的对象就是clientTCPHandlerCallback，回调时就是回调到了ServerByTCP的接口实现方法上
                ClientTCPHandler clientTCPHandler= null;
                try {
                    clientTCPHandler = new ClientTCPHandler(client, ServerByTCP.this);


                } catch (IOException e) {
                    e.printStackTrace();
                }
                // 读取数据并打印
                clientTCPHandler.readAndPrint();
                // 添加时保证线程安全
                synchronized (ServerByTCP.this) {
                    clientTCPHandlers.add(clientTCPHandler);
                }

            }
            System.out.println("服务器已关闭连接！");
        }

        private void exit() {
            done = true;
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
