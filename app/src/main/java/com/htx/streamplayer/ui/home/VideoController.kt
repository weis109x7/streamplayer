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
import androidx.core.content.ContentProviderCompat.requireContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout
import org.videolan.libvlc.media.VideoView
import org.videolan.libvlc.util.DisplayManager
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File
import java.io.InputStream
import java.util.*

class VideoController(activity: Activity): IVLCVout.Callback, MediaPlayer.EventListener {
    // create TAG for logging
    companion object {
        private var TAG = "VideoController"
    }

    // declare media player object
    private var mediaPlayer: MediaPlayer? = null

    // declare surface view object
    var mSurface: VLCVideoLayout? = null

    // declare libvlc object
    private var libvlc: LibVLC? = null

    // declare/initialize activity
    private var activity: Activity? = null
    init {
        this.activity = activity
    }


    /**
     * Creates MediaPlayer and plays video

     * @param media
     */
    fun createPlayer(media: String) {
        if (mediaPlayer != null || libvlc != null) {
            Log.i(TAG, "player not null, release player first")
            releasePlayer()
        }
        Log.i(TAG, "Creating vlc player")
        try {
            // create arraylist to assign option to create libvlc object
            val options = ArrayList<String>()

//            options.add("--rtsp-tcp");
//            options.add("--file-caching=0")
//            options.add("--live-caching=0")
//            options.add("--drop-late-frames")
//            options.add("--skip-frames")
            options.add("--aout=none")
            options.add("--http-reconnect")
//            options.add("--audio-time-stretch") // time stretching
            options.add("-vvv") // verbosity

            options.add("--video-filter=rotate")
            options.add("--rotate-angle=180")

            options.add("--swscale-mode=0")
            options.add(":network-caching=150")
            options.add(":clock-jitter=0")
            options.add(":clock-synchro=0")

            // create libvlc object
            libvlc = LibVLC(activity, options)

            // Creating media player
            mediaPlayer = MediaPlayer(libvlc)

            // Setting up video output
            mediaPlayer!!.attachViews(mSurface!!, null, false, true)

            //check if URL is local path
            val m = if (media[0] == '/') {
                //create media with local path
                Media(libvlc, media)
            } else {
                //create media with URL
                Media(libvlc, Uri.parse(media))
            }
            m.setHWDecoderEnabled(false, false);

            //attach media to player
            mediaPlayer!!.media = m
            //play media
            mediaPlayer!!.play()


        } catch (e: Exception) {
            Toast.makeText(
                activity, "Empty URL!", Toast
                    .LENGTH_LONG
            ).show()

            Log.i(TAG, "Error")
            Log.i(TAG, e.toString())
            e.printStackTrace()
        }

    }

    /*
   * release player
   * */
    fun releasePlayer() {
        Log.i(TAG, "releasing player started")

        //clearing all reference
        mediaPlayer!!.stop()
        mediaPlayer!!.detachViews()
        mediaPlayer!!.release()
        mediaPlayer = null
        libvlc!!.release()
        libvlc = null
        mSurface = null
        libvlc= null
        activity = null

        Log.i(TAG, "released player")
    }

    override fun onEvent(event: MediaPlayer.Event) {

        when (event.type) {
            MediaPlayer.Event.EndReached -> {
                this.releasePlayer()
            }

            MediaPlayer.Event.Playing -> Log.i("playing", "playing")
            MediaPlayer.Event.Paused -> Log.i("paused", "paused")
            MediaPlayer.Event.Stopped -> Log.i("stopped", "stopped")
            else -> Log.i("nothing", "nothing")
        }
    }

    override fun onSurfacesCreated(vlcVout: IVLCVout?) {
        Log.e(TAG, "Surface Created")
        val sw = mSurface!!.width
        val sh = mSurface!!.height

        if (sw * sh == 0) {
            Log.e(TAG, "Invalid surface size")
            return
        }

        mediaPlayer!!.vlcVout.setWindowSize(sw, sh)
        mediaPlayer!!.scale = 0f
    }

    override fun onSurfacesDestroyed(vlcVout: IVLCVout?) {
        Log.e(TAG, "Surface Destroy")
        releasePlayer()
    }

    fun record() {
        Log.v("Screen Record", "-----------Trying to record------------")
        val dir = File("/storage/emulated/0/Download", "recording")

        val success = mediaPlayer?.record(dir.toString())

        if (success == true) {
            activity?.runOnUiThread {
                Toast.makeText(
                    activity,
                    "Recording",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            activity?.runOnUiThread {
                Toast.makeText(
                    activity,
                    "Nothing to record",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun stoprecord() {
        Log.v("Screen Record", "-----------Stop Record------------")
        mediaPlayer?.record(null)
        activity?.runOnUiThread {
            Toast.makeText(
                activity,
                "Recording stopped",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

