package com.example.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.stream.Collectors
import kotlin.jvm.Throws


@State(Scope.Benchmark)
open class CreateDetachedTaskPayloadJmh {

    private val tasks = List(500) {
        MockDetachedTask(
            id = UUID.randomUUID().toString(),
            initiator = MockDetachedTask.Initiator.Holder,
            request = MockDetachedTask.Request(
                "task_$it"
            )
        )
    }

//    @Benchmark
//    @Throws(InterruptedException::class)
//    open fun create_task_payload_using_parallel_stream() {
//
//        tasks.parallelStream().map {
//            DetachedTaskPayloadFactory.createPayloadByDetachedTask(
//                task = it,
//                topic = "parallelStreamTopic",
//                partition = 1,
//                offset = 1,
//                taskHolderId = 123
//            )
//        }.collect(Collectors.toList())
//    }



    @Benchmark
    @Throws(InterruptedException::class)
    open fun create_task_payload_simple() {

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



    @Benchmark
    @Throws(InterruptedException::class)
    open fun create_task_payload_completable_future() {

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

    @Benchmark
    @Throws(InterruptedException::class)
    open fun create_task_payload_using_coroutines_default() = runBlocking(Dispatchers.Default) {

        runInterruptible {
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
            }
        }.awaitAll()

    }

    private val dispatcher = ForkJoinPool.commonPool().asCoroutineDispatcher()

    @Benchmark
    @Throws(InterruptedException::class)
    open fun create_task_payload_using_coroutines_concurrent_channel() = runBlocking(dispatcher) {

        val channel = Channel<MockDetachedTask>()
        val parallelism = Runtime.getRuntime().availableProcessors()

        val j = launch {
            tasks.asSequence().map { launch { channel.send(it) } }
        }

        val jobs = mutableListOf<Job>()

        (1..parallelism).forEach { _ ->
            jobs.add(launch {
                for (t in channel) {
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

        j.join()
        channel.close()
    }


    /*
    @Benchmark
    @Throws(InterruptedException::class)
    open fun create_task_payload_using_coroutines_as_flow() {

        runBlocking {

            //Flow take найти skip или offset
            tasks.asFlow().map {
                DetachedTaskPayloadFactory.createPayloadByDetachedTask(
                    task = it,
                    topic = "coroutineTopic",
                    partition = 1,
                    offset = 1,
                    taskHolderId = 123
                )
            }.toList()

        }

    }

    @Benchmark
    @Throws(InterruptedException::class)
    open fun create_task_payload_using_coroutines_as_flow_async() {

        runBlocking {

            //Flow take найти skip или offset
            tasks.asFlow().map {
                async {
                    DetachedTaskPayloadFactory.createPayloadByDetachedTask(
                        task = it,
                        topic = "coroutineTopic",
                        partition = 1,
                        offset = 1,
                        taskHolderId = 123
                    )
                }.await()
            }.toList()

        }

    }

    @Benchmark
    @Throws(InterruptedException::class)
    open fun create_task_payload_using_coroutines_as_flow_async_awaitAll() {

        runBlocking {

            //Flow take найти skip или offset
            tasks.asFlow().map {
                async {
                    DetachedTaskPayloadFactory.createPayloadByDetachedTask(
                        task = it,
                        topic = "coroutineTopic",
                        partition = 1,
                        offset = 1,
                        taskHolderId = 123
                    )
                }
            }.toList().awaitAll()

        }

    }
    */
}
