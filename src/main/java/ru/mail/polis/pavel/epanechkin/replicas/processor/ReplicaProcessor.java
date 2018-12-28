package ru.mail.polis.pavel.epanechkin.replicas.processor;

import one.nio.http.Response;
import ru.mail.polis.pavel.epanechkin.*;
import ru.mail.polis.pavel_epanechkin.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ReplicaProcessor {

    protected AtomicInteger ackCount = new AtomicInteger(0);

    protected ClusteredEntityService clusteredEntityService;

    protected EntityService entityService;

    protected int currentNodePort;

    protected ExecutorCompletionService<Void> executorCompletionService;

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
        String keyHash = Utils.getSHA256(entityId.getBytes());
        List<ClusterNode> sortedNodes = clusteredEntityService.getNodesSortedByDistances(keyHash);

        for (int i = 1; i < replicationOptions.getFrom(); i++) {
            ClusterNode node = sortedNodes.get(i);
            executorCompletionService.submit(() -> {
                createReplicaRequestAndHandleResult(method, entityId, value, node);
            }, null);

        }
        createReplicaRequestAndHandleResult(method, entityId, value, sortedNodes.get(0));

        for (int i = 1; i < replicationOptions.getFrom(); i++) {
            executorCompletionService.take();
        }

    }

    private void createReplicaRequestAndHandleResult(int method, String entityId, byte[] value, ClusterNode targetNode) {
        Response response;

        if (targetNode.getPort() == currentNodePort)
            response = createLocalEntityRequest(entityId, value);
        else
            response = clusteredEntityService.sendReplicationRequest(targetNode, method, entityId, value);

        handleResponse(response);
    }


    public int getAckCount() {
        return ackCount.get();
    }
}
