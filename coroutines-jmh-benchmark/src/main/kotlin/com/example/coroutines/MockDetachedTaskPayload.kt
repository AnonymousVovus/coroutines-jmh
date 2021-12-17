package com.example.coroutines

import java.time.LocalDateTime


data class MockDetachedTaskPayload(
    val task: MockDetachedTask,

    val topic: String,
    val partition: Int,
    val offset: Long,

    val holder: Holder,
    val executor: Executor = Executor.Holder,

    var state: State = State.ACQUIRED,
    var outcome: Outcome = Outcome.None
) {


    /**
     * Владелец задания. Информация о том обработчике, который захватил задание на себя
     * @param id идентификатор владельца задания. Тот кто захватил задание на себя
     * @param acquiredTs момент времени, когда обработчик захватил задание на себя
     */
    data class Holder(
        val id: Int,
        val acquiredTs: LocalDateTime
    )


    /**
     * Состояние задания
     */
    enum class State {

        /**
         * Задание захватил на себя [Holder] владелец
         */
        ACQUIRED,

        /**
         * Задание выполняет [Executor] исполнитель
         */
        IN_PROGRESS,

        /**
         * Задание выполнено
         */
        DONE,
    }


    /**
     * Исполнитель задания
     */
    sealed class Executor {

        /**
         * Исполнителем задания является сам Владелец задания
         */
        object Holder: Executor()


        /**
         * Исполнителем задания является bpm-процесс
         * Поэтому нам важно знать какой именно это процесс
         */
        data class BpmProcess(
            val processInstanceId: String,
            val processDefinitionId: String,
            val processVersion: Int,
            val businessKey: String? = null,
        ): Executor()
    }


    /**
     * Результат выполнения задания.
     * * [None] у задания нет никакого результата
     * * [Success] MockDetachedTask.Request.variables будут прокинуты в MockDetachedTaskPayload.Outcome.variables
     * * [Failure] результат с деталями ошибки в message
     */
    sealed class Outcome {

        object None : Outcome()

        data class Success(
            val variables: Map<String, String>,
            val doneTs: LocalDateTime
        ): Outcome()

        data class Failure(val message: String): Outcome()
    }
}
