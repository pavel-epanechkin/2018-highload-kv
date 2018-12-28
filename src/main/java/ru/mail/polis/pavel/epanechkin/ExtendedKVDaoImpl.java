package ru.mail.polis.pavel.epanechkin;

import org.dizitart.no2.objects.Cursor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

import static org.dizitart.no2.objects.filters.ObjectFilters.eq;

public class ExtendedKVDaoImpl extends KVDaoImpl {

    public ExtendedKVDaoImpl(@NotNull File storagePath) {
        super(storagePath);
    }

    @NotNull
    public StorageObject getRecord(@NotNull byte[] key) throws NoSuchElementException, IOException {
        String keyHash = Utils.getSHA256(key);
        StorageObject object = cache.getIfPresent(keyHash);

        if (object == null) {
            Cursor<StorageObject> cursor = objectRepository.find(eq("keyHash", keyHash));

            if (cursor.size() == 0)
                throw new NoSuchElementException();

            object = cursor.firstOrDefault();
            cache.put(keyHash, object);
        }

        return object;
    }

    public void setRemoved(@NotNull byte[] key) throws IOException {
        String keyHash = Utils.getSHA256(key);
        Cursor<StorageObject> cursor = objectRepository.find(eq("keyHash", keyHash));

        if (cursor.size() > 0) {
            StorageObject storageObject = cursor.firstOrDefault();
            storageObject.setRemoved(true);
            objectRepository.update(eq("keyHash", keyHash), storageObject, false);
            cache.invalidate(keyHash);
        }
    }
}
