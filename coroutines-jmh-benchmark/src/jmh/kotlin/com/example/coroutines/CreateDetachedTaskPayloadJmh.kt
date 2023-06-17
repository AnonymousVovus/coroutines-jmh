package com.example.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.openjdk.jmh.annotations.*
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors


@State(Scope.Benchmark)
open class CreateDetachedTaskPayloadJmh {

//    @Setup
//    fun setUp() {
//    }

//    @TearDown
//    fun end() {
//    }

    private val tasks = List(500) {
        MockDetachedTask(
            id = UUID.randomUUID().toString(),
            initiator = MockDetachedTask.Initiator.Holder,
            request = MockDetachedTask.Request(
                "task_$it"
            )
        )
    }

    @Benchmark
    @Throws(InterruptedException::class)
    open fun mode_A_payload_simple() {
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
    open fun mode_B_payload_using_parallel_stream() {
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

    @Benchmark
    @Throws(InterruptedException::class)
    open fun mode_C_payload_completable_future() {
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
    open fun mode_D_payload_using_coroutines_default() = runBlocking {
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

    @Benchmark
    @Throws(InterruptedException::class)
    open fun mode_E_payload_using_coroutines_parallel() {
        tasks.forEachParallel {
            DetachedTaskPayloadFactory.createPayloadByDetachedTask(
                task = it,
                topic = "coroutineTopic",
                partition = 1,
                offset = 1,
                taskHolderId = 123
            )
        }
    }

//    @Benchmark
//    @Throws(InterruptedException::class)
//    open fun mode_F_A_payload_using_windowed_tasks_coroutines_100(
//    ) {
//       runParametrized(100)
//    }

    @Benchmark
    @Throws(InterruptedException::class)
    open fun mode_F_B_payload_using_windowed_tasks_coroutines_50(
    ) {
       runParametrized(50)
    }

//    @Benchmark
//    @Throws(InterruptedException::class)
//    open fun mode_F_C_payload_using_windowed_tasks_coroutines_25(
//    ) {
//       runParametrized(25)
//    }

    @Benchmark
    @Throws(InterruptedException::class)
    open fun mode_G_payload_using_buffered_flow() = runBlocking {
        tasks.asFlow().buffer(100).collect {
            DetachedTaskPayloadFactory.createPayloadByDetachedTask(
                task = it,
                topic = "coroutineTopic",
                partition = 1,
                offset = 1,
                taskHolderId = 123
            )
        }
    }

    @Benchmark
    @Throws(InterruptedException::class)
    open fun mode_Z_payload_using_correct_buffered_flow() = runBlocking {
        coroutineScope {
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
            }.buffer(100).map { it.await() }.collect {
                val s = it.partition
                s + s
            }
        }
    }

    @Benchmark
    @Throws(InterruptedException::class)
    open fun mode_H_payload_using_buffered_flow_2() = runBlocking {

        val flow = flow {
            tasks.map { emit(it) }
        }.flowOn(Dispatchers.IO)

        val scope = CoroutineScope(Dispatchers.Default)

        val job = scope.launch {
            flow.map {
                DetachedTaskPayloadFactory.createPayloadByDetachedTask(
                    task = it,
                    topic = "coroutineTopic",
                    partition = 1,
                    offset = 1,
                    taskHolderId = 123
                )
            }.buffer(100).collect {
                it.partition + it.offset
            }
        }

        job.join()
    }

    private fun runParametrized(windowSize: Int) = runBlocking {
        tasks.windowed(windowSize, windowSize, true).map { window ->
            async {
                window.map {
                    DetachedTaskPayloadFactory.createPayloadByDetachedTask(
                        task = it,
                        topic = "coroutineTopic",
                        partition = 1,
                        offset = 1,
                        taskHolderId = 123
                    )
                }
            }
        }.awaitAll().flatten()
    }

    private fun <A> Collection<A>.forEachParallel(f: suspend (A) -> Unit): Unit = runBlocking {
        map { async { f(it) } }.awaitAll()
    }

    private fun <A> Collection<A>.forEachParallelWindowed(
        parallelism: Int,
        f: suspend (A) -> Unit
    ): Unit = runBlocking {
        val window = size / parallelism
        windowed(window, window, true).map { list -> async { list.map { f(it) } } }.awaitAll().flatten()
    }
}
