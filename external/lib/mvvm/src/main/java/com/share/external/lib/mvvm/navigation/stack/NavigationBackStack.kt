package com.share.external.lib.mvvm.navigation.stack

import androidx.compose.runtime.Stable
import com.share.external.lib.mvvm.navigation.content.NavigationKey

/**
 * Contract for a minimal navigation back stack that supports only **backward navigation**.
 *
 * This is used to expose a limited interface to nested or delegated navigation stacks, ensuring encapsulation by hiding
 * forward navigation capabilities (e.g., push).
 *
 * Must be invoked on the **main thread**.
 */
@Stable
interface NavigationBackStack {

    /**
     * The current number of entries in the back stack.
     *
     * Returns `0` when the stack is empty.
     */
    val size: Int

    /**
     * Pops the top-most entry from the stack.
     *
     * @return `true` if the stack was mutated (i.e., an element was removed), `false` if the stack was already empty.
     */
    fun pop(): Boolean

    /**
     * Pops entries from the stack until the specified [key] is at the top.
     *
     * If [inclusive] is `true`, the [key] itself will also be removed.
     *
     * @param key The navigation key to pop to.
     * @param inclusive Whether to remove the [key] as well.
     * @return `true` if the stack was mutated, `false` if the [key] was not found or no changes were made.
     */
    fun popTo(key: NavigationKey, inclusive: Boolean = false): Boolean

    /**
     * Removes the specified [key] from the stack, regardless of its position.
     *
     * @param key The navigation key to remove.
     *
     * No-op if the [key] is not found in the stack.
     */
    fun remove(key: NavigationKey)

    /**
     * Removes all entries from the stack, leaving it empty.
     *
     * No-op if the stack is already empty.
     */
    fun removeAll()

    fun transaction(block: () -> Unit)
}
