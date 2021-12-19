package com.example.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

private val dispatcher = ForkJoinPool.commonPool().asCoroutineDispatcher()
private val scope = CoroutineScope(dispatcher)

fun main() = runBlocking(dispatcher) {

    val tasks = List(500) {
        MockDetachedTask(
            id = UUID.randomUUID().toString(),
            initiator = MockDetachedTask.Initiator.Holder,
            request = MockDetachedTask.Request(
                "task_$it"
            )
        )
    }

    var time = measureTimeMillis {
        create_task_payload_using_parallel_stream(tasks)
    }
    println("Parallel stream: $time")

    time = measureTimeMillis {
        create_task_payload_simple(tasks)
    }

    println("Linear: $time")

    time = measureTimeMillis {
        create_task_payload_completable_future(tasks)
    }

    println("Completable Future: $time")

    time = measureTimeMillis {
        create_task_payload_using_coroutines_default(tasks)
    }

    println("Default coroutines: $time")

    time = measureTimeMillis {
        create_task_payload_using_coroutines_concurrent_channel(tasks)
    }

    println("Concurrent channel: $time")

    time = measureTimeMillis {
        create_task_payload_using_coroutines_as_flow_emit(tasks)
    }

    println("Flow emitter: $time")
}

fun create_task_payload_using_parallel_stream(tasks: List<MockDetachedTask>) {

    tasks.parallelStream().map {
        DetachedTaskPayloadFactory.createPayloadByDetachedTask(
            task = it,
            topic = "parallelStreamTopic",
            partition = 1,
            offset = 1,
            taskHolderId = 123
        )
    }.collect(Collectors.toList())
}

fun create_task_payload_simple(tasks: List<MockDetachedTask>) {
    tasks.map {
        DetachedTaskPayloadFactory.createPayloadByDetachedTask(
            task = it,
            topic = "simpleTopic",
            partition = 1,
            offset = 1,
            taskHolderId = 123
        )
    }
}

fun create_task_payload_completable_future(tasks: List<MockDetachedTask>) {

    val futures = tasks.map {
        CompletableFuture.supplyAsync {
            DetachedTaskPayloadFactory.createPayloadByDetachedTask(
                task = it,
                topic = "simpleTopic",
                partition = 1,
                offset = 1,
                taskHolderId = 123
            )
        }
    }.toTypedArray()

    CompletableFuture.allOf(*futures).get()
}


suspend fun create_task_payload_using_coroutines_default(tasks: List<MockDetachedTask>) = withContext(dispatcher) {
    tasks.map {
        async {
            DetachedTaskPayloadFactory.createPayloadByDetachedTask(
                task = it,
                topic = "coroutineTopic",
                partition = 1,
                offset = 1,
                taskHolderId = 123
            )
        }
    }.awaitAll()
}

suspend fun create_task_payload_using_coroutines_concurrent_channel(
    tasks: List<MockDetachedTask>
) = withContext(dispatcher) {

    val channel = Channel<MockDetachedTask>()
    val parallelism = Runtime.getRuntime().availableProcessors()
    val atomicSize = AtomicInteger(0)
    val jobs1 = mutableListOf<Job>()

    val j = launch {
        tasks.map {
            jobs1.add(launch { channel.send(it) })
        }
    }

    val jobs = mutableListOf<Job>()

    (1..parallelism).forEach { _ ->
        jobs.add(launch {
            for (t in channel) {
                atomicSize.incrementAndGet()
                DetachedTaskPayloadFactory.createPayloadByDetachedTask(
                    task = t,
                    topic = "coroutineTopic",
                    partition = 1,
                    offset = 1,
                    taskHolderId = 123
                )
            }
        })
    }

    while (jobs1.size < tasks.size) {}
    jobs1.joinAll()
    j.join()
    channel.close()
    jobs.joinAll()
    println(atomicSize.get())
}

suspend fun create_task_payload_using_coroutines_as_flow_emit(
    tasks: List<MockDetachedTask>
) = withContext(dispatcher) {

    val flow = MutableSharedFlow<MockDetachedTask>()
    val atomicSize = AtomicInteger(0)

    val job = launch {
        flow.collect {
            atomicSize.incrementAndGet()
            DetachedTaskPayloadFactory.createPayloadByDetachedTask(
                task = it,
                topic = "coroutineTopic",
                partition = 1,
                offset = 1,
                taskHolderId = 123
            )
        }
    }

    launch {
        tasks.map { launch { flow.emit(it) } }
    }.join()

    while (atomicSize.get() < tasks.size) {}
    job.cancel()
    println(atomicSize.get())
}