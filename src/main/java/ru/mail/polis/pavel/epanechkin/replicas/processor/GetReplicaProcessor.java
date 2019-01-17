package ru.mail.polis.pavel.epanechkin.replicas.processor;

import one.nio.http.Response;
import ru.mail.polis.pavel.epanechkin.ClusteredEntityService;
import ru.mail.polis.pavel.epanechkin.EntityService;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

public class GetReplicaProcessor extends ReplicaProcessor {

    private AtomicBoolean removed = new AtomicBoolean(false);

    private AtomicLong mostFreshTimestamp = new AtomicLong(0);

    private byte[] resultObject = null;

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
                    long objectTimestamp = Long.parseLong(timestamp);

                    if (response.getHeader(EntityService.ENTITY_REMOVED_HEADER) != null)
                        removed.set(true);
                    else {
                        long oldMostFreshTimestamp = mostFreshTimestamp
                                .getAndUpdate(t -> t < objectTimestamp ? objectTimestamp : t);
                        if (oldMostFreshTimestamp < objectTimestamp)
                            resultObject = response.getBody();
                    }

                }
            }
        }
    }

    public boolean isRemoved() {
        return removed.get();
    }


    public byte[] getResultObject() {
        return resultObject;
    }

}
