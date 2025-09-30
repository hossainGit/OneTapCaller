import android.app.Application

class ElderlyContactsApp : Application() {

    companion object {
        lateinit var instance: ElderlyContactsApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}