package com.share.external.lib.mvvm.navigation.stack

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import com.share.external.foundation.collections.DoublyLinkedMap
import com.share.external.foundation.collections.doublyLinkedMapOf
import com.share.external.foundation.collections.removeLast
import com.share.external.foundation.coroutines.MainImmediateScope
import com.share.external.foundation.coroutines.ManagedCoroutineScope
import com.share.external.lib.mvvm.navigation.content.NavigationKey
import com.share.external.lib.mvvm.navigation.lifecycle.ViewLifecycleScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Concrete, mutable navigation stack that manages [ViewModelStoreContentProvider] instances keyed by [NavigationKey].
 *
 * Backed by a [DoublyLinkedMap], this stack integrates tightly with Compose to drive screen transitions,
 * modal overlays, and scoped lifecycle management.
 *
 * ### Features:
 * - State is exposed via [stack], a [mutableStateOf] snapshot-backed map to trigger Compose recomposition.
 * - Supports nested transaction batching via [transactionRefCount] to defer recomposition and lifecycle effects.
 * - Pushes views onto the stack with [push], removing and cancelling existing entries with matching keys.
 * - Provides [pop], [popTo], and [removeAll] operations for typical back stack navigation.
 * - Defers cancellation logic (e.g., ViewModel teardown) until the transaction is complete using [transactionFinished].
 * - Uses [ViewLifecycleScope] to automatically remove entries when their lifecycle completes.
 *
 * @param V The view type managed by the stack.
 * @param rootScope The parent coroutine scope for all views in the stack.
 * @param initialStack Optional lambda to prepopulate the stack in a single transaction.
 *
 * @see ViewModelStoreContentProvider
 * @see NavigationKey
 * @see transaction
 */
@Stable
open class ViewModelNavigationStack<V>(
    private val rootScope: ManagedCoroutineScope,
    initialStack: (NavigationStack<V>) -> Unit = {},
) : NavigationBackStack {
    private val providers = doublyLinkedMapOf<NavigationKey, ViewModelStoreContentProvider<V>>()

    var stack: DoublyLinkedMap<NavigationKey, ViewModelStoreContentProvider<V>> by
        mutableStateOf(value = providers, policy = neverEqualPolicy())
        private set

    override val size: Int by derivedStateOf { stack.size }

    protected val last by derivedStateOf { stack.values.lastOrNull() }

    private var shouldUpdateState: Boolean = false
    private var transactionRefCount: Int = 0
    private val transactionFinished: MutableList<() -> Unit> = mutableListOf()

    init {
        transaction {
            initialStack(rootNavigationScope())
        }

        rootScope.invokeOnCompletion {
            // Parent scope could complete off main thread.
            MainImmediateScope().launch { removeAll() }
        }
    }

    fun rootNavigationScope(): NavigationStackScope<V> = NavigationStackContext(scope = rootScope, stack = this)

    internal fun push(key: NavigationKey, content: () -> V, scope: ViewLifecycleScope) {
        if (!rootScope.isActive || !scope.isActive) {
            Timber.tag(TAG).wtf("Scope is not active pushing $key, $content onto nav stack: $this")
            return
        }
        if (providers.keys.lastOrNull() == key) {
            return
        }
        val previous = providers[key]
        val provider = ViewModelStoreContentProviderImpl(view = content, scope = scope)
        providers[key] = provider

        transactionFinished.add {
            previous?.cancel(awaitChildrenComplete = true, message = "Pushed new content for key: $key")

            scope.invokeOnCompletion { MainImmediateScope().launch { remove(key) } }
        }

        // run lazy view after provider created and set in position to avoid out of order entries from a recursive call
        provider.view

        updateState()
    }

    override fun pop(): Boolean {
        return providers.removeLast()?.run {
            transactionFinished.add {
                cancel(awaitChildrenComplete = true, message = "Popped from back stack")
            }
            updateState()
        } != null
    }

    override fun popTo(key: NavigationKey, inclusive: Boolean): Boolean {
        val removed = providers.removeAllAfter(key, inclusive)
        return if (removed.isNotEmpty()) {
            transactionFinished.add {
                removed.asReversed().forEach {
                    it.cancel(
                        awaitChildrenComplete = true,
                        message = "Popped from back stack to: $key inclusive: $inclusive",
                    )
                }
            }
            updateState()
            true
        } else false
    }

    override fun removeAll() {
        providers.keys.firstOrNull()?.let { popTo(key = it, inclusive = true) }
    }

    override fun remove(key: NavigationKey) {
        providers.remove(key)?.run {
            transactionFinished.add {
                cancel(awaitChildrenComplete = true, message = "Removed from back stack")
            }
            updateState()
        }
    }

    final override fun transaction(block: () -> Unit) {
        transactionRefCount += 1
        try {
            block()
        } finally {
            transactionRefCount -= 1
            if (shouldUpdateState && transactionRefCount == 0) {
                shouldUpdateState = false
                updateState()
            }
        }
    }

    private fun updateState() {
        if (transactionRefCount > 0) {
            shouldUpdateState = true
        } else {
            stack = providers
            transactionFinished.forEach { it() }
            transactionFinished.clear()
        }
    }

    companion object {
        const val TAG = "ViewModelNavigationStack"
    }
}

fun <V> Map<NavigationKey, ViewModelStoreContentProvider<V>>.logEntries(
    analyticsId: String,
    tag: String,
    metadata: (ViewModelStoreContentProvider<V>) -> String? = { null },
) {
    Timber.tag(tag)
        .d(
            "%s",
            object {
                override fun toString(): String = buildString {
                    append("Backstack $analyticsId[")
                    entries.forEachIndexed { i, (key, provider) ->
                        append("{${key.analyticsId}")
                        metadata(provider)?.let { append(": $it") }
                        append("}")
                        if (i < size - 1) append(" ⇨ ")
                    }
                    append("]")
                }
            },
        )
}
