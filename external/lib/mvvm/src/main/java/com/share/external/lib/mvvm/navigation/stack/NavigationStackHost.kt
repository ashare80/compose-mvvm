package com.share.external.lib.mvvm.navigation.stack

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import co.touchlab.kermit.Logger
import com.share.external.lib.core.View
import com.share.external.lib.mvvm.navigation.content.ViewPresentation
import com.share.external.lib.core.ViewProvider
import com.share.external.lib.compose.modal.ModalContainer
import com.share.external.lib.compose.modal.ModalProperties

/**
 * Hosts a navigation stack of [View]s and renders the appropriate content based on their [ViewPresentation.Style].
 *
 * This composable observes a [ViewModelNavigationStack] and renders at most one full-screen view and one modal view at
 * any given time. Views are presented in order from the top of the stack (i.e., most recently pushed) and updated
 * automatically as the stack changes.
 *
 * ### Behavior
 * - Renders the first view in the stack marked as [ViewPresentation.Style.FullScreen].
 * - Renders the first view in the stack marked as [ViewPresentation.Style.Modal], **if it appears before** the
 *   full-screen view. Modal content is layered above the full-screen or [defaultContent].
 * - If no full-screen view is found, [defaultContent] is displayed.
 * - Supports modal presentation with layout and interaction behavior defined via [ModalProperties].
 * - Handles system back navigation via [BackHandler] when [backHandlerEnabled] is true.
 * - Preserves state across recompositions for all rendered views using [rememberSaveableStateHolder].
 * - Logs view appearance state changes using [analyticsId] for debugging or analytics purposes.
 *
 * @param analyticsId A string identifier used to log changes to the visible back stack.
 * @param navigationStack The stack of views to render, maintained via [ViewModelNavigationStack].
 * @param backHandlerEnabled Enables system back button support to pop the stack when true.
 * @param defaultContent Composable content shown when no full-screen view is present.
 * @see View
 * @see ViewPresentation
 * @see ModalContainer
 * @see ViewModelNavigationStack
 * @see ViewModelStoreContentProvider
 */
@Composable
fun <V> NavigationStackHost(
    analyticsId: String,
    navigationStack: ViewModelNavigationStack<V>,
    backHandlerEnabled: Boolean = true,
    defaultContent: @Composable () -> Unit,
) where V : ViewProvider, V : ViewPresentation {
    navigationStack.run {
        val saveableStateHolder = rememberSaveableStateHolder()

        var backing: ViewPresentationProviderViewModelStoreContentProvider<V>? = null
        var last: ViewPresentationProviderViewModelStoreContentProvider<V>? = null
        var properties: ModalProperties? = null

        for (provider in navigationStack.stack.asReversed()) {
            when (val displayMode = provider.preferredPresentationStyle()) {
                ViewPresentation.Style.FullScreen -> {
                    if (last == null) {
                        last = provider
                    } else {
                        backing = provider
                    }
                    break
                }
                is ViewPresentation.Style.Modal -> {
                    if (last == null) {
                        last = provider
                        properties = displayMode.properties
                    }
                }
            }
        }

        if (backHandlerEnabled && last != null) {
            BackHandler(onBack = ::pop)
        }

        backing?.run {
            LocalOwnersProvider(
                saveableStateHolder = saveableStateHolder,
                content = view.content
            )
        }

        last?.run {
            LocalOwnersProvider(
                saveableStateHolder = saveableStateHolder,
            ) {
                // Always display last view in modal container to keep view size changes in the same context.
                ModalContainer(onDismiss = ::pop, properties = properties, content = view.content)
            }
        } ?: defaultContent()

        LaunchedEffect(stack) {
            logger.logEntries(entries = stack, analyticsId = analyticsId) {
                if (backing == it || (backing == null && last == it)) {
                    "FullScreen"
                } else if (last == it) {
                    "Modal"
                } else null
            }
        }
    }
}

private val logger = Logger.withTag("NavigationStackHost")
