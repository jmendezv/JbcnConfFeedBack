package cat.cristina.pep.jbcnconffeedback.components

import cat.cristina.pep.jbcnconffeedback.activity.MainActivity
import cat.cristina.pep.jbcnconffeedback.modules.ContextModule
import dagger.Component

@Component(modules = [ContextModule::class], dependencies = [ApplicationComponent::class])
interface MainActivityComponent {

    public fun inject(mainActivity: MainActivity): Unit

}