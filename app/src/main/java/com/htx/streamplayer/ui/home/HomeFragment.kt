package com.htx.streamplayer.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.htx.streamplayer.MainActivity
import com.htx.streamplayer.databinding.FragmentHomeBinding
import java.util.*

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
        /* wsad implementation
//        updownleftright buttons
//        val buttonup = binding.buttonup
//        val buttondown = binding.buttondown
//        val buttonleft = binding.buttonleft
//        val buttonright = binding.buttonright

        // finding the edit text
//        val directionText = binding.directionText

        //repeat while pressed
//        buttonup.setOnTouchListener(RepeatListener(400, 400) {
//            // the code to execute repeatedly
//            Log.i(TAG, "forward")
//            directionText.text = getString(R.string.forward)
//            (activity as MainActivity).client?.write("FORWARD")
//        })
//
//        buttondown.setOnTouchListener(RepeatListener(400, 400) {
//            // the code to execute repeatedly
//            Log.i(TAG, "back")
//            directionText.text = getString(R.string.back)
//            (activity as MainActivity).client?.write("BACKWARD")
//        })
//
//        buttonleft.setOnTouchListener(RepeatListener(400, 400) {
//            // the code to execute repeatedly
//            Log.i(TAG, "left")
//            directionText.text = getString(R.string.left)
//            (activity as MainActivity).client?.write("LEFT")
//        })
//
//        buttonright.setOnTouchListener(RepeatListener(400, 400) {
//            // the code to execute repeatedly
//            Log.i(TAG, "right")
//            directionText.text = getString(R.string.right)
//            (activity as MainActivity).client?.write("RIGHT")
//
//        })
         */

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

        val button1 = binding.button1
        button1.setOnClickListener{
            val intent = Intent("pl.effisoft.rpicamviewer2.PREVIEW")
            val camera = 0

            //--------- Basic settings
            intent.putExtra("full_screen", true)
            intent.putExtra("name$camera", "My pipeline name")
            intent.putExtra("host$camera", "192.168.0.1")
            intent.putExtra("port$camera", 5000)
            intent.putExtra("description$camera", "My pipeline description")
            intent.putExtra("uuid$camera", UUID.randomUUID().toString())
            intent.putExtra("aspectRatio$camera", 1.6)
            intent.putExtra("autoplay$camera", true)

            //--------- Enable advanced mode
            intent.putExtra("advanced$camera", true)
            intent.putExtra("custom_pipeline$camera", "tcpclientsrc host=192.168.207.101 port=5001 ! gdpdepay !rtph264depay ! avdec_h264 ! videoconvert !autovideosink sync=false")

            //--------- Enable application extra features
            intent.putExtra("extraFeaturesEnabled$camera", false)

            //--------- Add autoaudiosink to featured pipeline
            intent.putExtra("extraFeaturesSoundEnabled$camera", false)

            //--------- Scale Video Stream option
            intent.putExtra("extraResizeVideoEnabled$camera", false)
            intent.putExtra("width$camera", 320)
            intent.putExtra("height$camera", 200)

            //--------- Add plugins
            val plugins = ArrayList<String>()
            intent.putExtra("plugins$camera", plugins)
            intent.setPackage("pl.effisoft.rpicamviewer2")
            startActivityForResult(intent, 0)
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

//test func for storage
//fun writeFileOnInternalStorage(mcoContext: Context, sFileName: String?, sBody: String?) {
//    val dir = File("/storage/emulated/0/Download", "mydir")
//    if (!dir.exists()) {
//        dir.mkdir()
//    }
//    try {
//        val gpxfile = File(dir, sFileName)
//        val writer = FileWriter(gpxfile)
//        writer.append(sBody)
//        writer.flush()
//        writer.close()
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
//}

/* Unused repeat listener for wsad movement
class RepeatListener(
    initialInterval: Int, normalInterval: Int,
    clickListener: View.OnClickListener?
) : View.OnTouchListener {
    private val handler = Handler()
    private val initialInterval: Int
    private val normalInterval: Int
    private val clickListener: View.OnClickListener
    private var touchedView: View? = null
    private val handlerRunnable: Runnable = object : Runnable {
        override fun run() {
            if (touchedView!!.isEnabled) {
                handler.postDelayed(this, normalInterval.toLong())
                clickListener!!.onClick(touchedView)
            } else {
                // if the view was disabled by the clickListener, remove the callback
                handler.removeCallbacks(this)
                touchedView!!.isPressed = false
                touchedView = null
            }
        }
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacks(handlerRunnable)
                handler.postDelayed(handlerRunnable, initialInterval.toLong())
                touchedView = view
                touchedView!!.isPressed = true
                clickListener.onClick(view)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(handlerRunnable)
                touchedView!!.isPressed = false
                touchedView = null
                return true
            }
        }
        return false
    }

    /**
     * @param initialInterval The interval after first click event
     * @param normalInterval The interval after second and subsequent click
     * events
     * @param clickListener The OnClickListener, that will be called
     * periodically
     */
    init {
        requireNotNull(clickListener) { "null runnable" }
        require(!(initialInterval < 0 || normalInterval < 0)) { "negative interval" }
        this.initialInterval = initialInterval
        this.normalInterval = normalInterval
        this.clickListener = clickListener
    }
}
 */
