package pavel_epanechkin;

import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

import static org.dizitart.no2.objects.filters.ObjectFilters.eq;

public class KeyValueDao implements KVDao {

    private Nitrite storage;

    private ObjectRepository<StorageObject> objectRepository;

    @NotNull
    @Override
    public byte[] get(@NotNull byte[] key) throws NoSuchElementException, IOException {
        String keyHash = Utils.getMD5(key);
        Cursor<StorageObject> cursor = objectRepository.find(eq("keyHash", keyHash));

        if (cursor.size() == 0)
            throw new NoSuchElementException();

        return cursor.firstOrDefault().getValue();
    }

    @Override
    public void upsert(@NotNull byte[] key, @NotNull byte[] value) throws IOException {
        String keyHash = Utils.getMD5(key);
        objectRepository.update(eq("keyHash", keyHash), new StorageObject(key, value), true);
    }

    @Override
    public void remove(@NotNull byte[] key) throws IOException {
        String keyHash = Utils.getMD5(key);
        objectRepository.remove(eq("keyHash", keyHash));
    }

    @Override
    public void close() throws IOException {
        objectRepository.close();
        storage.close();
    }

    public KeyValueDao(@NotNull File storagePath) {
        storage = Nitrite.builder().filePath(storagePath.getPath() + File.separator + "storage.db").openOrCreate();
        objectRepository = storage.getRepository(StorageObject.class);
    }
}
