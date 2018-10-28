package ru.mail.polis.pavel_epanechkin;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReplicationService {

    private EntityService entityService;

    private Set<String> topology;

    private ArrayList<ClusterNode> clusterNodes;

    private int currentPort;

    public static final String REPLICATION_HEADER = "X-Replication";

    public ReplicationService(KVDao dao, Set<String> topology, int port) {
        entityService = new EntityService(dao);
        this.topology = topology;
        this.currentPort = port;

        clusterNodes = new ArrayList<>();
        for (String topologyNode : topology)
            clusterNodes.add(new ClusterNode(topologyNode));
    }

    public Response getEntity(String id, String replicas, boolean replication) {
        Replicas replicasObj = getReplicas(replicas);

        if (replicasObj == null)
            return new Response(Response.BAD_REQUEST, Response.EMPTY);

        if (replication) {
            return entityService.getEntity(id);
        }
        else {
            int ackCount = 0;
            boolean removedFlag = false;
            byte[] resultObject = null;
            long mostFreshTimestamp = 0;
            String keyHash = Utils.getMD5(id.getBytes());
            List<ClusterNode> sortedNodes = getNodesSortedByDistances(keyHash);
            for (int i = 0; i < replicasObj.getFrom(); i++) {
                Response response;

                if (sortedNodes.get(i).getPort() == currentPort)
                    response = entityService.getEntity(id);
                else
                    response = sendReplicationRequest(sortedNodes.get(i), Request.METHOD_GET, id, null);

                if (response != null) {
                    if (response.getStatus() == 200 || response.getStatus() == 404) {
                        ackCount++;
                        if (response.getStatus() == 200) {
                            String timestamp = response.getHeader(EntityService.ENTITY_TIMESTAMP_HEADER);
                            Long objectTimestamp = new Long(timestamp);

                            if (response.getHeader(EntityService.ENTITY_REMOVED_HEADER) != null)
                                removedFlag = true;
                            else if (objectTimestamp > mostFreshTimestamp) {
                                mostFreshTimestamp = objectTimestamp;
                                resultObject = response.getBody();
                            }
                        }
                    }
                }
                if (ackCount >= replicasObj.getAck()) {
                    if (removedFlag || resultObject == null)
                        return new Response(Response.NOT_FOUND, Response.EMPTY);
                    else {
                        return new Response(Response.OK, resultObject);
                    }
                }
            }
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    public Response saveEntity(String id, byte[] value, String replicas, boolean replication) {
        Replicas replicasObj = getReplicas(replicas);

        if (replicasObj == null)
            return new Response(Response.BAD_REQUEST, Response.EMPTY);

        if (replication) {
            return entityService.saveEntity(id, value);
        }
        else {
            int ackCount = 0;
            String keyHash = Utils.getMD5(id.getBytes());
            List<ClusterNode> sortedNodes = getNodesSortedByDistances(keyHash);
            for (int i = 0; i < replicasObj.getFrom(); i++) {
                Response response;

                if (sortedNodes.get(i).getPort() == currentPort)
                    response = entityService.saveEntity(id, value);
                else
                    response = sendReplicationRequest(sortedNodes.get(i), Request.METHOD_PUT, id, value);

                if (response != null && response.getStatus() == 201) {
                    ackCount++;
                }
            }
            if (ackCount >= replicasObj.getAck()) {
                return new Response(Response.CREATED, Response.EMPTY);
            }

            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    public Response removeEntity(String id, String replicas, boolean replication) {
        Replicas replicasObj = getReplicas(replicas);

        if (replicasObj == null)
            return new Response(Response.BAD_REQUEST, Response.EMPTY);

        if (replication) {
            return entityService.removeEntity(id);
        }
        else {
            int ackCount = 0;
            String keyHash = Utils.getMD5(id.getBytes());
            List<ClusterNode> sortedNodes = getNodesSortedByDistances(keyHash);
            for (int i = 0; i < replicasObj.getFrom(); i++) {
                Response response;
                if (sortedNodes.get(i).getPort() == currentPort)
                    response = entityService.removeEntity(id);
                else
                    response = sendReplicationRequest(sortedNodes.get(i), Request.METHOD_DELETE, id, null);

                if (response != null && response.getStatus() == 202) {
                    ackCount++;
                }
            }
            if (ackCount >= replicasObj.getAck()) {
                return new Response(Response.ACCEPTED, Response.EMPTY);
            }

            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }

    private Response sendReplicationRequest(ClusterNode clusterNode, int method, String id, byte[] body) {
        Request request = new Request(method, getReplicationRequestUrl(id), true);
        request.addHeader(REPLICATION_HEADER + ": true");

        if (body != null) {
            request.addHeader("Content-Length: " + body.length);
            request.setBody(body);
        }

        try {
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

    private Replicas getReplicas(String replicas) {
        if (replicas == null)
            return getDefaultReplicas();

        try {
            String[] tmp = replicas.split("/");
            int ack = new Integer(tmp[0]);
            int from = new Integer(tmp[1]);

            if (ack > 0 && ack <= from)
                return new Replicas(ack, from);
        }
        catch (NumberFormatException err) {
            //TODO: logging
        }

        return null;
    }

    private Replicas getDefaultReplicas() {
        int from = topology.size();
        int rem = from % 2;
        long ack = rem == 0 ? from / 2 + 1 : Math.round(((double) from) / ((double) 2));

        return new Replicas(((int) ack), from);
    }

    private List<ClusterNode> getNodesSortedByDistances(String hash) {
        return clusterNodes.stream()
                .sorted(Comparator.comparingInt(node -> node.getDistance(hash)))
                .collect(Collectors.toList());
    }
}
