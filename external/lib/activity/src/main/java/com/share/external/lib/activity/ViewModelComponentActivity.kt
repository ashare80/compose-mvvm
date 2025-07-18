package com.share.external.lib.activity

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import com.share.external.lib.activity.application.ApplicationProvider
import com.share.external.lib.activity.viewmodel.viewModel

abstract class ViewModelComponentActivity<ViewModelComponent : ActivityViewModelComponent> :
    ComponentActivity(), ApplicationProvider, ActivityViewModelComponentProvider<ViewModelComponent> {
    private val componentHolderViewModel by viewModel {
        val component = buildViewModelComponent()
        ComponentViewModel(component = component, coroutineScope = component.scope)
    }

    val viewModelComponent: ViewModelComponent
        get() = componentHolderViewModel.component

    private class ComponentViewModel<ViewModelComponent>(
        val component: ViewModelComponent,
        coroutineScope: ActivityViewModelCoroutineScope,
    ) :
        ViewModel(
            AutoCloseable { coroutineScope.cancel(awaitChildrenComplete = false, message = "Activity destroyed") }
        )
}
