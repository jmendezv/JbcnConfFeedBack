package cat.cristina.pep.jbcnconffeedback.components

import cat.cristina.pep.jbcnconffeedback.modules.ApplicationModule
import dagger.Component

@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {

    // getters for top level graph application objects

}