package serverside;

import ByteUtils.ByteUtils;
import cons.TCPParas;
import cons.UDPParas;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

public class ServerProvider {
    //RESPONSE_TCP_PORT指服务器回送给客户端自己的TCP端口
    private  static int RESPONSE_TCP_PORT= TCPParas.SERVER_TCP_PORT;
    //单例模式
    private static ServerUDPProvider PROVIDER_INSTANCE;

    public static void start() {
        checkAndStop();
        String sn = UUID.randomUUID().toString();

        ServerUDPProvider serverUDPProvider = new ServerUDPProvider(sn,RESPONSE_TCP_PORT);
        serverUDPProvider.start();
        PROVIDER_INSTANCE = serverUDPProvider;
    }

    //当前线程中存在啊PROVIDER_INSTANCE对象时，关闭该线程中的udpsocket，同时退出while(!done)循环
    public static void checkAndStop() {
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }

    private static class ServerUDPProvider extends Thread{
        private final byte[] sn;
        private final int port;
        private boolean done = false;
        private DatagramSocket ds = null;

        // 存储消息的Buffer
        final byte[] buffer = new byte[128];

        ServerUDPProvider(String sn, int port) {
            this.sn = sn.getBytes();
            this.port = port;
        }



        @Override
        public void run() {
            super.run();
            System.out.println("服务器UDP端口监听启动");
            try {
                // 服务器监听自己的UDP 端口
                ds = new DatagramSocket(UDPParas.SERVER_UDP_PORT);
                //构建接收消息的Packet
                DatagramPacket receivePacket= new DatagramPacket(buffer,buffer.length);

                while (!done){
                    // 接收(阻塞)
                    ds.receive(receivePacket);

                    // 打印接收到的信息与发送者的信息
                    // 客户端的IP地址
                    String clientIp = receivePacket.getAddress().getHostAddress();
                    int clientPort = receivePacket.getPort();
                    int clientDataLen = receivePacket.getLength();
                    byte[] clientData = receivePacket.getData();
                    boolean isValid = clientDataLen >= (UDPParas.HEADER.length + 2 + 4) && ByteUtils.startsWith(clientData, UDPParas.HEADER);

                    System.out.println("服务器UDP端口收到消息来自客户端： ip:" + clientIp + "\tport:" + clientPort + "\tdataValid:" + isValid);

                    if (!isValid){
                        // 无效下个循环
                        continue;
                    }

                    // 有效时，解析命令（cmd=1）与回送端口(responsePort)，
                    // 这里的responsePort是由客户端发来的消息中携带的
                    int index = UDPParas.HEADER.length;
                    short cmd = (short) ((clientData[index++] << 8) | (clientData[index++] & 0xff));
                    int responsePort = (((clientData[index++]) << 24) |
                            ((clientData[index++] & 0xff) << 16) |
                            ((clientData[index++] & 0xff) << 8) |
                            ((clientData[index] & 0xff)));

                    // 判断解析出来的数据合法性
                    if (cmd == 1 && responsePort > 0) {
                        // 构建一份回送数据
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPParas.HEADER);
                        byteBuffer.putShort((short) 2);
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);
                        //byteBuffer当前的游标所指位置
                        int len = byteBuffer.position();
                        //构建回送的Packet
                        DatagramPacket responsePacket = new DatagramPacket(buffer, len, receivePacket.getAddress(), responsePort);
                        ds.send(responsePacket);
                        System.out.println("服务器UDP向客户端:" + clientIp + "\tport:" + responsePort + "响应了"+"\tdataLen:" + len+"消息");
                    }else {
                        System.out.println("服务器UDP收到的消息cmd合法， cmd:" + cmd + "\tport:" + port);
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                //当报异常退出时所要调用的部分
                close();
            }


            // 完成
            System.out.println("服务器UDP过程结束.");

        }


        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        public void exit() {
            done=true;
            close();
        }
    }
}
