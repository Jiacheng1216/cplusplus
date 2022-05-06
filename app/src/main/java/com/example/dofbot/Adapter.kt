package com.example.dofbot

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class Adapter(private val data: ArrayList<Contact>):
    RecyclerView.Adapter<Adapter.ViewHolder>(){
        class ViewHolder(v: View): RecyclerView.ViewHolder(v){
            val home_name = v.findViewById<TextView>(R.id.home_name)
            val home_id = v.findViewById<TextView>(R.id.home_id)
            val homeSelection = v.findViewById<ImageView>(R.id.homeSelection)
        }

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int):
        ViewHolder {
        val v = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.homeview,viewGroup,false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.home_name.text = data[position].name
        holder.home_id.text = data[position].id
        holder.homeSelection.setOnClickListener {
           //alertDialog(0)
        }
    }

    fun alertDialog(context: Context){
        val item = arrayOf("進入","刪除該家庭")
        AlertDialog.Builder(context)
            .setItems(item) {dialogInterface, i ->
                when(i){
                    1 -> return@setItems

                }

            }

    }
    }
