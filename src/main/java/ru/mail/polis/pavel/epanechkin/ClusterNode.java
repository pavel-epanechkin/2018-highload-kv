package ru.mail.polis.pavel.epanechkin;

import one.nio.http.HttpClient;
import one.nio.net.ConnectionString;

public class ClusterNode {
    private String address;
    private int port;
    private String id;
    private HttpClient httpClient;

    public ClusterNode(String address) {
        this.address = address;
        this.port = getPortFromName(address);
        this.id = Utils.getSHA256(address.getBytes());
        httpClient = new HttpClient(new ConnectionString(address));
    }

    private int getPortFromName(String name) {
        String[] tmp = name.split(":");
        return Integer.parseInt(tmp[2]);
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public int getDistance(String string) {
        return Utils.getSimpleStringsDistance(string, id);
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}
