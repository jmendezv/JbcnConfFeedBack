package cat.cristina.pep.jbcnconffeedback.modules

import android.content.Context
import dagger.Module
import dagger.Provides

@Module
data class ContextModule public constructor(val context: Context){

    @Provides
    public fun getFeedBackApplicationContext(): Context = context

}