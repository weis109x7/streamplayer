package com.htx.streamplayer.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.htx.streamplayer.MainActivity
import com.htx.streamplayer.ObjectDetectorHelper
import com.htx.streamplayer.databinding.FragmentHomeBinding
import org.freedesktop.gstreamer.GStreamer
import org.tensorflow.lite.task.gms.vision.detector.Detection
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

private const val TAG = "HomeFragmentActivity"

class HomeFragment : Fragment() , ObjectDetectorHelper.DetectorListener {
    private external fun nativeGetGStreamerInfo(): String? //Get Gstreamer version, a basic function to test if gstreamer is working or not
    private external fun nativeInit() // Initialize native code, build pipeline, etc
    private external fun nativeFinalize() // Destroy pipeline and shutdown native code
    private external fun nativePlay() // Set pipeline to PLAYING
    private external fun nativePause() // Set pipeline to PAUSED
    private external fun nativeSurfaceInit(surface: Any) // A new surface is available
    private external fun nativeSurfaceFinalize() // Surface about to be destroyed
    private external fun nativeSetPipeline(pipeline:String) // send pipeline to native code
    private val native_custom_data : Long = 0 // Native code will use this to keep private data


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
    private lateinit var objectDetectorHelper: ObjectDetectorHelper

    private var recordToggle = false
    private var objToggle = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "on Create View")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //init object detection model
        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this)

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
            // we use this function to set the pipeline on the c side
            nativeSetPipeline(streamURL)
        }

        //init gstreamer player
        try {
            GStreamer.init(requireContext())
        } catch (e: Exception) {
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
        }
        nativeInit()

        val sv = binding.surfaceVideo
        //texturelistener to deal with the init and destruction of the gstreamer video surface
        sv.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                Log.d("GStreamer", "Surface created: $surface with with of $width + height of $height")
                val mSurface = Surface(surface)
                nativeSurfaceInit(mSurface) //init surface for gstreamer to display video
            }

            //update video surface when aspect ratio is changed. eg, landscape/portrait view
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                Log.d(
                    "GStreamer", "Surface changed to format " + "format" + " width "
                            + width + " height " + height
                )
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.d("GStreamer", "Surface destroyed")
                nativeSurfaceFinalize() //release gstreamer surface
                return true
            }

