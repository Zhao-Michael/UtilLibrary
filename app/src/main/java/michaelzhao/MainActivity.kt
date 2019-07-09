package michaelzhao

import android.content.res.Configuration
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import michaelzhao.controls.VideoPlayer
import org.jetbrains.anko.find


class MainActivity : AppCompatActivity() {

    val player by lazy { find<VideoPlayer>(R.id.mVideoPlayer) }

    val btn1 by lazy { find<Button>(R.id.btn1) }
    val btn2 by lazy { find<Button>(R.id.btn2) }
    val btn3 by lazy { find<Button>(R.id.btn3) }
    val btn4 by lazy { find<Button>(R.id.btn4) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        App.Instance.registerActivityLifecycleCallbacks(player)

        player.SetVideoUrl(
            "https://ap1-ws.yoipu.com/2OQZyV31X1z/preview/pv.m3u8"
        )

        player.ShowTitleBar(true)
        player.ShowControlBar(true)
        btn1.setOnClickListener {
            player.StartVideo()
        }
        btn2.setOnClickListener {
            player.PauseVideo()
        }
        btn3.setOnClickListener {
            player.StopVideo()
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        player.mConfigChangedCallback()
    }

}
