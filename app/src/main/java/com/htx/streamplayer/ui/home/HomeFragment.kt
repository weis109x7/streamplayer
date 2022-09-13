package com.htx.streamplayer.ui.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.htx.streamplayer.MainActivity
import com.htx.streamplayer.databinding.FragmentHomeBinding
import org.freedesktop.gstreamer.GStreamer


private const val TAG = "HomeFragmentActivity"

class HomeFragment : Fragment() , SurfaceHolder.Callback {
    private external fun nativeGetGStreamerInfo(): String? //Get Gstreamer version, a basic function to test if gstreamer is working or not
    private external fun nativeInit() // Initialize native code, build pipeline, etc
    private external fun nativeFinalize() // Destroy pipeline and shutdown native code
    private external fun nativePlay() // Set pipeline to PLAYING
    private external fun nativePause() // Set pipeline to PAUSED
    private external fun nativeSurfaceInit(surface: Any) // A new surface is available
    private external fun nativeSurfaceFinalize() // Surface about to be destroyed
    private external fun nativeSetPipeline(pipeline:String) // send pipeline to native code
    private val native_custom_data : Long = 0 // Native code will use this to keep private data
    private var is_playing_desired = true // Whether the user asked to go to PLAYING ( it will always be true as a livestream wont be stopped )


    companion object { //init gstreamer library
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


    private var recordToggle = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "Create View")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        innitPlayer()
        innitButtons()

        return root
    }

    private fun innitPlayer() {
        Log.i(TAG, "innitPlayer")

        //using shared preference to get gstreamer pipeline from settings fragment
        val sharedPreference = this.requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
        val streamURL = sharedPreference.getString("savedURL"," ")
        if (streamURL != null) {
            Log.i(TAG, "Pipeline sent to C code: $streamURL")
            // ww use this function to set the pipeline on the c side
            nativeSetPipeline(streamURL)
        }

        //init gstreamer player
        try {
            GStreamer.init(requireContext())
        } catch (e: Exception) {
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
        }
        val sv = binding.surfaceVideo
        val sh = sv.holder
        sh.addCallback(this)
        nativeInit()
    }

    private fun innitButtons() {
        Log.i(TAG, "InnitButtons")

        // joystick controls from "https://github.com/controlwear/virtual-joystick-android"
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

            val pstring = String.format("%.2f", p)
            val p1string = String.format("%.2f", p1)

            //send motor inputs through sockets every 0.5sec
            (activity as MainActivity).client?.write("$pstring#$p1string#")
            Log.i(TAG, "$p#$p1" )
        },500)



        //setup recording button
        val buttonRecord = binding.Record
        buttonRecord.setOnClickListener {
            if (!recordToggle){
                recordToggle = true
                buttonRecord.text = "Stop"
                //setup recording function
                //TODO
            }else{
                recordToggle = false
                buttonRecord.text = "Record"
                //setup stop recording function
                //TODO
            }
        }
    }

    //we use these two function to calculate motor signal of the robot
    //normalize function
    private fun normalize(value:Double, min:Double, max:Double, outmin:Double, outmax:Double): Double {
        return ( (value-min)/(max-min) * (outmax-outmin) +outmin)
    }
    //ensure range
    private fun ensureRange(value: Double, min: Double, max: Double): Double {
        return value.coerceAtLeast(min).coerceAtMost(max)
    }

    override fun onDestroyView() {
        Log.i(TAG, "View Destroyed")
        super.onDestroyView()

        //stop recording before we destroy the view
        if (recordToggle) {
            //setup stop recording function
            //TODO
            recordToggle=false
        }

        //release gstreamer surface
        nativeFinalize()

        _binding = null
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "on Pause")
    }

    override fun onResume() {
        Log.i(TAG, "on Resume")
        super.onResume()
    }


    // Called from native code. This sets the content of the TextView from the UI thread.
    private fun setMessage(message: String) {
        //display msg on a textview for user to understand what is going on in gstreamer
        val errorlog = binding.errormsglog
        requireActivity().runOnUiThread {
            errorlog.text = message
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("GStreamer", "Saving state, playing:$is_playing_desired")
        //save playing state when leaving HomeFragment, but in our case video will always be playing.
        outState.putBoolean("playing", is_playing_desired)
    }

    private fun onGStreamerInitialized() {
        Log.i("GStreamer", "Gst initialized. Restoring state, playing:$is_playing_desired")
        // Restore previous playing state, but in our case video will always be playing
        if (is_playing_desired) {
            nativePlay()
        } else {
            nativePause()
        }

    }

    //update video surface when aspect ratio is changed. eg, landscape/portrait view
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
