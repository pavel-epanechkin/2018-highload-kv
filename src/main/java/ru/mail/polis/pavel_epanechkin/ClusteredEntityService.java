package ru.mail.polis.pavel_epanechkin;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;
import ru.mail.polis.pavel_epanechkin.replicas.processor.DeleteReplicaProcessor;
import ru.mail.polis.pavel_epanechkin.replicas.processor.GetReplicaProcessor;
import ru.mail.polis.pavel_epanechkin.replicas.processor.PutReplicaProcessor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ClusteredEntityService {

    private EntityService entityService;

    private Set<String> topology;

    private ArrayList<ClusterNode> clusterNodes;

    private int currentPort;

    public static final String REPLICATION_HEADER = "X-Replication";

    private Executor executor;

    public ClusteredEntityService(KVDao dao, Set<String> topology, int port) {
        executor = Executors.newFixedThreadPool(topology.size());
        entityService = new EntityService(dao);
        this.topology = topology;
        this.currentPort = port;

        clusterNodes = new ArrayList<>();
        for (String topologyNode : topology)
            clusterNodes.add(new ClusterNode(topologyNode));
    }

    public Response getEntity(String id, String replicas, boolean replication) throws ExecutionException, InterruptedException {
        ReplicationOptions replicationOptions = getReplicationOptions(replicas);

        if (replicationOptions == null)
            return new Response(Response.BAD_REQUEST, Response.EMPTY);

        if (replication) {
            return entityService.getEntity(id);
        }
        else {
            GetReplicaProcessor replicaProcessor = new GetReplicaProcessor(this, entityService, executor, currentPort);
            replicaProcessor.getAndHandleReplicas(Request.METHOD_GET, id, null, replicationOptions);

            if (replicaProcessor.getAckCount() >= replicationOptions.getAck()) {
                if (replicaProcessor.isRemoved() || replicaProcessor.getResultObject() == null)
                    return new Response(Response.NOT_FOUND, Response.EMPTY);
                else {
                    return new Response(Response.OK, replicaProcessor.getResultObject());
                }
            }

            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    public Response saveEntity(String id, byte[] value, String replicas, boolean replication) throws ExecutionException, InterruptedException {
        ReplicationOptions replicationOptions = getReplicationOptions(replicas);

        if (replicationOptions == null)
            return new Response(Response.BAD_REQUEST, Response.EMPTY);

        if (replication) {
            return entityService.saveEntity(id, value);
        }
        else {
            PutReplicaProcessor replicaProcessor = new PutReplicaProcessor(this, entityService, executor, currentPort);
            replicaProcessor.getAndHandleReplicas(Request.METHOD_PUT, id, value, replicationOptions);

            if (replicaProcessor.getAckCount() >= replicationOptions.getAck()) {
                return new Response(Response.CREATED, Response.EMPTY);
            }

            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    public Response removeEntity(String id, String replicas, boolean replication) throws ExecutionException, InterruptedException {
        ReplicationOptions replicationOptions = getReplicationOptions(replicas);

        if (replicationOptions == null)
            return new Response(Response.BAD_REQUEST, Response.EMPTY);

        if (replication) {
            return entityService.removeEntity(id);
        }
        else {
            DeleteReplicaProcessor replicaProcessor = new DeleteReplicaProcessor(this, entityService, executor, currentPort);
            replicaProcessor.getAndHandleReplicas(Request.METHOD_DELETE, id, null, replicationOptions);

            if (replicaProcessor.getAckCount() >= replicationOptions.getAck()) {
                return new Response(Response.ACCEPTED, Response.EMPTY);
            }

            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    public Response sendReplicationRequest(ClusterNode clusterNode, int method, String id, byte[] body) {
        Request request = new Request(method, getReplicationRequestUrl(id), true);
        request.addHeader(REPLICATION_HEADER + ": true");

        if (body != null) {
            request.addHeader("Content-Length: " + body.length);
            request.setBody(body);
        }

        try {
            clusterNode.getHttpClient().setConnectTimeout(100);
            Response response = clusterNode.getHttpClient().invoke(request);
            return response;
        }
        catch (Exception err) {
            err.printStackTrace();
        }

        return null;
    }

    private String getReplicationRequestUrl(@NotNull final String id) {
        return "/v0/entity?id=" + id;
    }

    private ReplicationOptions getReplicationOptions(String replicas) {
        if (replicas == null)
            return getDefaultReplicationOptions();

        try {
            String[] tmp = replicas.split("/");
            int ack = new Integer(tmp[0]);
            int from = new Integer(tmp[1]);

            if (ack > 0 && ack <= from)
                return new ReplicationOptions(ack, from);
        }
        catch (NumberFormatException err) {
            //TODO: logging
        }

        return null;
    }

    private ReplicationOptions getDefaultReplicationOptions() {
        int from = topology.size();
        int rem = from % 2;
        long ack = rem == 0 ? from / 2 + 1 : Math.round(((double) from) / ((double) 2));

        return new ReplicationOptions(((int) ack), from);
    }

    public List<ClusterNode> getNodesSortedByDistances(String hash) {
        return clusterNodes.stream()
                .sorted(Comparator.comparingInt(node -> node.getDistance(hash)))
                .collect(Collectors.toList());
    }
}
