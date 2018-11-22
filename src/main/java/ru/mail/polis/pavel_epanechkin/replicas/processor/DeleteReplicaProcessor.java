package ru.mail.polis.pavel_epanechkin;

import one.nio.http.Request;
import one.nio.http.Response;

import java.util.concurrent.Future;

public class PutReplicaProcessor extends ReplicaProcessor {

    private boolean removedFlag = false;

    public PutReplicaProcessor(ClusteredEntityService clusteredEntityService, EntityService entityService, int currentNodePort) {
        super(clusteredEntityService, entityService, currentNodePort);
    }

    @Override
    protected Future<Response> createReplicaRequest(String entityId, byte[] value, ClusterNode targetNode) {
        if (targetNode.getPort() == currentNodePort)
            return entityService.saveEntity(entityId, value);
        else
            return clusteredEntityService.sendReplicationRequest(targetNode, Request.METHOD_PUT, entityId, value);
    }

    @Override
    protected void handleResponse(Response response) {
        if (response != null && response.getStatus() == 201) {
            ackCount++;
        }
    }

}
