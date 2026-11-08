package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.json.PathStep

/**
 * A node's path: its parent's and one step more. The nodes of a document share their parents' steps rather than each
 * holding a copy of them: a document of 10 MB has a million nodes.
 */
internal class NodePath private constructor(
    private val parent: NodePath?,
    private val step: PathStep?,
    override val size: Int
) : AbstractList<PathStep>() {
    override fun get(index: Int): PathStep {
        if (index !in 0 until size) throw IndexOutOfBoundsException("Step $index of a path of $size")
        var path = this
        repeat(size - 1 - index) { path = checkNotNull(path.parent) }
        return checkNotNull(path.step)
    }

    // Walked up once, rather than once for each step.
    override fun iterator(): Iterator<PathStep> {
        val steps = ArrayList<PathStep>(size)
        var path = this
        while (true) {
            steps += path.step ?: break
            path = checkNotNull(path.parent)
        }
        steps.reverse()
        return steps.iterator()
    }

    /** This path and [step] after it. */
    operator fun plus(step: PathStep): NodePath = NodePath(this, step, size + 1)

    companion object {
        /** The root's path, which has no step. */
        val Root = NodePath(parent = null, step = null, size = 0)
    }
}

/**
 * What the nodes of one document share as it is read: each name once, its step once, and each place in an array
 * written once, where a document's reader has each of them as many times as it is written.
 */
internal class SharedParts {
    private val names = HashMap<String, String>()
    private val keys = HashMap<String, PathStep.Key>()
    private val places = ArrayList<String>()

    /** [name], the same one each time it is given. */
    fun name(name: String): String = names.getOrPut(name) { name }

    /** The step to the member named [name]. */
    fun key(name: String): PathStep.Key = keys.getOrPut(name) { PathStep.Key(name(name)) }

    /** The name of the item at [index] of an array: its place, written. */
    fun place(index: Int): String {
        while (places.size <= index) places += "${places.size}"
        return places[index]
    }

    /** [written], or [read] where the two are the same: a value written as it is read, kept once. */
    fun sameOrBoth(written: String, read: String): String = if (written == read) read else written
}
