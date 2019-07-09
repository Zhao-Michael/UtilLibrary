package michaelzhao

import android.app.Application

class App : Application() {

    companion object {
        lateinit var Instance: App
    }

    override fun onCreate() {
        super.onCreate()
        Instance = this
    }

}