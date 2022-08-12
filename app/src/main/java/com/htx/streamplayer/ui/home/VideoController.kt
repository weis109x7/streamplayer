package com.htx.streamplayer.ui.home


import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Point
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.Toast
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout
import java.io.File
import java.io.InputStream
import java.util.*


class VideoController(activity: Activity): IVLCVout.Callback, MediaPlayer.EventListener {
    // create TAG for logging
    companion object {
        private var TAG= "VideoController"
    }
    // declare media player object
    private var mediaPlayer: MediaPlayer?=null
    // declare surface view object
    var mSurface: SurfaceView?=null
    // declare surface holder object
    var holder: SurfaceHolder?= null

    // declare libvlc object
    private var libvlc: LibVLC?=null

    private var controller : MediaController?=null

    // declare/initialize activity
    private var activity: Activity?=null
    init {
        this.activity=activity
    }




    /**
     * Creates MediaPlayer and plays video

     * @param media
     */
    fun createPlayer(media: String) {
        if(mediaPlayer!=null && libvlc!=null){
            releasePlayer()
        }
        Log.i(TAG, "Creating vlc player")
        try {
            // create arraylist to assign option to create libvlc object
            val options = ArrayList<String>()
            options.add("--aout=opensles")
            options.add("--http-reconnect")
            options.add("--audio-time-stretch") // time stretching
            options.add("--network-caching=1500")
            options.add("-vvv") // verbosity

            options.add("--video-filter=rotate")
            options.add("--rotate-angle=180")


            // create libvlc object
            libvlc = LibVLC(activity, options)

            // get surface view holder to display video
            this.holder=mSurface!!.holder
            holder!!.setKeepScreenOn(true)

            // Creating media player
            mediaPlayer = MediaPlayer(libvlc)

//            controller = MediaController(activity);
//            controller?.setMediaPlayer(playerInterface);
//            controller?.setAnchorView(mSurface);
//            mSurface!!.setOnClickListener{ controller!!.show() }



                    // Setting up video output
            val vout = mediaPlayer!!.vlcVout
            vout.setVideoView(mSurface)
            vout.addCallback(this)
            vout.attachViews()

            val m = if (media[0] == '/'){
                Media(libvlc, media)
            } else{
                Media(libvlc, Uri.parse(media))
            }
            mediaPlayer!!.media = m
            mediaPlayer!!.play()


        } catch (e: Exception) {
            Toast.makeText(activity, "Error in creating player!", Toast
                .LENGTH_LONG).show()
        }

    }

    /*
   * release player
   * */
    fun releasePlayer() {
        Log.i(TAG,"releasing player started")
        if (libvlc == null)
            return
        mediaPlayer!!.stop()
        var vout: IVLCVout = mediaPlayer!!.vlcVout
        vout.removeCallback(this)
        vout.detachViews()
        mediaPlayer!!.release()
        mediaPlayer=null
        holder = null
        libvlc!!.release()
        libvlc = null

        Log.i(TAG,"released player")
    }

    override fun onEvent(event: MediaPlayer.Event) {

        when (event.type) {
            MediaPlayer.Event.EndReached -> {
                this.releasePlayer()
            }

            MediaPlayer.Event.Playing->Log.i("playing","playing")
            MediaPlayer.Event.Paused->Log.i("paused","paused")
            MediaPlayer.Event.Stopped->Log.i("stopped","stopped")
            else->Log.i("nothing","nothing")
        }
    }

    override fun onSurfacesCreated(vlcVout: IVLCVout?) {
        val sw = mSurface!!.width
        val sh = mSurface!!.height

        if (sw * sh == 0) {
            Log.e(TAG, "Invalid surface size")
            return
        }

        mediaPlayer!!.vlcVout.setWindowSize(sw, sh)
        mediaPlayer!!.aspectRatio="4:3"
        mediaPlayer!!.setScale(0f)
    }

    override fun onSurfacesDestroyed(vlcVout: IVLCVout?) {
        releasePlayer()
    }

    fun pause(){
        mediaPlayer?.pause()
    }

    fun toggleFullScreen() {
//        TODO create fullscreen
        Log.v("FullScreen", "-----------Set full screen SCREEN_ORIENTATION_LANDSCAPE------------")
//      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
//        val display: Display? = (activity?.windowManager?.getDefaultDisplay())
//        val size = Point()
//        if (display != null) {
//            display.getSize(size)
//        }
//        val width = size.x
//        val height = size.y
//
//        Log.d("fffffff", width.toString())
//        Log.d("fffffff", height.toString())
//
//        mSurface?.holder?.setFixedSize(width, height);


        //2nd implwmentatipon
//        Log.v("FullScreen", "-----------Set full screen SCREEN_ORIENTATION_LANDSCAPE------------")
////        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
//        val size = Point()
//        (activity?.windowManager?.getDefaultDisplay())?.getSize(size)
//        val width = size.x
//        val height = size.y
//
//        val layout = FrameLayout.LayoutParams(width, height)
//        layout.gravity = Gravity.CENTER
//        mSurface?.layoutParams = layout

    }

    fun snapshot() {
//        TODO create snapshot

    }

    fun record() {
//        TODO create record
        Log.v("Screen Record", "-----------Trying to record------------")
        val dir = File("/storage/emulated/0/Download", "recording")
        mediaPlayer?.record(dir.toString())
    }

//    private val playerInterface: MediaPlayerControl = object : MediaPlayerControl {
//        override fun getBufferPercentage(): Int {
//            return 0
//        }
//
//        override fun getCurrentPosition(): Int {
//            val pos: Float = mediaPlayer!!.position
//            return (pos * duration).toInt()
//        }
//
//        override fun getDuration(): Int {
//            return mediaPlayer!!.length.toInt()
//        }
//
//        override fun isPlaying(): Boolean {
//            return mediaPlayer!!.isPlaying
//        }
//
//        override fun pause() {
//            mediaPlayer?.pause()
//        }
//
//        override fun seekTo(pos: Int) {
//            mediaPlayer?.setPosition(pos.toFloat() / duration)
//        }
//
//        override fun start() {
//            mediaPlayer?.play()
//        }
//
//        override fun canPause(): Boolean {
//            return true
//        }
//
//        override fun canSeekBackward(): Boolean {
//            return true
//        }
//
//        override fun canSeekForward(): Boolean {
//            return true
//        }
//
//        override fun getAudioSessionId(): Int {
//            return 0
//        }
//    }

}

