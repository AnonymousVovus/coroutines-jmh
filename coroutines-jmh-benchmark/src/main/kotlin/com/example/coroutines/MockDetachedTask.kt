package com.example.coroutines


data class MockDetachedTask(
    val id: String,
    val parentId: String? = null,
    val initiator: Initiator,
    val request: Request,
) {

    sealed class Initiator {

        /**
         * Создателем является сам Владелец задания
         */
        object Holder : Initiator()

        /**
         * Создателем задания является bpm-процесс
         * Поэтому нам важно знать какой именно это процесс
         * @param processInstanceId идентификатор экземпляра процесса
         * @param processDefinitionId название бизнес процесса
         * @param processVersion номер версии. На какой версии процесса работает этот экземпляр
         * @param messageKey идентификатор сообщения, которое нужно отправить при завершении задания
         * @param businessKey дополнительно к [processInstanceId] может задаваться еще один идентификатор, для человека
         */
        data class BpmProcess(
            val processInstanceId: String,
            val processDefinitionId: String,
            val processVersion: Int,
            val messageKey: String,
            val businessKey: String? = null,
        ) : Initiator()


        companion object
    }


    /**
     * Параметры запроса
     * name - описание эмулируемого функционала
     * variables - входные переменные
     */
    data class Request(
        val name: String,
        val variables: Map<String, String> = mapOf()
    )
}
