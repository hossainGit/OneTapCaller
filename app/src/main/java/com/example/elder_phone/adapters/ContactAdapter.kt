package com.example.elder_phone.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.elder_phone.R
import com.example.elder_phone.data.Contact
import com.google.android.material.imageview.ShapeableImageView
import coil.load

class ContactAdapter(
    private val onContactClick: (Contact) -> Unit,
    private val isEditMode: Boolean = false
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = getItem(position)
        holder.bind(contact)
        holder.itemView.setOnClickListener { onContactClick(contact) }
    }

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contactImage: ShapeableImageView = itemView.findViewById(R.id.contact_image)
        private val contactName: TextView = itemView.findViewById(R.id.contact_name)

        fun bind(contact: Contact) {
            contactName.text = contact.name

            // Load image with Coil
            contactImage.load(contact.photoUri) {
                placeholder(R.drawable.ic_avatar_placeholder)
                error(R.drawable.ic_avatar_placeholder)
                crossfade(true)
            }

            // Add visual affordance for edit mode
            if (isEditMode) {
                contactImage.alpha = 0.8f
            } else {
                contactImage.alpha = 1.0f
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }
}