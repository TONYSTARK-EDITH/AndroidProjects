package com.example.passwordsaver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.passwordsaver.database.PasswordData
import com.example.passwordsaver.database.PasswordViewModel


class PasswordAdapter(
    private val passwords: MutableList<PasswordData>,
    private val db: PasswordViewModel
) :
    RecyclerView.Adapter<PasswordAdapter.ViewHolder>() {

    //declare interface
    private lateinit var onClick: OnItemClicked

    //make interface like this
    interface OnItemClicked {
        fun onItemClick(position: Int, username: String)
    }


    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val textView: TextView = itemView.findViewById(R.id.username)
        val copy: ImageView = itemView.findViewById(R.id.copy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.password_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val username = passwords[position].username
        holder.textView.text = username
        holder.copy.setOnClickListener { onClick.onItemClick(position, username) }
    }

    fun permanentRemove(passwordData: PasswordData, position: Int) {
        db.delete(passwordData)
        notifyItemRemoved(position)
    }

    fun removeAt(position: Int) {
        passwords.removeAt(position)
        notifyItemRemoved(position)
    }

    fun restore(position: Int, passwordData: PasswordData) {
        passwords.add(position, passwordData)
        notifyItemInserted(position)
    }


    fun getItem(position: Int): PasswordData {
        return passwords[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return passwords.size
    }

    fun setOnClick(onClick: OnItemClicked) {
        this.onClick = onClick
    }
}