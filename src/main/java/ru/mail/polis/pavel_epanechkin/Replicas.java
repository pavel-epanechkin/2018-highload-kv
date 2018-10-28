package ru.mail.polis.pavel_epanechkin;

public class Replicas {
    private int ack;
    private int from;

    public Replicas(int ack, int from) {
        this.ack = ack;
        this.from = from;
    }

    public int getAck() {
        return ack;
    }

    public void setAck(int ack) {
        this.ack = ack;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }
}
