package com.example.coroutines

import java.time.LocalDateTime


object DetachedTaskPayloadFactory {

    fun createPayloadByDetachedTask(
        task: MockDetachedTask,
        topic: String,
        partition: Int,
        offset: Long,
        taskHolderId: Int
    ) = MockDetachedTaskPayload(
        task,
        topic,
        partition,
        offset,
        MockDetachedTaskPayload.Holder(taskHolderId, LocalDateTime.now())
    )
}
