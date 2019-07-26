package clientside;

import ByteUtils.ByteUtils;
import clientside.bean.ServerInfo;
import cons.TCPParas;
import cons.UDPParas;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class ClientSearcher {
    private final static int LISTEN_UDP_PORT= UDPParas.CLIENT_UDP_PORT;

    //使用广播方式，搜索局域网内所有服务器，得到所有的设备列表
    public static ServerInfo searchServer( int timeout) {

        //客户端先启动对自己UDP端口的监听，再开始进行广播
        System.out.println("1-1客户端开始监听服务器是否有回送");
        // 成功收到回送的栅栏
        CountDownLatch receiveLatch = new CountDownLatch(1);
        Listener listener = null;
        try {
            //在发送广播前，先启动监听客户端自己指定的UDP端口
            listener = listen(receiveLatch);
            //客户端向服务器发送广播
            clientSendBroadcast();
            /**
             * CountDownLatch主要有两个方法：countDown()和await()。
             * countDown()方法用于使计数器减一，其一般是执行任务的线程调用，
             * await()方法则使调用该方法的线程处于等待状态，其一般是主线程调用。
             * 这里需要注意的是，countDown()方法并没有规定一个线程只能调用一次，当同一个线程调用多次countDown()方法时，每次都会使计数器减一；
             * 另外，await()方法也并没有规定只能有一个线程执行该方法，如果多个线程同时执行await()方法，
             * 那么这几个线程都将处于等待状态，并且以共享模式享有同一个锁。
             */
            //await等待其余线程的countdown减到0，减到0之后结束await状态
            // 等待timeout时间仍未结束await状态，则强制结束
            receiveLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 搜索服务器操作完成
        System.out.println("3-2搜索服务器操作完成.");
        if (listener == null) {
            return null;
        }
        //listener非空时
        List<ServerInfo> devices = listener.getServerInfoAndClose();
        if (devices.size() > 0) {
            //当前是单服务器测试
            /**
             * devices.get(0);
             */
            return devices.get(0);
        }
        return null;
    }


    private static Listener listen(CountDownLatch receiveLatch) throws InterruptedException {
        System.out.println("1-2Listener开始监听客户端UDP端口");
        CountDownLatch startDownLatch=new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_UDP_PORT, startDownLatch, receiveLatch);
        Thread listenerThread=new Thread(listener);
        listenerThread.start();
        startDownLatch.await();

        return listener;
    }

    //客户端发送广播
    private static void clientSendBroadcast() throws SocketException, UnknownHostException {
        System.out.println("2-1客户端开始发送广播");
        //发送UDP广播，自身端口无需指定，系统分配
        DatagramSocket ds = new DatagramSocket();
        //构建广播消息
        ByteBuffer bf=ByteBuffer.allocate(128);
        //头部
        bf.put(UDPParas.HEADER);
        /**
         * ？？？？？？ //CMD命名
         */
        //CMD命名
        bf.putShort((short) 1);
        //发送client的UDP端口信息,server之后向该端口发送广播
        bf.putInt(LISTEN_UDP_PORT);
        /**
         * ？？？？？？bf.position() + 1
         */
        //构建DatagramPacket
        DatagramPacket dp = new DatagramPacket(bf.array(), bf.position() + 1);
        //广播地址，搜索的是局域网中的所有服务器
        dp.setAddress(InetAddress.getByName(UDPParas.CLIENT_BROADCAST_IP));
        dp.setPort(UDPParas.SERVER_UDP_PORT);

        //发送
        try {
            ds.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            ds.close();
        }

        //结束提醒
        System.out.println("2-2客户端发送广播结束");
    }

    //Listener类
    private static class Listener implements Runnable{
        private final int listenPort;
        private final CountDownLatch startDownLatch;
        private final CountDownLatch receiveDownLatch;
        //CMD 2字节 port 4字节
        private final int minLen=UDPParas.HEADER.length+2+4;
        /**
         * final?
         * byte[ ]和ByteBuffer区别
         */
        private  byte[] reveiveBuffer=new byte[128];
        private DatagramSocket ds=null;
        private final List<ServerInfo> serverInfoList = new ArrayList<>();
        //是否退出while(){ds.reveive(dp)}
        private boolean done = false;

         private Listener(int listenPort, CountDownLatch startDownLatch, CountDownLatch receiveDownLatch) {
            this.listenPort = listenPort;
            this.startDownLatch = startDownLatch;
            this.receiveDownLatch = receiveDownLatch;
        }


        @Override
        public void run() {
            //通知Listener线程已经启动了，确认Listener对象在start之后成功调用了run()方法。
            startDownLatch.countDown();

            try {
                //监听CLIENT_UDP_PORT端口
                ds = new DatagramSocket(listenPort);
                //构建接受packet
                DatagramPacket dp = new DatagramPacket(reveiveBuffer,reveiveBuffer.length);

                while (!done){
                    //接收
                    ds.receive(dp);
                    //打印接受到的信息
                    String ip=dp.getAddress().getHostAddress();
                    int port=dp.getPort();
                    int dataLen=dp.getLength();
                    //getData是拿到整个完整数据
                    byte[] data=dp.getData();
                    //校验接收到的reveiveBuffer
                    boolean isValid = (dataLen >= minLen) && (ByteUtils.startsWith(data, UDPParas.HEADER));

                    System.out.println("3-1客户端收到广播消息，来自服务器 ip: "+ip+"\tport: "+port+" \tdataVaild: "+isValid);

                    if(!isValid){
                        continue;
                    }

                    //将数据从reveiveBuffer(byte[]类型)放入到ByteBuffer类型中
                    /**
                     * 为啥是dataLen不是dataLen-UDPParas.HEADER.length
                     */
                    //ByteBuffer基础操作说明  https://blog.csdn.net/mars5337/article/details/6576417
                    ByteBuffer byteBuffer=ByteBuffer.wrap(reveiveBuffer,UDPParas.HEADER.length,dataLen);
                    final short cmd=byteBuffer.getShort();
                    final int server_TCPPort=byteBuffer.getInt();
                    if (cmd!=2||server_TCPPort<=0){
                        System.out.println("服务器消息解析，cmd:"+cmd+"\tTCPPort:"+server_TCPPort);
                        continue;
                    }

                    /**
                     * dataLen-minLen??
                     */
                    String server_sn=new String(reveiveBuffer,minLen,dataLen-minLen);
                    ServerInfo serverInfo = new ServerInfo(server_sn, ip, server_TCPPort);
                    serverInfoList.add(serverInfo);

                    //成功收到一份服务器信息
                    receiveDownLatch.countDown();
                }


            } catch (SocketException e) {

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close();
            }
        }

        private void close() {
             if (ds!=null){
                 ds.close();
                 ds=null;
             }
        }

        public List<ServerInfo> getServerInfoAndClose() {
             done=true;
             close();
            return serverInfoList;
        }

    }
}
