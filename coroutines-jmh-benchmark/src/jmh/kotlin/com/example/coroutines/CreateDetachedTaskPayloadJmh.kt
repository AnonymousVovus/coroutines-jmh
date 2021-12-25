package com.example.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.openjdk.jmh.annotations.*
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors
import kotlin.jvm.Throws


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

    private fun <A> Collection<A>.forEachParallel(f: suspend (A) -> Unit): Unit = runBlocking {
        map { async { f(it) } }.awaitAll()
    }
}
