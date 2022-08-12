package com.htx.streamplayer.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.htx.streamplayer.MainActivity
import com.htx.streamplayer.R
import com.htx.streamplayer.databinding.FragmentHomeBinding
import java.io.File
import java.io.FileWriter


private const val TAG = "HomeFragmentActivity"

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var mSurface: SurfaceView? = null
    private var mFullSurface: SurfaceView? = null
    private var videoController: VideoController?=null

    private var streamURL:String?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        var sharedPreference = this.requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)

        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        mSurface = binding.surfaceView
        videoController= VideoController(requireActivity())
        videoController!!.mSurface=mSurface

        innitButtons()

        streamURL = sharedPreference.getString("savedURL"," ")
        streamURL?.let { videoController!!.createPlayer(it) }

        return root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun innitButtons() {
        val buttonup = binding.buttonup
        val buttondown = binding.buttondown
        val buttonleft = binding.buttonleft
        val buttonright = binding.buttonright

        // finding the edit text
        val directionText = binding.directionText

        //repeat while pressed
        buttonup.setOnTouchListener(RepeatListener(400, 400) {
            // the code to execute repeatedly
            Log.i(TAG, "forward")
            directionText.text = getString(R.string.forward)
            (activity as MainActivity).client?.write("FORWARD")
        })
        buttonup.setOnClickListener {
            Log.i(TAG, "STOP")
            directionText.text = "STOP"
            (activity as MainActivity).client?.write("STOP")
        }

        buttondown.setOnTouchListener(RepeatListener(400, 400) {
            // the code to execute repeatedly
            Log.i(TAG, "back")
            directionText.text = getString(R.string.back)
            (activity as MainActivity).client?.write("BACKWARD")
        })
        buttondown.setOnClickListener {
            Log.i(TAG, "STOP")
            directionText.text = "STOP"
            (activity as MainActivity).client?.write("STOP")
        }

        buttonleft.setOnTouchListener(RepeatListener(400, 400) {
            // the code to execute repeatedly
            Log.i(TAG, "left")
            directionText.text = getString(R.string.left)
            (activity as MainActivity).client?.write("LEFT")
        })
        buttonleft.setOnClickListener {
            Log.i(TAG, "STOP")
            directionText.text = "STOP"
            (activity as MainActivity).client?.write("STOP")
        }

        buttonright.setOnTouchListener(RepeatListener(400, 400) {
            // the code to execute repeatedly
            Log.i(TAG, "right")
            directionText.text = getString(R.string.right)
            (activity as MainActivity).client?.write("RIGHT")
        })
        buttonright.setOnClickListener {
            Log.i(TAG, "STOP")
            directionText.text = "STOP"
            (activity as MainActivity).client?.write("STOP")
        }

        val buttonRecord = binding.Record
        buttonRecord.setOnClickListener {
            activity?.runOnUiThread {
                videoController?.record()
            }
        }

        val buttonStop = binding.Snapshot
        buttonStop.setOnClickListener {
            videoController?.snapshot()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

//test func for storage
fun writeFileOnInternalStorage(mcoContext: Context, sFileName: String?, sBody: String?) {
    val dir = File("/storage/emulated/0/Download", "mydir")
    if (!dir.exists()) {
        dir.mkdir()
    }
    try {
        val gpxfile = File(dir, sFileName)
        val writer = FileWriter(gpxfile)
        writer.append(sBody)
        writer.flush()
        writer.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

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
