package com.example.coroutines

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors


@State(Scope.Benchmark)
open class CreateDetachedTaskPayloadJmh {

    @Setup
    fun setUp() {
        corCounter.set(0)
    }

    @TearDown
    fun end() {
        print("Count: ${corCounter.get()} ")
    }

    private val corCounter = AtomicLong(0)

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
        corCounter.set(0)
        tasks.map {
            corCounter.incrementAndGet()
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
        corCounter.set(0)
        tasks.parallelStream().map {
            corCounter.incrementAndGet()
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
        corCounter.set(0)
        val futures = tasks.map {
            CompletableFuture.supplyAsync {
                corCounter.incrementAndGet()
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
        corCounter.set(0)
        tasks.map {
            async {
                corCounter.incrementAndGet()
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
        corCounter.set(0)
        tasks.forEachParallel {
            corCounter.incrementAndGet()
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
    open fun mode_F_A_payload_using_windowed_tasks_coroutines_100(
    ) {
       runParametrized(100)
    }

    @Benchmark
    @Throws(InterruptedException::class)
    open fun mode_F_B_payload_using_windowed_tasks_coroutines_50(
    ) {
       runParametrized(50)
    }

    @Benchmark
    @Throws(InterruptedException::class)
    open fun mode_F_C_payload_using_windowed_tasks_coroutines_25(
    ) {
       runParametrized(25)
    }

    @Benchmark
    @Throws(InterruptedException::class)
    open fun mode_G_payload_using_buffered_flow() = runBlocking {
        corCounter.set(0)
        tasks.asFlow().buffer(100).collect {
            corCounter.incrementAndGet()
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
    open fun mode_H_payload_using_buffered_flow_2() = runBlocking {
        corCounter.set(0)
        val flow = flow {
            tasks.map { emit(it) }
        }

        flow.buffer(100).collect {
            corCounter.incrementAndGet()
            DetachedTaskPayloadFactory.createPayloadByDetachedTask(
                task = it,
                topic = "coroutineTopic",
                partition = 1,
                offset = 1,
                taskHolderId = 123
            )
        }
    }

    private fun runParametrized(windowSize: Int) = runBlocking {
        corCounter.set(0)
        tasks.windowed(windowSize, windowSize, true).map { window ->
            async {
                window.map {
                    corCounter.incrementAndGet()
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
