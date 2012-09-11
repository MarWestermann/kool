package io.kool.stream.support

import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Queue
import io.kool.stream.*

/**
* Creates an [[Stream]] which puts each event into a [[List]] of a fixed size
*/
class TimeWindowStream<T>(val delegate: Stream<T>, val millis: Long) : Stream<List<T>>() {

    public override fun open(handler: Handler<List<T>>): Cursor {
        val newHandler: Handler<T> = TimeWindowHandler(handler, millis)
        val cursor = delegate.open(newHandler)
        newHandler.onOpen(cursor)
        return cursor
    }
}


/**
* Creates an [[Stream]] which puts the events into a moving window
*/
class TimeWindowHandler<T>(delegate: Handler<List<T>>, val millis: Long): DelegateHandler<T,List<T>>(delegate) {
    val queue: Queue<#(Long, T)> = ArrayDeque<#(Long, T)>()

    public override fun onNext(next: T) {
        val now = System.currentTimeMillis()
        val window = now - millis
        while (queue.notEmpty()) {
            val value = queue.peek()
            if (value != null && window > value._1) {
                queue.remove()
            } else {
                break
            }
        }
        queue.add(#(now, next))

        // now lets create an immutable copy to avoid any concurrent issues
        // TODO we may wish to use a more optimal fixed ReadOnlyList here
        val copy = ArrayList<T>(queue.size())
        for (t in queue) {
            copy.add(t._2)
        }
        delegate.onNext(copy)
    }
}
