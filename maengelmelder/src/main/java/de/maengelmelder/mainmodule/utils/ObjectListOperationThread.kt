package de.maengelmelder.mainmodule.utils

/**
 * Iteration of a list in a thread
 */
abstract class ObjectListOperationThread<T>(list: List<T>): Thread() {

    /**
     * Whether the iteration should be stopped or not
     */
    private var bShouldStop = false

    /**
     * list of the objects to be iterated
     */
    private val mList = list

    /**
     * Sets [bShouldStop] to true
     */
    fun stopThread() {
        bShouldStop = true
    }

    /**
     * Starts the thread
     */
    override fun start() {
        bShouldStop = false
        super.start()
    }

    override fun run() {
        mList.forEach { item ->
            if (bShouldStop) return@forEach
            forEveryObject(item)
        }
    }

    /**
     * Should be inherited by the inheriting class. Defines the operation done by each object in the list
     */
    abstract fun forEveryObject(item: T)
}