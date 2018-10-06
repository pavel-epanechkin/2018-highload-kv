package pavel_epanechkin;


import one.nio.http.*;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;
import ru.mail.polis.KVService;

import java.io.IOException;

public class KeyValueService extends HttpServer implements KVService {

    private EntityService entityService;

    public KeyValueService(final int port, @NotNull final KVDao dao) throws IOException {
        super(HttpServerConfigFactory.create(port));
        entityService = new EntityService(dao);
    }

    @Path("/v0/status")
    public Response handleStatusRequest(Request request) throws IOException {
        return Response.ok("OK");
    }

    @Path("/v0/entity")
    public Response handleEntityRequest(Request request, @Param("id") String id) throws IOException {
        if (id == null || id.isEmpty())
            return new Response(Response.BAD_REQUEST, Response.EMPTY);

        switch (request.getMethod()) {
            case Request.METHOD_GET:
                return entityService.getEntity(id);
            case Request.METHOD_PUT:
                return entityService.saveEntity(id, request.getBody());
            case Request.METHOD_DELETE:
                return entityService.removeEntity(id);
            default:
                return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
        }
    }

}
