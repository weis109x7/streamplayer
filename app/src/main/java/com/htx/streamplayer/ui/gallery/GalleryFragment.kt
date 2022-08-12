package com.htx.streamplayer.ui.gallery

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.htx.streamplayer.MainActivity
import com.htx.streamplayer.databinding.FragmentGalleryBinding
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread


class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val sharedPreference = this.requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)

        val galleryViewModel =
            ViewModelProvider(this).get(GalleryViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textGallery
        galleryViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        val editTextView = binding.etURL
        editTextView.setText(sharedPreference.getString("savedURL"," "))

        innitButtons()

        return root
    }


    override fun onPause() {
        super.onPause()
        val sharedPreference = this.requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putString("savedURL",binding.etURL.text.toString())
        editor.putString("savedSocket",binding.socketURL.text.toString())
        editor.apply()
        _binding = null
    }

    private fun innitButtons() {
        val sharedPreference = this.requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)

        var addr:String
        var port:Int

        val socketUrl=binding.socketURL
        socketUrl.setText(sharedPreference.getString("savedSocket"," "))

        val connectBtn = binding.connectBtn
        connectBtn.setOnClickListener {
            thread (start = true ) {
                try {
                    addr=socketUrl.text.toString().split(":")[0]
                    port = socketUrl.text.toString().split(":")[1].toInt()
                    (activity as MainActivity).client = SocketClient(addr,port)

                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "connected" + socketUrl.text.toString(),
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                } catch (e: IOException) {
                    println("failed to create socket")
//                    e.printStackTrace()
                    println(e.toString())

                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "failed " + socketUrl.text.toString(),
                            Toast.LENGTH_SHORT
                        ).show()

                    }

                }
            }
        }

        val clientSendBtn = binding.clientSendBtn
        clientSendBtn.setOnClickListener {
            (activity as MainActivity).client?.write("test string")
        }
        val closeConnectionBtn = binding.closeConnectionBtn
        closeConnectionBtn.setOnClickListener {
            (activity as MainActivity).client?.closeConnection()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}