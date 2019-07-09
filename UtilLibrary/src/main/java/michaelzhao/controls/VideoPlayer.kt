package michaelzhao.controls

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Context.SENSOR_SERVICE
import android.content.pm.ActivityInfo.*
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.*
import androidx.core.view.GestureDetectorCompat
import michaelzhao.utillibrary.*
import org.jetbrains.anko.find
import org.jetbrains.anko.toast
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class VideoPlayer(context: Context, attrs: AttributeSet) :
    RelativeLayout(context, attrs),
    SeekBar.OnSeekBarChangeListener,
    View.OnTouchListener,
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    Application.ActivityLifecycleCallbacks {

    private enum class STATUS {
        STATE_ERROR,
        STATE_IDLE,
        STATE_PREPARING,
        STATE_PREPARED,
        STATE_PLAYING,
        STATE_PAUSED,
        STATE_PLAYBACK_COMPLETED
    }

    //region Play Field

    private var mCurrentState = STATUS.STATE_IDLE
    private var mOld_Position = 0
    private var mDownBrightness = 0
    private var mDownVolume = 0
    private var mTimeThread: Thread? = null
    private var mHideBarTick = -1
    private var mHideLoadTick = -1
    private var mCurrPlayTime = 0
    private var mVideoUri: String = ""
    private var mEnableSeekVideo = true

    //endregion

    //region Activity Fields

    private val mActivity = context as Activity
    private val mGestureListener by lazy { GestureDetectorCompat(context, this) }
    private val mAudioManager by lazy { context.getSystemService(AUDIO_SERVICE) as AudioManager }
    private val mFormatter by lazy {
        SimpleDateFormat("HH:mm:ss", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
    }
    private val mSensorManager by lazy { context.getSystemService(SENSOR_SERVICE) as SensorManager }
    private val mMaxVolume by lazy { mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    private val mLock = Object()
    private var mIsExpanded = false
    private var mIsVertical = false
    private var mShrinkSize = Point()

    var mConfigChangedCallback = {
        onConfigurationChanged()
    }

    //endregion

    //region controls
    private val mVideoView by lazy { find<VideoView>(R.id.mVideoView) }

    private val mPopupMenu: PopupMenu by lazy { PopupMenu(context, buttonMenu) }
    private val mBigPlayBtn by lazy { (find<ImageView>(R.id.mBigPlayBtn)) }
    private val mSmallPlayBtn by lazy { find<ImageView>(R.id.mSmallPlayBtn) }
    private val mExpandBtn by lazy { find<ImageView>(R.id.mExpandBtn) }
    private val mShrinkBtn by lazy { find<ImageView>(R.id.mShrinkBtn) }

    private val mVideoProgress by lazy { find<SeekBar>(R.id.mVideoProgress) }
    private val mTopBlurView by lazy { find<View>(R.id.mTop_layout) }
    private val mBottomBlurView by lazy { find<View>(R.id.mBottom_layout) }
    private val mTop_layout by lazy { find<LinearLayout>(R.id.mTop_layout) }
    private val mBottom_layout by lazy { find<LinearLayout>(R.id.mBottom_layout) }
    private val mLoadingProgress by lazy { find<ProgressBar>(R.id.mLoadingProgress) }
    private val mPreviewImage by lazy { find<ImageView>(R.id.mPreviewImage) }
    private val mMain_Layout by lazy { find<RelativeLayout>(R.id.mMain_Layout) }
    private val mButtonBack by lazy { find<ImageButton>(R.id.mButtonBack) }
    private val buttonMenu by lazy { find<ImageButton>(R.id.buttonMenu) }
    private val mBrightness_layout by lazy { find<LinearLayout>(R.id.mBrightness_layout) }
    private val mVolume_layout by lazy { find<LinearLayout>(R.id.mVolume_layout) }
    private val mTextTime by lazy { find<TextView>(R.id.mTextTime) }
    private val mVideoTitle by lazy { find<TextView>(R.id.mVideoTitle) }
    private val mRootLayout by lazy { find<FrameLayout>(R.id.mRootLayout) }

    private val mAnimationOut_top by lazy { loadAnimation(context, R.anim.anim_exit_top) }
    private val mAnimationOut_bottom by lazy { loadAnimation(context, R.anim.anim_exit_bottom) }
    private val mAnimationIn_top by lazy { loadAnimation(context, R.anim.anim_enter_top) }
    private val mAnimationIn_bottom by lazy { loadAnimation(context, R.anim.anim_enter_bottom) }

    //endregion

    //region Init methods

    init {
        View.inflate(context, R.layout.videoplayer_layout, this)
        mVideoProgress.setOnSeekBarChangeListener(this)
        this.setOnTouchListener(this)
        mMain_Layout.performClick()
        init_VideoView()
        init_Timer()
        init_Button()
        InitSensor()
    }

    private fun init_Button() {
        mSmallPlayBtn.onClick {
            val res = mSmallPlayBtn.tag
            if (res == R.drawable.ic_btn_pause)
                PauseVideo()
            else
                StartVideo()
        }
        mBigPlayBtn.onClick { StartVideo() }
        mExpandBtn.onClick { Expand() }
        mShrinkBtn.onClick { Shrink() }
        mButtonBack.onClick {
            if (mIsExpanded) {
                Shrink()
            } else {
                mActivity.finish()
            }
        }
    }

    private fun init_VideoView() {
        mVideoView.setOnPreparedListener {
            if (mCurrentState != STATUS.STATE_PAUSED)
                mCurrentState = STATUS.STATE_PLAYING
            showLoading(false)
            it.setOnInfoListener { _, what, _ ->
                when (what) {
                    MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {//3
                        mCurrentState = STATUS.STATE_PLAYING
                        uiThread(300) { showLoading(false) }
                    }
                    MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING -> {//700
                        showLoading()
                    }
                    MediaPlayer.MEDIA_INFO_BUFFERING_END -> {//702
                        showLoading(false)
                    }
                    703 -> {//MEDIA_INFO_NETWORK_BANDWIDTH
                        showLoading()
                    }
                    MediaPlayer.MEDIA_INFO_NOT_SEEKABLE -> {//801
                        context.toast("Media_Info_Not_Seekable")
                    }
                    MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> {//901
                        context.toast("Media_Error_Unsupported")
                    }
                }
                false
            }
        }
        mVideoView.setOnErrorListener { _, what, code ->
            StopVideo()
            val error = when {
                code == MEDIA_ERROR_IO -> "Media_Error_IO"
                code == MEDIA_ERROR_MALFORMED -> "Media_Error_Malformed"
                code == MEDIA_ERROR_UNSUPPORTED -> "Media_Error_Unsupported"
                code == MEDIA_ERROR_TIMED_OUT -> "Media_Error_Timed_out"
                what == MEDIA_ERROR_UNKNOWN -> "Media_Error_Unknown"
                what == MEDIA_ERROR_SERVER_DIED -> "Media_Error_Server_died"
                else -> "Media_Error_System"
            }
            context.toast("Player Error : $error")
            return@setOnErrorListener true
        }
        mVideoView.setOnCompletionListener {
            mBigPlayBtn.show()
            mCurrentState = STATUS.STATE_PLAYBACK_COMPLETED
            mVideoProgress.progress = mVideoProgress.max
        }
    }

    private fun init_Timer() {
        if (mTimeThread != null && mTimeThread!!.isAlive)
            return
        mTimeThread = Thread {
            try {
                timerTick()
            } catch (ex: Throwable) {
                println(ex.message)
                StopVideo()
            }
            mTimeThread = null
        }
        mTimeThread?.start()
    }

    private fun InitSensor() {
        mSensorManager.registerListener(object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    try {
                        if (!mIsExpanded || mIsVertical)
                            return
                        if (Sensor.TYPE_ACCELEROMETER != event.sensor?.type)
                            return
                    } catch (ex: Exception) {
                        println(ex.message)
                        return
                    }

                    val Sensor_ax: Float = event.values[0]
                    val Sensor_ay: Float = event.values[1]
                    val Sensor_az: Float = event.values[2]

                    if (Sensor_ax > 6 && Sensor_ay < 2 && Sensor_az < 7) {
                        mActivity.requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE
                    } else if (Sensor_ax < -7 && Sensor_ay < 1 && Sensor_az < 7) {
                        mActivity.requestedOrientation = SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    }
                }
            }
        }, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
    }

    //endregion

    //region Public control functions

    fun ShowTitleBar(show: Boolean) {
        if (show)
            mTopBlurView.show()
        else
            mTopBlurView.visibility = View.INVISIBLE
    }

    fun ShowControlBar(show: Boolean) {
        if (show)
            mBottomBlurView.show()
        else
            mBottomBlurView.visibility = View.INVISIBLE
    }

    fun EnableSeek(enable: Boolean) {
        mEnableSeekVideo = enable
    }

    fun SetTitle(title: String) {
        mVideoTitle.text = title
    }

    fun SetVideoUrl(url: String) {
        mVideoUri = url
    }

    fun StartVideo() {
        init_Timer()
        if (!isInPlaybackState() || mCurrentState == STATUS.STATE_PLAYBACK_COMPLETED) {
            showLoading()
            mPreviewImage.hide()
            mBigPlayBtn.hide()
            setSmallPlayBtnState(false)
            mCurrentState = STATUS.STATE_PREPARING
            mCurrPlayTime = 0
            mVideoView.setVideoURI(Uri.parse(mVideoUri))
            mVideoView.start()
        } else if (isInPlaybackState() && mCurrentState == STATUS.STATE_PAUSED) {
            mPreviewImage.hide()
            mBigPlayBtn.hide()
            setSmallPlayBtnState(false)
            mVideoView.start()
        }
    }

    fun PauseVideo() {
        if (mVideoView.canPause() && mVideoView.isPlaying) {
            mCurrentState = STATUS.STATE_PAUSED
            setSmallPlayBtnState()
            mVideoView.pause()
        }
    }

    fun StopVideo() {
        uiThread {
            mCurrPlayTime = 0
            showLoading(false)
            mBigPlayBtn.show()
            setSmallPlayBtnState()
            mCurrentState = STATUS.STATE_IDLE
            mVideoView.stopPlayback()
            mTimeThread = null
        }
    }

    private fun setVideoRect(width: Float, height: Float) {
        setHeight(height.toInt())
        setWidth(width.toInt())
        this.layoutParams.height = height.toInt()
        this.layoutParams.width = width.toInt()
        (parent as View).layoutParams.height = height.toInt()
        (parent as View).layoutParams.width = width.toInt()
    }

    fun Expand() {
        if (mIsExpanded)
            return
        mIsExpanded = true

        mShrinkSize = Point(width, height)

        resetOrientation()

        mExpandBtn.hide()
        mShrinkBtn.show()
    }

    fun Shrink() {
        if (!mIsExpanded)
            return
        mIsExpanded = false

        resetOrientation()

        mExpandBtn.show()
        mShrinkBtn.hide()
    }

    private fun resetOrientation() {
        if (mIsVertical) {
            mActivity.requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
        } else if (mIsExpanded) {
            mActivity.requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE
        } else {
            mActivity.requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun onConfigurationChanged() {
        if (mIsVertical || mIsExpanded) {
            setVideoRect(mActivity.getWidthInPx(), mActivity.getHeightInPx())
            hideStatusBar(true)
        } else {
            setVideoRect(mShrinkSize.x.toFloat(), mShrinkSize.y.toFloat())
            hideStatusBar(false)
        }
    }

    private fun getTopOffset(): Int {
        return if (Build.VERSION.SDK_INT >= 28) {
            mActivity.window.decorView.rootWindowInsets.displayCutout.safeInsetTop
        } else if (Build.MODEL == "MI 8 SE") {
            mActivity.getStatusBarHeight() + 20
        } else if (Build.MODEL == "MI 8") {
            0
        } else {
            0
        }
    }

    private fun hideStatusBar(isHide: Boolean) {
        if (isHide)
            mActivity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        else
            mActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    //endregion

    //region Private functions

    private fun showLoading(b: Boolean = true) {
        if (b) {
            mLoadingProgress.show()
        } else {
            mLoadingProgress.hide()
        }
    }

    private fun isInPlaybackState(): Boolean {
        return mCurrentState != STATUS.STATE_ERROR &&
                mCurrentState != STATUS.STATE_IDLE &&
                mCurrentState != STATUS.STATE_PREPARING
    }

    private fun updatePlayProgress() {
        uiThread {
            if (mCurrentState == STATUS.STATE_PLAYING) {
                val allTime = max(mVideoView.duration, 1)
                mCurrPlayTime = mVideoView.currentPosition
                mVideoProgress.progress = mCurrPlayTime * 100 / allTime
                val time =
                    "${mFormatter.format(Date(mCurrPlayTime.toLong()))}/${mFormatter.format(Date(allTime.toLong()))}"
                mTextTime.text = time
            }
            mVideoProgress.secondaryProgress = mVideoView.bufferPercentage
        }
    }

    private fun timerTick() {
        while (!mActivity.isFinishing) {
            updatePlayProgress()

            if (mVideoView.isPlaying
                && mOld_Position == mVideoView.currentPosition
                && mVideoView.currentPosition > 100
            ) {
                showLoading()
            }

            if (mVideoView.isPlaying
                && mLoadingProgress.isShown
                && mOld_Position != mVideoView.currentPosition
            ) {
                mHideLoadTick++
                if (mHideLoadTick > 4) {
                    mHideLoadTick = 0
                    showLoading(false)
                    mCurrentState = STATUS.STATE_PLAYING
                }
            } else {
                mHideLoadTick = 0
            }

            mOld_Position = mVideoView.currentPosition

            Thread.sleep(300)
        }
    }

    private fun setSmallPlayBtnState(isSetPlay: Boolean = true) {
        val res = if (isSetPlay) R.drawable.ic_btn_play else R.drawable.ic_btn_pause
        mSmallPlayBtn.setImageResource(res)
        mSmallPlayBtn.tag = res
    }

    //endregion

    //region interface for seek bar

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            mHideBarTick = 0
            val time = progress * mVideoView.duration / 100
            if (mEnableSeekVideo) {
                mVideoView.seekTo(time)
                updatePlayProgress()
            } else {
                val dst = progress * 1.0 / mVideoProgress.max
                val currPer = mVideoView.currentPosition * 1.0 / mVideoView.duration
                val buffPer = mVideoView.bufferPercentage / 100.0
                if (dst < buffPer && dst > currPer) {
                    mVideoView.seekTo(time)
                    updatePlayProgress()
                }
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit

    //endregion

    //region interface for GestureDetector

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        mGestureListener.onTouchEvent(event)
        return true
    }

    override fun onShowPress(e: MotionEvent?) = Unit

    override fun onSingleTapUp(e: MotionEvent?): Boolean = false

    override fun onDown(e: MotionEvent?): Boolean = false

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean = false

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean = false

    override fun onLongPress(e: MotionEvent?) = Unit

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        if (mIsVertical) {
            return false
        } else if (mIsExpanded && isInPlaybackState()) {
            Shrink()
        } else if (!mIsExpanded && isInPlaybackState()) {
            Expand()
        }
        return false
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean = false

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean = false

    //endregion

    //region Activity Callback

    override fun onActivityPaused(activity: Activity?) {
        if (activity == mActivity) {
            PauseVideo()
        }
    }

    override fun onActivityResumed(activity: Activity?) {
        if (activity == mActivity) {
            if (isInPlaybackState() && mCurrPlayTime > 0) {
                mVideoView.seekTo(mCurrPlayTime)
            }
        }
    }

    override fun onActivityStarted(activity: Activity?) = Unit

    override fun onActivityDestroyed(activity: Activity?) = Unit

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) = Unit

    override fun onActivityStopped(activity: Activity?) = Unit

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) = Unit

    //endregion

    override fun performClick(): Boolean {
        return super.performClick()
    }
}