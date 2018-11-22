package ru.mail.polis.pavel_epanechkin.replicas.processor;

import one.nio.http.Request;
import one.nio.http.Response;
import ru.mail.polis.pavel_epanechkin.ClusterNode;
import ru.mail.polis.pavel_epanechkin.ClusteredEntityService;
import ru.mail.polis.pavel_epanechkin.EntityService;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

public class DeleteReplicaProcessor extends ReplicaProcessor {

    public DeleteReplicaProcessor(ClusteredEntityService clusteredEntityService, EntityService entityService, Executor executor, int currentNodePort) {
        super(clusteredEntityService, entityService, executor, currentNodePort);
    }

    @Override
    protected Response createLocalEntityRequest(String entityId, byte[] value) {
        return entityService.removeEntity(entityId);
    }

    @Override
    protected void handleResponse(Response response) {
        if (response != null && response.getStatus() == 202) {
            ackCount++;
        }
    }

}
