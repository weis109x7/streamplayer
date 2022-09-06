package com.htx.streamplayer.ui.settings

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.htx.streamplayer.MainActivity
import com.htx.streamplayer.databinding.FragmentSettingsBinding
import kotlin.concurrent.thread

private const val TAG = "SettingsActivity"

class GalleryFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "on create view")

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        innitButtons()

        return root
    }


    override fun onPause() {
        super.onPause()
        Log.i(TAG, "on Pause")

        //Save text box contents when leaving the page
        val sharedPreference = this.requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putString("savedURL",binding.etURL.text.toString())
        editor.putString("savedSocket",binding.socketURL.text.toString())
        editor.apply()

        _binding = null
    }

    private fun innitButtons() {
        Log.i(TAG, "innit buttons")

        //Restore text box from previous session
        val sharedPreference = this.requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
        val editTextView = binding.etURL
        editTextView.setText(sharedPreference.getString("savedURL",""))
        val socketUrl=binding.socketURL
        socketUrl.setText(sharedPreference.getString("savedSocket",""))

        val connectBtn = binding.connectBtn
        connectBtn.setOnClickListener {
            thread (start = true ) {
                try {
                    //get socket url
                    val socketString = socketUrl.text.toString().split(":")
                    val addr = socketString[0]
                    val port = socketString[1].toInt()
                    //create socket
                    (activity as MainActivity).client = SocketClient(addr,port)

                    //show toast if success
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "connected" + socketUrl.text.toString(),
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                } catch (e: Exception) {
                    Log.i(TAG, "failed to create socket")
                    Log.i(TAG, e.toString())
                    e.printStackTrace()

                    //show toast if failed
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

        /* test function to send msg
//        val clientSendBtn = binding.clientSendBtn
//        clientSendBtn.setOnClickListener {
//            (activity as MainActivity).client?.write("test string")
//        }
         */

        val closeConnectionBtn = binding.closeConnectionBtn
        closeConnectionBtn.setOnClickListener {
            (activity as MainActivity).client?.closeConnection()

            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "closing connection",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }

    override fun onDestroyView() {
        Log.i(TAG, "view destroy")
        super.onDestroyView()
        _binding = null
    }
}