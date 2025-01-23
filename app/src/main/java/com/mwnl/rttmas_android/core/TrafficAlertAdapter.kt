package com.mwnl.rttmas_android.core

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.mwnl.rttmas_android.R
import com.mwnl.rttmas_android.models.TrafficAlert


class TrafficAlertAdapter(
    private val context: Activity,
    private val items: ArrayList<TrafficAlert>
) : RecyclerView.Adapter<TrafficAlertAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var icon            : ImageView         = itemView.findViewById(R.id.icon)
        var textTitle       : MaterialTextView  = itemView.findViewById(R.id.text_title)
        var textDescription : MaterialTextView  = itemView.findViewById(R.id.text_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = context.layoutInflater
        val rowView = layout.inflate(R.layout.listitem_traffic_alert, parent, false)
        return ViewHolder(rowView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

//        holder.croppedImage.setImageBitmap(item.bitmap)
        holder.textTitle.text = item.title
        holder.textDescription.text = item.description
    }

    override fun getItemCount() = items.size
}
