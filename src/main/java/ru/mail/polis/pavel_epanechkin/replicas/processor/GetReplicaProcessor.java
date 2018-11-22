package ru.mail.polis.pavel_epanechkin;

import one.nio.http.Request;
import one.nio.http.Response;

import java.util.concurrent.Future;

public class GetReplicaProcessor extends ReplicaProcessor {

    private int ackCount = 0;
    private boolean removedFlag = false;
    private byte[] resultObject = null;
    private long mostFreshTimestamp = 0;

    public GetReplicaProcessor(ClusteredEntityService clusteredEntityService, EntityService entityService, int currentNodePort) {
        super(clusteredEntityService, entityService, currentNodePort);
    }

    @Override
    protected Future<Response> createReplicaRequest(String entityId, byte[] value, ClusterNode targetNode) {
        if (targetNode.getPort() == currentNodePort)
            return entityService.getEntity(entityId);
        else
            return clusteredEntityService.sendReplicationRequest(targetNode, Request.METHOD_GET, entityId, value);
    }

    @Override
    protected void handleResponse(Response response) {
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
    }

    public int getAckCount() {
        return ackCount;
    }

    public void setAckCount(int ackCount) {
        this.ackCount = ackCount;
    }

    public boolean isRemovedFlag() {
        return removedFlag;
    }

    public void setRemovedFlag(boolean removedFlag) {
        this.removedFlag = removedFlag;
    }

    public byte[] getResultObject() {
        return resultObject;
    }

    public void setResultObject(byte[] resultObject) {
        this.resultObject = resultObject;
    }
}
