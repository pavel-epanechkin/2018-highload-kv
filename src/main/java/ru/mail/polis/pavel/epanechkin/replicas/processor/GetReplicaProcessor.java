package ru.mail.polis.pavel.epanechkin.replicas.processor;

import one.nio.http.Response;
import ru.mail.polis.pavel.epanechkin.ClusteredEntityService;
import ru.mail.polis.pavel.epanechkin.EntityService;

import java.util.concurrent.Executor;

public class GetReplicaProcessor extends ReplicaProcessor {

    private boolean removed = false;

    private byte[] resultObject = null;

    private long mostFreshTimestamp = 0;

    public GetReplicaProcessor(ClusteredEntityService clusteredEntityService, EntityService entityService, Executor executor, int currentNodePort) {
        super(clusteredEntityService, entityService, executor, currentNodePort);
    }

    @Override
    protected Response createLocalEntityRequest(String entityId, byte[] value) {
        return entityService.getEntity(entityId);
    }

    @Override
    protected void handleResponse(Response response) {
        if (response != null) {
            if (response.getStatus() == 200 || response.getStatus() == 404) {
                ackCount.incrementAndGet();
                if (response.getStatus() == 200) {
                    String timestamp = response.getHeader(EntityService.ENTITY_TIMESTAMP_HEADER);
                    Long objectTimestamp = new Long(timestamp);

                    synchronized (this) {
                        if (response.getHeader(EntityService.ENTITY_REMOVED_HEADER) != null)
                            removed = true;
                        else if (objectTimestamp > mostFreshTimestamp) {
                            mostFreshTimestamp = objectTimestamp;
                            resultObject = response.getBody();
                        }
                    }
                }
            }
        }
    }

    public boolean isRemoved() {
        return removed;
    }


    public byte[] getResultObject() {
        return resultObject;
    }

}
