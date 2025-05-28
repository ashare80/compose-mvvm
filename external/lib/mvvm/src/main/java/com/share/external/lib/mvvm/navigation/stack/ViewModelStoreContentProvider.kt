package com.share.external.lib.mvvm.navigation.stack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.share.external.foundation.coroutines.ManagedCoroutineScope
import com.share.external.lib.mvvm.navigation.lifecycle.DefaultViewModelStoreOwner
import com.share.external.lib.mvvm.navigation.lifecycle.LocalOwnersProvider
import com.share.external.lib.mvvm.navigation.lifecycle.ObserveViewVisibility
import com.share.external.lib.mvvm.navigation.lifecycle.ViewLifecycleScope

/**
 * A container that bridges a [view] and its associated [ViewModelStore], [CoroutineScope], and [SaveableStateHolder]
 * into a reusable composition host.
 *
 * This interface ensures that each screen or modal view receives correct lifecycle ownership and state management
 * support within a Compose navigation context.
 *
 * Implementations must ensure:
 * - A retained [CoroutineScope] for view-related logic.
 * - Correct scoping of [ViewModelStoreOwner] and [SaveableStateHolder] via [LocalOwnersProvider].
 *
 * @param V The view type being hosted.
 */
interface ViewModelStoreContentProvider<V> : ManagedCoroutineScope {
    /** The view instance that this provider is managing. */
    val view: V

    /**
     * Provides the necessary local owners ([ViewModelStoreOwner], [SaveableStateHolder], etc.) and lifecycle visibility
     * context for [view] composition.
     *
     * This should be called from within a composable scope to wrap the [view]'s UI logic.
     *
     * @param saveableStateHolder The state holder used to preserve UI state across recompositions.
     * @param content The composable content representing the [view]'s UI.
     */
    @Composable
    fun LocalOwnersProvider(
        saveableStateHolder: SaveableStateHolder = rememberSaveableStateHolder(),
        content: @Composable () -> Unit,
    )
}

/**
 * Default implementation of [ViewModelStoreContentProvider] that wires a [view] to its [ViewModelStoreOwner],
 * [CoroutineScope], and lifecycle visibility events.
 *
 * This class ensures that:
 * - The [view] has a retained [ViewModelStore] scoped to its lifecycle.
 * - Lifecycle visibility is tracked via [ViewAppearanceEvents].
 * - Coroutine scope is tied to the view's appearance and cancelled appropriately.
 *
 * @param view The view instance to be hosted.
 * @param scope The view-scoped lifecycle and coroutine scope.
 */
@Immutable
internal open class ViewModelStoreContentProviderImpl<V>(view: () -> V, private val scope: ViewLifecycleScope) :
    ViewModelStoreContentProvider<V>, ManagedCoroutineScope by scope {
    private val owner = DefaultViewModelStoreOwner()

    override val view: V by lazy(mode = LazyThreadSafetyMode.NONE, initializer = view)

    override fun cancel(awaitChildrenComplete: Boolean, message: String) {
        owner.clear()
        scope.cancel(awaitChildrenComplete = awaitChildrenComplete, message = message)
    }

    @Composable
    override fun LocalOwnersProvider(saveableStateHolder: SaveableStateHolder, content: @Composable () -> Unit) {
        owner.LocalOwnersProvider(saveableStateHolder) {
            scope.viewAppearanceEvents.ObserveViewVisibility()
            content()
        }
    }
}
