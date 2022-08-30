package com.htx.streamplayer.ui.home

import android.R
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.htx.streamplayer.MainActivity
import com.htx.streamplayer.databinding.FragmentHomeBinding


private const val TAG = "HomeFragmentActivity"

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private var videoController: VideoController?=null

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
        innitPlayer()
        innitButtons()

        return root
    }

    private fun innitPlayer() {
        Log.i(TAG, "innitPlayer")
        val sharedPreference = this.requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
        val streamURL = sharedPreference.getString("savedURL"," ")

        videoController= VideoController(requireActivity())
        videoController!!.mSurface=binding.surfaceView

        //create player with media URL
        streamURL?.let { videoController!!.createPlayer(it) }

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
                binding.Record.text = "Stop"
                activity?.runOnUiThread {
                    videoController?.record()
                }
            }else{
                recordToggle = false
                binding.Record.text = "Record"
                activity?.runOnUiThread {
                    videoController?.stoprecord()
                }
            }
        }
    }


    override fun onDestroyView() {
        Log.i(TAG, "View Destroyed")
        super.onDestroyView()
        _binding = null

        if (recordToggle) {
            videoController?.stoprecord()
            recordToggle=false
        }
        videoController?.releasePlayer()

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
}
