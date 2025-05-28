package com.share.sample.feature.onboarding

import androidx.compose.runtime.Composable
import com.share.external.lib.mvvm.navigation.content.Screen
import com.share.external.lib.mvvm.navigation.content.View
import com.share.external.lib.mvvm.navigation.stack.NavigationStackHost
import com.share.external.lib.mvvm.navigation.stack.ViewModelNavigationStack
import com.share.sample.feature.onboarding.signin.SignInComponent
import dagger.Module
import dagger.Provides

@Module
object OnboardingViewModule {
    @OnboardingScope
    @Provides
    fun onboardingView(scope: OnboardingComponent.Scope, signIn: SignInComponent.Factory) =
        OnboardingView(navigationStack = ViewModelNavigationStack(scope, initialStack = { it.push(signIn) }))
}

class OnboardingView(private val navigationStack: ViewModelNavigationStack<Screen>) : View {
    override val content: @Composable () -> Unit = {
        NavigationStackHost(
            analyticsId = "OnboardingNavigationStackHost",
            backHandlerEnabled = navigationStack.size > 1,
            navigationStack = navigationStack,
        ) {}
    }
}
