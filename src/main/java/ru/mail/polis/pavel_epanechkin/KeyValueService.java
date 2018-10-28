package ru.mail.polis.pavel_epanechkin;


import one.nio.http.*;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;
import ru.mail.polis.KVService;

import java.io.IOException;
import java.util.Set;

public class KeyValueService extends HttpServer implements KVService {

    private ReplicationService replicationService;

    private KVDao dao;

    public KeyValueService(final int port, @NotNull final KVDao dao, Set<String> topology) throws IOException {
        super(HttpServerConfigFactory.create(port));
        replicationService = new ReplicationService(dao, topology, port);
        this.dao = dao;
    }

    @Path("/v0/status")
    public Response handleStatusRequest(Request request) throws IOException {
        return Response.ok("OK");
    }

    @Path("/v0/entity")
    public Response handleEntityRequest(Request request, @Param("id") String id, @Param("replicas") String replicas) throws IOException {
        if (id == null || id.isEmpty())
            return new Response(Response.BAD_REQUEST, Response.EMPTY);

        if (replicas != null && replicas.isEmpty())
            return new Response(Response.BAD_REQUEST, Response.EMPTY);

        String replicationHeader = request.getHeader(ReplicationService.REPLICATION_HEADER);
        boolean isReplication = replicationHeader != null;

        switch (request.getMethod()) {
            case Request.METHOD_GET:
                return replicationService.getEntity(id, replicas, isReplication);
            case Request.METHOD_PUT:
                return replicationService.saveEntity(id, request.getBody(), replicas, isReplication);
            case Request.METHOD_DELETE:
                return replicationService.removeEntity(id, replicas, isReplication);
            default:
                return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
        }
    }

    @Override
    public void handleDefault(Request request, HttpSession session) throws IOException {
        Response response = new Response(Response.BAD_REQUEST, Response.EMPTY);
        session.sendResponse(response);
    }
}