//            this function is called on evey frame update of the surface
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                //if object detection is turned on. run object detection function
                if (objToggle) {
                    //run on every frame
                    detectObj(sv)
                }
            }
        }
    }

    private fun innitButtons() {
        Log.i(TAG, "InnitButtons")

        // joystick controls from "https://github.com/controlwear/virtual-joystick-android"
        val joystick = binding.joystick
        joystick.setOnMoveListener ({ angle, strength ->
            //will keep running while joystick is pressed
            Log.i(TAG, "angle is $angle strength is $strength")
            var x = strength * kotlin.math.cos(Math.toRadians(angle.toDouble()))
            var y = strength * kotlin.math.sin(Math.toRadians(angle.toDouble()))
            Log.i(TAG, "cartesian X $x")
            Log.i(TAG, "cartesian Y $y")

            //convert x y values to servo motor inputs
            var leftmotor = y+x
            leftmotor = normalize(ensureRange(leftmotor,-99.0,99.0),-100.0,100.0,2.0,12.0)
            var rightmotor = y-x
            rightmotor = normalize(ensureRange(rightmotor,-99.0,99.0),-100.0,100.0,12.0,2.0)

            val leftmotorstring = String.format("%.2f", leftmotor)
            val rightmotorstring = String.format("%.2f", rightmotor)

            //send motor inputs through sockets every 0.5sec
            (activity as MainActivity).client?.write("$leftmotorstring#$rightmotorstring#")
            Log.i(TAG, "$leftmotorstring#$rightmotorstring#" )
        },500)



        //setup recording button
        //TODO for now recording button only takes a snapshot everytime it turns on.
        val buttonRecord = binding.Record
        buttonRecord.setOnClickListener {
            if (!recordToggle){
                buttonRecord.text = "Stop"
                //start record
                val sv = binding.surfaceVideo
                startRecord(sv)
            }else{
                buttonRecord.text = "Record"
                //stop record
                stopRecord()
            }
        }

        //setup object detect button
        val buttonObjDetect = binding.ObjDetect
        buttonObjDetect.setOnClickListener {
            if (!objToggle){
                buttonObjDetect.text = "Obj Off"
                //start detect
                startDetect()
            }else{
                buttonObjDetect.text = "Obj On"
                //stop detect
                stopDetect()
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

    private fun startRecord(textureView: TextureView){
        recordToggle=true
        //TODO
        storeImage(textureView.bitmap!!)
//        objectDetectorHelper.detect(textureView.bitmap!!, 0)
    }

    private fun storeImage(image: Bitmap) {
        val pictureFile: File? = getOutputMediaFile()
        if (pictureFile == null) {
            Log.d(
                TAG,
                "Error creating media file, check storage permissions: "
            ) // e.getMessage());
            return
        }
        try {
            val fos = FileOutputStream(pictureFile)
            image.compress(Bitmap.CompressFormat.PNG, 90, fos)
            fos.close()
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "File not found: " + e.message)
        } catch (e: IOException) {
            Log.d(TAG, "Error accessing file: " + e.message)
        }
    }

    /** Create a File for saving an image or video  */
    private fun getOutputMediaFile(): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        val mediaStorageDir = File(
            "/storage/emulated/0/Download/mydir/"
        )

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        // Create a media file name
        val timeStamp: String = System.currentTimeMillis().toString()
        val mediaFile: File
        val mImageName = "MI_$timeStamp.png"
        mediaFile = File(mediaStorageDir.path + File.separator + mImageName)
        return mediaFile
    }

    private fun stopRecord(){
        recordToggle=false
        //TODO
    }

    private fun detectObj(textureView: TextureView){
        //convert texture view to bitmap, send bitmap to object detection
        objectDetectorHelper.detect(textureView.bitmap!!, 0)
    }

    private fun startDetect(){
        objToggle=true
    }
    private fun stopDetect(){
        objToggle=false

        activity?.runOnUiThread {
            //clear overlay when detection is stopped
            binding.overlay.setResults(
                null ?: LinkedList<Detection>(),
                0,
                0
            )
            // Force a redraw
            binding.overlay.invalidate()
        }

    }

    // Called from native code. This sets the content of the TextView from the UI thread.
    private fun setMessage(message: String) {
        //display msg on a textview for user to understand what is going on in gstreamer
        val errorlog = binding.errormsglog
        requireActivity().runOnUiThread {
            errorlog.text = message
        }
    }

    override fun onDestroyView() {
        Log.i(TAG, "View Destroyed")
        super.onDestroyView()

        //stop recording and object detection before we destroy the view
        if (recordToggle) {
            stopRecord()
            stopDetect()
        }
        //release gstreamer
        nativeFinalize()

        //gstreamer surface destruction is handled by surfaceTextureListener

        _binding = null
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "on Pause")
    }

    override fun onResume() {
        Log.i(TAG, "on Resume")
        super.onResume()

        //using shared preference to get selected model
        val sharedPreference = this.requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
        try {
            objectDetectorHelper.currentModel = sharedPreference.getString("savedModel","0")?.toInt()!!
            objectDetectorHelper.clearObjectDetector()
            Log.e(TAG, "selected model: " + objectDetectorHelper.currentModel)
        } catch (e: Exception) {
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
//        Log.d("GStreamer", "Saving state, playing:$is_playing_desired")
    }

    private fun onGStreamerInitialized() {
//        Log.i("GStreamer", "Gst initialized. Restoring state, playing:$is_playing_desired")
        // Start playing everytime Gstreamer is initialized
        nativePlay()
    }

    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        Log.i("objectdetect", "Results done")
        Log.i("objectdetect", "width $imageHeight + height $imageWidth + inference time $inferenceTime")
        Log.i("objectdetect", "$results")

        //send results to be displayed on errorlog for debugging
        val errorlog = binding.errormsglog
        errorlog.text = results.toString()

        activity?.runOnUiThread {
            // Pass necessary information to OverlayView for drawing on the canvas
            binding.overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )

            // Force a redraw
            binding.overlay.invalidate()
        }
    }

    //object detection error
    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInitialized() {
        Log.i(TAG, "object detector inited")
        objectDetectorHelper.setupObjectDetector()
    }
}
