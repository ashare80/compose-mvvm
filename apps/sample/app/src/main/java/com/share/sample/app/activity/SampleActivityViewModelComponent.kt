package com.share.sample.app.activity

import com.share.external.lib.mvvm.activity.ActivityComponentProvider
import com.share.external.lib.mvvm.activity.ActivityViewModelComponent
import com.share.external.lib.mvvm.activity.ActivityViewModelComponentProvider
import com.share.external.lib.mvvm.activity.ActivityViewModelCoroutineScope
import com.share.external.lib.mvvm.application.ApplicationCoroutineScope
import com.share.sample.feature.onboarding.OnboardingComponent
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Scope

@Scope @MustBeDocumented @Retention(value = AnnotationRetention.RUNTIME) annotation class ActivityViewModelScope

@ActivityViewModelScope
@Subcomponent(modules = [SampleActivityViewModelModule::class])
interface SampleActivityViewModelComponent :
    ActivityViewModelComponent, ActivityComponentProvider<SampleActivityComponent.Factory> {
    val view: SampleActivityView

    @Subcomponent.Factory interface Factory : () -> SampleActivityViewModelComponent

    interface Application {
        val sampleActivityViewModelComponent: Factory
    }

    interface Activity : ActivityViewModelComponentProvider<SampleActivityViewModelComponent> {
        override fun buildViewModelComponent(): SampleActivityViewModelComponent {
            return (getApplication() as Application).sampleActivityViewModelComponent()
        }
    }
}

@Module(subcomponents = [SampleActivityComponent::class, OnboardingComponent::class])
object SampleActivityViewModelModule {
    @ActivityViewModelScope
    @Provides
    fun scope(applicationCoroutineScope: ApplicationCoroutineScope) =
        ActivityViewModelCoroutineScope(applicationCoroutineScope)

    @ActivityViewModelScope
    @Provides
    fun pageNavigationController(scope: ActivityViewModelCoroutineScope) =
        ActivityViewNavigationController(scope = scope)

    @ActivityViewModelScope
    @Provides
    fun view(onboarding: OnboardingComponent.Factory, navigationController: ActivityViewNavigationController) =
        SampleActivityView(onboarding = onboarding, navigationController = navigationController)
}
