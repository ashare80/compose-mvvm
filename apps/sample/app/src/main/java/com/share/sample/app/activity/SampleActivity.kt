package com.share.sample.app.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.share.external.lib.mvvm.activity.ActivityComponentInject
import com.share.external.lib.mvvm.activity.ViewModelComponentActivity
import com.share.external.lib.mvvm.navigation.modal.LocalDecorViewProperties
import com.share.external.lib.mvvm.navigation.modal.decorViewProperties

interface SampleActivityComponentInject :
    ActivityComponentInject<
        SampleActivity,
        SampleActivityComponent,
        SampleActivityComponent.Factory,
        SampleActivityViewModelComponent,
    >

class SampleActivity :
    ViewModelComponentActivity<SampleActivityViewModelComponent>(),
    SampleActivityComponentInject,
    SampleActivityViewModelComponent.Activity {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inject()
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalDecorViewProperties provides decorViewProperties()) {
                viewModelComponent.view.content()
            }
        }
    }
}
