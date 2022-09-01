package com.htx.streamplayer.ui.home

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.htx.streamplayer.MainActivity
import com.htx.streamplayer.databinding.FragmentHomeBinding
import org.freedesktop.gstreamer.GStreamer


private const val TAG = "HomeFragmentActivity"

class HomeFragment : Fragment() , SurfaceHolder.Callback {
    private external fun nativeGetGStreamerInfo(): String?
    private external fun nativeInit() // Initialize native code, build pipeline, etc
    private external fun nativeFinalize() // Destroy pipeline and shutdown native code
    private external fun nativeSetUri(uri: String) // Set the URI of the media to play
    private external fun nativePlay() // Set pipeline to PLAYING
    private external fun nativeSetPosition(milliseconds: Int) // Seek to the indicated position, in milliseconds
    private external fun nativePause() // Set pipeline to PAUSED
    private external fun nativeSurfaceInit(surface: Any) // A new surface is available
    private external fun nativeSurfaceFinalize() // Surface about to be destroyed
    private val native_custom_data : Long = 0 // Native code will use this to keep private data
    private var is_playing_desired = true // Whether the user asked to go to PLAYING
    private val position = 0 // Current position, reported by native code
    private val duration = 0 // Current clip duration, reported by native code
    private val is_local_media = false // Whether this clip is stored locally or is being streamed
    private val desired_position = 0 // Position where the users wants to seek to
    private val mediaUri : String? = null // URI of the clip being played


    companion object {
        @JvmStatic
        private external fun nativeClassInit(): Boolean // Initialize native class: cache Method IDs for callbacks

        init {
            System.loadLibrary("gstreamer_android")
            System.loadLibrary("streamplayer")
            nativeClassInit()
        }
    }

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

//    private var videoController: VideoController?=null

