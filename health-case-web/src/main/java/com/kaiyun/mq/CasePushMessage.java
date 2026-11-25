package com.kaiyun.mq;

import lombok.Value;

@Value
class CasePushMessage {
    String taskId;
    String caseId;
}
