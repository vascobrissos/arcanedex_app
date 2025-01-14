package pt.ipt.arcanedex_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Classe de dados que representa uma entidade Arcane.
 * @property id Identificador único para cada Arcane.
 * @property name Nome do Arcane.
 * @property description Descrição opcional do Arcane.
 * @property image URL opcional da imagem associada ao Arcane.
 */
data class Arcane(
    val id: Int,
    val name: String,
    val description: String?,
    val image: String?
)

/**
 * Adapter para exibir uma lista de objetos do tipo Arcane num RecyclerView.
 * @param arcanes Lista mutável de objetos do tipo Arcane.
 * @param onEditClick Callback chamado ao clicar no botão de edição, recebendo o objeto Arcane e a posição.
 */
class ArcaneAdapter(
    private val arcanes: MutableList<Arcane>,
    private val onEditClick: (Arcane, Int) -> Unit
) : RecyclerView.Adapter<ArcaneAdapter.ArcaneViewHolder>() {

    /**
     * ViewHolder responsável por armazenar as referências às views de cada item na lista.
     * @param itemView A view correspondente a um item no RecyclerView.
     */
    inner class ArcaneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val arcaneName: TextView = itemView.findViewById(R.id.arcaneName)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
    }

    /**
     * Cria e infla o layout para os itens do RecyclerView.
     * @param parent O ViewGroup ao qual a nova View será adicionada.
     * @param viewType O tipo da nova View (não utilizado neste caso).
     * @return Um ArcaneViewHolder contendo a View inflada.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArcaneViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_arcane, parent, false)
        return ArcaneViewHolder(view)
    }

    /**
     * Liga os dados do objeto Arcane às views no ViewHolder.
     * @param holder O ViewHolder que será configurado.
     * @param position A posição do item na lista.
     */
    override fun onBindViewHolder(holder: ArcaneViewHolder, position: Int) {
        val arcane = arcanes[position]
        holder.arcaneName.text = arcane.name

        // Aciona o callback ao clicar no botão de edição
        holder.editButton.setOnClickListener { onEditClick(arcane, position) }
    }

    /**
     * Retorna o número total de itens na lista.
     * @return O número de itens na lista.
     */
    override fun getItemCount(): Int = arcanes.size
}
