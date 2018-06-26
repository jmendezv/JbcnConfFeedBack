package cat.cristina.pep.jbcnconffeedback.components

import cat.cristina.pep.jbcnconffeedback.activity.MainActivity
import dagger.Component

@Component(modules = [], dependencies = [ApplicationComponent::class])
interface MainActivityComponent {

    public fun inject(mainActivity: MainActivity): Unit

}