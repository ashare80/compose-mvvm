package com.share.external.foundation.coroutines

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import timber.log.Timber

private const val TAG = "CoroutineScopeExt"

/**
 * Creates a child [CoroutineScope] from the calling [CoroutineScope], using a standard [Job] as the parent job. This
 * means any failure in the child scope will cancel all children of that scope, propagating up as part of structured
 * concurrency.
 *
 * This is useful if you want strict failure handling, where one failing child cancels its sibling coroutines in the
 * same scope.
 *
 * @param name A human-readable label appended to the parent's [CoroutineName], if present.
 * @param context Additional [CoroutineContext] elements, defaulting to [EmptyCoroutineContext].
 * @return A new [CoroutineScope] that inherits its parent job and context, plus a new child [Job].
 */
fun CoroutineScope.childJobScope(name: String = "", context: CoroutineContext = EmptyCoroutineContext) =
    childScope(name = name, context = context, childJob = { Job(parent = it) })

/**
 * Creates a child [CoroutineScope] from the calling [CoroutineScope], using a [SupervisorJob] as the parent job. With a
 * supervisor job, a failure in one child does not automatically cancel other children of the same supervisor scope.
 *
 * This is useful for scenarios where you want independent error handling among sibling coroutines, allowing them to
 * continue running even if one fails.
 *
 * @param name A human-readable label appended to the parent's [CoroutineName], if present.
 * @param context Additional [CoroutineContext] elements, defaulting to [EmptyCoroutineContext].
 * @return A new [CoroutineScope] that inherits its parent job and context, plus a new child [SupervisorJob].
 */
fun CoroutineScope.childSupervisorJobScope(name: String = "", context: CoroutineContext = EmptyCoroutineContext) =
    childScope(name = name, context = context, childJob = { SupervisorJob(parent = it) })

/**
 * Internal helper function for creating a child [CoroutineScope]. It merges a new [Job] (provided by [childJob]) with
 * the caller's existing context ([this.coroutineContext]), plus an optional [CoroutineContext].
 *
 * It also sets up a [CoroutineName] that appends [name] to the parent's coroutine name (if any), for improved logging
 * and debugging.
 *
 * @param name A human-readable label appended to the parent's [CoroutineName].
 * @param context Additional [CoroutineContext] elements, defaulting to [EmptyCoroutineContext].
 * @param childJob A function that creates either a [Job] or [SupervisorJob], given the parent job.
 * @return A new child [CoroutineScope] incorporating the newly created job and name.
 */
private fun CoroutineScope.childScope(
    name: String,
    context: CoroutineContext = EmptyCoroutineContext,
    childJob: (Job) -> Job,
): CoroutineScope {
    val job = childJob(coroutineContext.job)
    val parentName =
        (coroutineContext[CoroutineName.Key]?.name ?: "").ifBlank {
            "parent(${Integer.toHexString(System.identityHashCode(coroutineContext.job))})"
        }
    val childName = "$parentName⇨$name(${Integer.toHexString(System.identityHashCode(job))})"

    job.invokeOnCompletion { error ->
        if (error is CancellationException) {
            Timber.tag(TAG).d(message = "Child scope completed: %s, cancellation: %S", childName, error.message)
        } else {
            Timber.tag(TAG).d(t = error, message = "Child scope completed with error: %s", childName)
        }
    }

    return CoroutineScope(context + job + CoroutineName(childName))
}
