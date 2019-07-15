package cons;

public class UDPParas {
    //客户端已知服务器的UDP端口，向该端口发消息，发的是自己的UDP端口；
    //服务器接收到客户端的UDP消息后，向各个客户端UDP端口广播消息，广播的是自己的TCP端口
    //客户端通过该TCP端口向服务器发起连接


    //服务器UDP端口
    public static int SERVER_UDP_PORT=50000;
    //客户端广播的局域网地址
    public static String CLIENT_BROADCAST_IP="255.255.255.255";
    //客户端UDP端口
    public static int CLIENT_UDP_PORT=60000;
    //客户端发送广播的消息头
    public static byte[] HEADER = new byte[]{5,5,5,5,5,5,5,5};

}
