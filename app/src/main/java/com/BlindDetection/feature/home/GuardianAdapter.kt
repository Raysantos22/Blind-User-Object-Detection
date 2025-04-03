package com.BlindDetection.feature.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.BlindDetection.R

/**
 * RecyclerView adapter for displaying and managing guardians
 */
class GuardianAdapter(
    private var guardians: MutableList<Guardian>,
    private var onEdit: (Guardian) -> Unit,
    private var onDelete: (Guardian) -> Unit
) : RecyclerView.Adapter<GuardianAdapter.GuardianViewHolder>() {

    /**
     * Update the list of guardians
     */
    fun updateList(newGuardians: List<Guardian>) {
        guardians.clear()
        guardians.addAll(newGuardians)
        notifyDataSetChanged()
    }

    /**
     * Set the edit listener
     */
    fun setOnEditListener(listener: (Guardian) -> Unit) {
        onEdit = listener
    }

    /**
     * Set the delete listener
     */
    fun setOnDeleteListener(listener: (Guardian) -> Unit) {
        onDelete = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuardianViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_guardian, parent, false)
        return GuardianViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuardianViewHolder, position: Int) {
        holder.bind(guardians[position])
    }

    override fun getItemCount(): Int = guardians.size

    inner class GuardianViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.guardianName)
        private val phoneTextView: TextView = itemView.findViewById(R.id.guardianPhone)
        private val editButton: ImageButton = itemView.findViewById(R.id.editGuardianButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteGuardianButton)

        fun bind(guardian: Guardian) {
            nameTextView.text = guardian.name
            phoneTextView.text = guardian.phoneNumber

            editButton.setOnClickListener {
                onEdit(guardian)
            }

            deleteButton.setOnClickListener {
                onDelete(guardian)
            }
        }
    }
}