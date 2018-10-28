package ru.mail.polis.pavel_epanechkin;

import one.nio.http.Response;
import ru.mail.polis.KVDao;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class EntityService {

    private KVDao dao;

    public static final String ENTITY_TIMESTAMP_HEADER = "X-Timestamp: ";
    public static final String ENTITY_REMOVED_HEADER = "X-Removed: ";

    public EntityService(KVDao dao) {
        this.dao = dao;
    }

    public Response getEntity(String id) {
        Response response;

        try {
            StorageObject object = ((ExtendedKeyValueDao) dao).getRecord(id.getBytes(Charset.defaultCharset()));

            response = Response.ok(object.getValue());
            response.addHeader(ENTITY_TIMESTAMP_HEADER + object.getTimestamp().getTime());

            if (object.getRemoved())
                response.addHeader(ENTITY_REMOVED_HEADER + true);
        }
        catch (NoSuchElementException e) {
            response = new Response(Response.NOT_FOUND, Response.EMPTY);
        }
        catch (IOException e) {
            response = new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return response;
    }

    public Response saveEntity(String id, byte[] value) {
        Response response;

        try {
            dao.upsert(id.getBytes(Charset.defaultCharset()), value);
            response = new Response(Response.CREATED, Response.EMPTY);
        }
        catch (IOException e) {
            response = new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return response;
    }

    public Response removeEntity(String id) {
        Response response;

        try {
            ((ExtendedKeyValueDao) dao).setRemoved(id.getBytes(Charset.defaultCharset()));
            response = new Response(Response.ACCEPTED, Response.EMPTY);
        }
        catch (IOException e) {
            response = new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return response;
    }


}
