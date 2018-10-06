package pavel_epanechkin;

import org.dizitart.no2.IndexType;
import org.dizitart.no2.objects.Index;
import org.dizitart.no2.objects.Indices;
import pavel_epanechkin.Utils;

@Indices({
        @Index(value = "keyHash", type = IndexType.Unique)
})

public class StorageObject {

    private String keyHash;

    private byte[] key;

    private byte[] value;

    public StorageObject(byte[] key, byte[] value) {
        this.key = key;
        this.value = value;
        this.keyHash = Utils.getMD5(key);
    }

    public StorageObject() {}


    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
        this.keyHash = Utils.getMD5(key);
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }
}
