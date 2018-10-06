package pavel_epanechkin;

import one.nio.http.Response;
import ru.mail.polis.KVDao;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;

public class EntityService {

    private KVDao dao;

    public EntityService(KVDao dao) {
        this.dao = dao;
    }

    public Response getEntity(String id) {
        Response response;

        try {
            byte[] value = dao.get(id.getBytes(Charset.defaultCharset()));
            response = Response.ok(value);
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
            dao.remove(id.getBytes(Charset.defaultCharset()));
            response = new Response(Response.ACCEPTED, Response.EMPTY);
        }
        catch (IOException e) {
            response = new Response(Response.INTERNAL_ERROR, Response.EMPTY);
        }

        return response;
    }
}
