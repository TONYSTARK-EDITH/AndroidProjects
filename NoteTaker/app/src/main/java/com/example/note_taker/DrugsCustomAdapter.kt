package com.example.note_taker

import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class DrugsCustomAdapter(private val mList: List<DrugsQuantity>,private val click: DrugItemClickListener) : RecyclerView.Adapter<DrugsCustomAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycle_view_item, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val drugsQuantity = mList[position]
        holder.drugName.text = drugsQuantity.name
        holder.drugQty.text = drugsQuantity.quantity.joinToString(" ")
        holder.itemView.setOnClickListener {
            click.onClick(drugsQuantity)
        }

    }


    override fun getItemCount(): Int {
        return mList.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val drugName: TextView = itemView.findViewById(R.id.drugName)
        val drugQty: TextView = itemView.findViewById(R.id.drugQty)
    }
}