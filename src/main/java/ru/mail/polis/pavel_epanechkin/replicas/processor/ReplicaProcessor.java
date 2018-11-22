package ru.mail.polis.pavel_epanechkin.replicas.processor;

import one.nio.http.Request;
import one.nio.http.Response;
import ru.mail.polis.pavel_epanechkin.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

public abstract class ReplicaProcessor {

    protected int ackCount = 0;

    protected ClusteredEntityService clusteredEntityService;

    protected EntityService entityService;

    protected int currentNodePort;

    protected ExecutorCompletionService<Response> executorCompletionService;

    protected abstract Response createLocalEntityRequest(String entityId, byte[] value);

    protected abstract void handleResponse(Response response);

    public ReplicaProcessor(ClusteredEntityService clusteredEntityService, EntityService entityService, Executor executor, int currentNodePort) {
        this.clusteredEntityService = clusteredEntityService;
        this.entityService = entityService;
        this.currentNodePort = currentNodePort;
        this.executorCompletionService = new ExecutorCompletionService<>(executor);
    }

    public void getAndHandleReplicas(int method, String entityId, byte[] value,
                                     ReplicationOptions replicationOptions) throws ExecutionException, InterruptedException {
        String keyHash = Utils.getMD5(entityId.getBytes());
        List<ClusterNode> sortedNodes = clusteredEntityService.getNodesSortedByDistances(keyHash);

        for (int i = 0; i < replicationOptions.getFrom(); i++) {
            createReplicaRequest(method, entityId, value, sortedNodes.get(i));
        }

        for (int i = 0; i < replicationOptions.getFrom(); i++) {
            handleResponse(executorCompletionService.take().get());
        }

    }

    private Future<Response> createReplicaRequest(int method, String entityId, byte[] value, ClusterNode targetNode) {
        return executorCompletionService.submit(() -> {
            if (targetNode.getPort() == currentNodePort)
                return createLocalEntityRequest(entityId, value);
            else
                return clusteredEntityService.sendReplicationRequest(targetNode, method, entityId, value);
        });
    }

    public int getAckCount() {
        return ackCount;
    }
}