    private var recordToggle = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "Create View")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        /* View model implementation if using
//        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
//        val textView: TextView = binding.textHome
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
*/

        try {
            GStreamer.init(requireContext())
        } catch (e: Exception) {
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
        }


        val tv = binding.textView2
        tv.text = "Welcome to " + nativeGetGStreamerInfo() + " !"

        val sv = binding.surfaceVideo
        val sh = sv.holder
        sh.addCallback(this)
        if (savedInstanceState != null) {
            Log.i("GStreamer", "Activity created. Saved state is playing:$is_playing_desired")
        } else {
            Log.i("GStreamer", "Activity created. There is no saved state, playing: false")
        }

        val play = binding.buttonPlay
        play.setOnClickListener {
            is_playing_desired = true
            nativePlay()
        }
        val pause = binding.buttonPause
        pause.setOnClickListener {
            is_playing_desired = false
            nativePause()
        }

        // Start with disabled buttons, until native code is initialized
        play.isEnabled = false
        pause.isEnabled = false
        nativeInit()

        innitPlayer()
        innitButtons()

        return root
    }

    private fun innitPlayer() {
        Log.i(TAG, "innitPlayer")
        val sharedPreference = this.requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
        val streamURL = sharedPreference.getString("savedURL"," ")

//        videoController= VideoController(requireActivity())
//        videoController!!.mSurface=binding.surfaceView

        //create player with media URL
//        streamURL?.let { videoController!!.createPlayer(it) }

    }

    private fun innitButtons() {
        Log.i(TAG, "InnitButtons")

        val joystick = binding.joystick
        joystick.setOnMoveListener ({ angle, strength ->
            //will keep running while joystick is pressed
            Log.i(TAG, "angle is $angle strength is $strength")
            val x = strength * kotlin.math.cos(Math.toRadians(angle.toDouble()))
            val y = strength * kotlin.math.sin(Math.toRadians(angle.toDouble()))
            Log.i(TAG, "cartesian X $x")
            Log.i(TAG, "cartesian Y $y")


            //convert x y values to servo motor inputs
            var p = y+x
            p = normalize(ensureRange(p,-100.0,100.0),-100.0,100.0,2.0,12.0)
            var p1 = y-x
            p1 = normalize(ensureRange(p1,-100.0,100.0),-100.0,100.0,12.0,2.0)
//            Log.i(TAG, p.toString())
//            Log.i(TAG, p1.toString())

            //send motor inputs through sockets
            (activity as MainActivity).client?.write("$p#$p1#")
            Log.i(TAG, "$p#$p1" )
        },500)


        //setup recording button
        val buttonRecord = binding.Record
        buttonRecord.setOnClickListener {
            if (!recordToggle){
                recordToggle = true
                buttonRecord.text = "Stop"
                activity?.runOnUiThread {
//                    videoController?.record()
                }
            }else{
                recordToggle = false
                buttonRecord.text = "Record"
                activity?.runOnUiThread {
//                    videoController?.stoprecord()
                }
            }
        }
    }


    override fun onDestroyView() {
        Log.i(TAG, "View Destroyed")
        super.onDestroyView()
        _binding = null

        if (recordToggle) {
//            videoController?.stoprecord()
            recordToggle=false
        }
//        videoController?.releasePlayer()

    }

    override fun onResume() {
        Log.i(TAG, "on Resume")
        super.onResume()

        //use to hide title bar to maximise screen space
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }

    //normalize function
    private fun normalize(value:Double, min:Double, max:Double, outmin:Double, outmax:Double): Double {
        return ( (value-min)/(max-min) * (outmax-outmin) +outmin)
    }

    //ensure range
    private fun ensureRange(value: Double, min: Double, max: Double): Double {
        return value.coerceAtLeast(min).coerceAtMost(max)
    }

    // Called from native code. This sets the content of the TextView from the UI thread.
    private fun setMessage(message: String) {
//        val tv = this.findViewById<View>(R.id.textview_message) as TextView
//        runOnUiThread(Runnable { tv.text = message })
        //TODO
    }

    // Called from native code
    private fun setCurrentPosition(position: Int, duration: Int) {
//        val sb = this.findViewById<View>(R.id.seek_bar) as SeekBar
//
//        // Ignore position messages from the pipeline if the seek bar is being dragged
//        if (sb.isPressed) return
//        runOnUiThread(Runnable {
//            sb.max = duration
//            sb.progress = position
//            updateTimeWidget()
//            sb.isEnabled = duration != 0
//        })
//        this.position = position
//        this.duration = duration
        //TODO
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("GStreamer", "Saving state, playing:$is_playing_desired")
        outState.putBoolean("playing", is_playing_desired)
    }

    private fun onGStreamerInitialized() {
        Log.i("GStreamer", "Gst initialized. Restoring state, playing:$is_playing_desired")
        // Restore previous playing state
        if (is_playing_desired) {
            nativePlay()
        } else {
            nativePause()
        }

        // Re-enable buttons, now that GStreamer is initialized
        requireActivity().runOnUiThread {
            binding.buttonPlay.isEnabled = true
            binding.buttonPause.isEnabled = true
        }
    }

    // Called from native code when the size of the media changes or is first detected.
    // Inform the video surface about the new size and recalculate the layout.
    private fun onMediaSizeChanged(width: Int, height: Int) {
        Log.i("GStreamer", "Media size changed to " + width + "x" + height)
//        val gsv: GStreamerSurfaceView =
//            this.findViewById<View>(R.id.surface_video) as GStreamerSurfaceView
//        gsv.media_width = width
//        gsv.media_height = height
//        runOnUiThread(Runnable { gsv.requestLayout() })
        //TODO
    }

    override fun surfaceChanged(
        holder: SurfaceHolder, format: Int, width: Int,
        height: Int
    ) {
        Log.d(
            "GStreamer", "Surface changed to format " + format + " width "
                    + width + " height " + height
        )
        nativeSurfaceInit(holder.surface)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d("GStreamer", "Surface created: " + holder.surface)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d("GStreamer", "Surface destroyed")
        nativeSurfaceFinalize()
    }
}
