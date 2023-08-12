package com.example.note_taker

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity(), DrugItemClickListener {

    private val drugs = listOf("Amp","Ecstasy","Weed","Coke","Hero","Meth", "Lsd", "Dmt", "Shrooms")
    private lateinit var drugsQuantities: ArrayList<DrugsQuantity>
    private lateinit var adapter: DrugsCustomAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drugsQuantities = ArrayList()
        for(i in drugs) drugsQuantities.add(DrugsQuantity(i, listOf("0")))

        adapter = DrugsCustomAdapter(drugsQuantities, this)

        val recyclerview = findViewById<RecyclerView>(R.id.recycle)

        recyclerview.layoutManager = LinearLayoutManager(this)

        recyclerview.adapter = adapter


//        val add: FloatingActionButton = findViewById(R.id.add)
//
//        add.setOnClickListener {
//            Toast.makeText(this, "HI", Toast.LENGTH_SHORT).show()
//        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.delete -> {
                for(i in drugsQuantities) i.quantity = listOf("0")
                adapter.notifyDataSetChanged()

            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun showDialog(title: String, qty: String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)


        val input = EditText(this)
        input.hint = "Enter Text"
        input.inputType = InputType.TYPE_CLASS_DATETIME
        input.setText(qty)
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val newQty = input.text.toString()
            val index = drugs.indexOf(title)
            drugsQuantities[index] = DrugsQuantity(title, newQty.split(" "))
            adapter.notifyItemChanged(index)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }


    override fun onClick(data:DrugsQuantity) {
        showDialog(data.name, data.quantity.joinToString(" "))
    }
}