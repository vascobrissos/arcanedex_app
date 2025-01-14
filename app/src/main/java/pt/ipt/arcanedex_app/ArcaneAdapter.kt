package pt.ipt.arcanedex_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Arcane(
    val id: Int, // Unique identifier for each arcane
    val name: String,
    val description: String?,
    val image: String?
)

class ArcaneAdapter(
    private val arcanes: MutableList<Arcane>, // Use the updated Arcane data class
    private val onEditClick: (Arcane, Int) -> Unit // Callback with the full Arcane object
) : RecyclerView.Adapter<ArcaneAdapter.ArcaneViewHolder>() {

    inner class ArcaneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val arcaneName: TextView = itemView.findViewById(R.id.arcaneName)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArcaneViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_arcane, parent, false)
        return ArcaneViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArcaneViewHolder, position: Int) {
        val arcane = arcanes[position]
        holder.arcaneName.text = arcane.name

        // Trigger the callback with the Arcane object and position when edit is clicked
        holder.editButton.setOnClickListener { onEditClick(arcane, position) }
    }

    override fun getItemCount(): Int = arcanes.size
}
