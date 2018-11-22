package ru.mail.polis.pavel_epanechkin.replicas.processor;

import one.nio.http.Request;
import one.nio.http.Response;
import ru.mail.polis.pavel_epanechkin.ClusterNode;
import ru.mail.polis.pavel_epanechkin.ClusteredEntityService;
import ru.mail.polis.pavel_epanechkin.EntityService;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

public class PutReplicaProcessor extends ReplicaProcessor {

    public PutReplicaProcessor(ClusteredEntityService clusteredEntityService, EntityService entityService, Executor executor, int currentNodePort) {
        super(clusteredEntityService, entityService, executor, currentNodePort);
    }

    @Override
    protected Response createLocalEntityRequest(String entityId, byte[] value) {
        return entityService.saveEntity(entityId, value);
    }

    @Override
    protected void handleResponse(Response response) {
        if (response != null && response.getStatus() == 201) {
            ackCount++;
        }
    }

}
