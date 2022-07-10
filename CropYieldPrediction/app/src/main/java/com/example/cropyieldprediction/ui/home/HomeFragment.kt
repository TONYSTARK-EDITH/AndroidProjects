package com.example.cropyieldprediction.ui.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.Fragment
import com.beust.klaxon.Klaxon
import com.example.cropyieldprediction.R
import com.example.cropyieldprediction.RestApi
import com.example.cropyieldprediction.databinding.FragmentHomeBinding
import okhttp3.*
import java.io.IOException
import java.util.concurrent.CountDownLatch


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!
    private val client = OkHttpClient()
    private val BASEURL = "crop-dlt.herokuapp.com"
    private val SCHEME = "https"
    private val PATH = "apipredict"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val items = listOf("Ha", "Acre", "Cent", "Square Feet")
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, items)
        (binding.options.editText as? AutoCompleteTextView)?.setAdapter(adapter)
        val crops = listOf(
            "Barley", "Cotton", "Ground Nuts", "Maize", "Millets", "Oil seeds",
            "Paddy", "Pulses", "Sugarcane", "Tobacco", "Wheat"
        )
        val newAd = ArrayAdapter(requireContext(), R.layout.list_item, crops)
        (binding.c.editText as? AutoCompleteTextView)?.setAdapter(newAd)

        try {


            val crop = binding.c.editText as AutoCompleteTextView
            val unit = binding.options.editText as AutoCompleteTextView
            var cropSelected = "Barley"
            var unitSelected = "Ha"

            crop.onItemClickListener = OnItemClickListener { _, _, position, _ ->
                cropSelected = newAd.getItem(position).toString()
            }

            unit.onItemClickListener = OnItemClickListener { _, _, position, _ ->
                unitSelected = adapter.getItem(position).toString()
            }

            binding.predict.setOnClickListener {
                val countDownLatch = CountDownLatch(1)
                val n = binding.n.editText?.text.toString()
                val p = binding.p.editText?.text.toString()
                val k = binding.k.editText?.text.toString()
                val area = binding.a.editText?.text.toString()
                val rain = binding.r.editText?.text.toString()
                val months = binding.g.editText?.text.toString()
                val temp = binding.t.editText?.text.toString()
                val moist = binding.m.editText?.text.toString()
                if (n.check() || p.check() || k.check() || rain.check(10.0) ||
                    temp.check(16.0, 100.0) || moist.check() || months.check(max = 12.0)
                    || unitSelected.check(isDouble = false) || cropSelected.check(isDouble = false)
                ) {
                    Toast.makeText(
                        requireContext(),
                        "Please fill all the fields",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                lateinit var out: String
                val url = HttpUrl.Builder().scheme(SCHEME).host(BASEURL).addPathSegment(PATH)
                    .addQueryParameter("n", n)
                    .addQueryParameter("p", p)
                    .addQueryParameter("k", k)
                    .addQueryParameter("rain", rain)
                    .addQueryParameter("month", months)
                    .addQueryParameter("moist", moist)
                    .addQueryParameter("temp", temp)
                    .addQueryParameter("area", "$area$unitSelected")
                    .addQueryParameter("crop", cropSelected).build()

                fun run() {
                    val request = Request.Builder()
                        .url(url)
                        .build()
                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            e.printStackTrace()
                        }

                        override fun onResponse(call: Call, response: Response) {
                            response.use {
                                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                                out = response.body()!!.string()
                                countDownLatch.countDown()

                            }
                        }
                    })

                }
                run()
                countDownLatch.await()
                val result = Klaxon().parse<RestApi>(out)
                result?.pred?.let { it1 -> showDialog(it1) }
            }
        } catch (e: Exception) {
            e.message?.let { showDialog(it) }
        }


        return root
    }


    private fun showDialog(title: String) {
        val dialog = activity?.let { Dialog(it) }
        dialog?.setCancelable(false)
        dialog?.setContentView(R.layout.custom_layout)
        val body = dialog?.findViewById(R.id.message_box_content) as TextView
        body.text = title
        val yesBtn = dialog.findViewById(R.id.ok) as Button
        yesBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

    }

    private fun String.check(
        min: Double = 1.0,
        max: Double = Double.MAX_VALUE,
        isDouble: Boolean = true
    ): Boolean {
        return if (isDouble) (this.isBlank() || this.isEmpty() || this.toDouble() < min || this.toDouble() > max) else (this.isEmpty() || this.isBlank())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}