package ru.mail.polis.pavel.epanechkin;

import org.dizitart.no2.IndexType;
import org.dizitart.no2.objects.Index;
import org.dizitart.no2.objects.Indices;

import java.io.Serializable;

@Indices({
        @Index(value = "keyHash", type = IndexType.Unique)
})

public class StorageObject implements Serializable {

    private String keyHash;

    private byte[] key;

    private byte[] value;

    private long timestamp;

    private Boolean removed;

    public StorageObject(byte[] key, byte[] value, long timestamp) {
        this.key = key;
        this.value = value;
        this.removed = false;
        this.timestamp = timestamp;
        this.keyHash = Utils.getSHA256(key);
    }

    public StorageObject() {}

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
        this.keyHash = Utils.getSHA256(key);
    }

    public String getKeyHash() {
        return keyHash;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getRemoved() {
        return removed;
    }

    public void setRemoved(Boolean removed) {
        this.removed = removed;
    }
}
