package clientside.bean;

public class ServerInfo {
    //服务器设备号
    private String sn;
    //ip
    private String ip;
    //port(TCP)
    private int port;


    public ServerInfo(String sn, String ip, int port) {
        this.sn = sn;
        this.ip = ip;
        this.port = port;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "sn='" + sn + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
