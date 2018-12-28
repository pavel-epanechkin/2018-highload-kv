package ru.mail.polis.pavel.epanechkin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

import static org.dizitart.no2.objects.filters.ObjectFilters.eq;

public class KVDaoImpl implements KVDao {

    private Nitrite storage;

    protected ObjectRepository<StorageObject> objectRepository;

    private static final int CACHE_SIZE = 1000;

    protected Cache<String, StorageObject> cache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE).build();

    @NotNull
    @Override
    public byte[] get(@NotNull byte[] key) throws NoSuchElementException, IOException {
        String keyHash = Utils.getSHA256(key);
        StorageObject object = cache.getIfPresent(keyHash);

        if (object == null) {
            Cursor<StorageObject> cursor = objectRepository.find(eq("keyHash", keyHash));

            if (cursor.size() == 0)
                throw new NoSuchElementException();

            object = cursor.firstOrDefault();
            cache.put(keyHash, object);
        }
        return object.getValue();
    }

    @Override
    public void upsert(@NotNull byte[] key, @NotNull byte[] value) throws IOException {
        String keyHash = Utils.getSHA256(key);
        Long timestamp = System.currentTimeMillis();
        StorageObject object = new StorageObject(key, value, timestamp);
        objectRepository.update(eq("keyHash", keyHash), object, true);
        cache.put(keyHash, object);
    }

    @Override
    public void remove(@NotNull byte[] key) throws IOException {
        String keyHash = Utils.getSHA256(key);
        objectRepository.remove(eq("keyHash", keyHash));
        cache.invalidate(keyHash);
    }

    @Override
    public void close() throws IOException {
        objectRepository.close();
        storage.close();
    }

    public KVDaoImpl(@NotNull File storagePath) {
        storage = Nitrite.builder()
                .filePath(storagePath.getPath() + File.separator + "storage.db")
                .openOrCreate();
        objectRepository = storage.getRepository(StorageObject.class);
    }
}
