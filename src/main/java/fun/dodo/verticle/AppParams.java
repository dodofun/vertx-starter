package fun.dodo.verticle;

public class AppParams {
    private String rpcClientHost;
    private int rpcClientPort;

    public String getRpcClientHost() {
        return rpcClientHost;
    }

    public void setRpcClientHost(String rpcClientHost) {
        this.rpcClientHost = rpcClientHost;
    }

    public int getRpcClientPort() {
        return rpcClientPort;
    }

    public void setRpcClientPort(int rpcClientPort) {
        this.rpcClientPort = rpcClientPort;
    }
}
