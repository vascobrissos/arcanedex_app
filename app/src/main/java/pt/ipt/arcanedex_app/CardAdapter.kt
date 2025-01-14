package pt.ipt.arcanedex_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import pt.ipt.arcanedex_app.data.CardItem

/**
 * Adapter responsável por exibir uma lista de objetos do tipo CardItem num RecyclerView.
 * @param items Lista mutável de itens do tipo CardItem.
 * @param onItemClick Callback chamado ao clicar num item.
 * @param onFavoriteToggle Callback chamado ao alternar o estado de favorito de um item.
 * @param showFavorites Indica se o botão de favoritos deve ser exibido.
 */
class CardAdapter(
    private val items: MutableList<CardItem>,
    private val onItemClick: (CardItem) -> Unit,
    private val onFavoriteToggle: (CardItem) -> Unit,
    private val showFavorites: Boolean
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    /**
     * ViewHolder responsável por armazenar as referências às views de cada item da lista.
     * @param itemView A view correspondente a um item do RecyclerView.
     */
    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleCard)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val favoriteButton: ImageView? = itemView.findViewById(R.id.favoriteIcon)
    }

    /**
     * Cria e infla o layout para os itens do RecyclerView.
     * @param parent O ViewGroup ao qual a nova View será adicionada.
     * @param viewType O tipo da nova View (não utilizado neste caso).
     * @return Um CardViewHolder contendo a View inflada.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    /**
     * Liga os dados do objeto CardItem às views no ViewHolder.
     * @param holder O ViewHolder que será configurado.
     * @param position A posição do item na lista.
     */
    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val cardItem = items[position]

        // Define o título e a imagem do CardItem
        holder.titleTextView.text = cardItem.Name
        Glide.with(holder.itemView.context)
            .load(cardItem.Img)
            .into(holder.imageView)

        // Configura o botão de favorito, caso esteja habilitado
        if (showFavorites) {
            holder.favoriteButton?.visibility = View.VISIBLE
            holder.favoriteButton?.setImageResource(
                if (cardItem.isFavorite) R.drawable.baseline_star_24 else R.drawable.baseline_star_border_24
            )

            holder.favoriteButton?.setOnClickListener {
                onFavoriteToggle(cardItem)
                notifyItemChanged(position)
            }
        } else {
            holder.favoriteButton?.visibility = View.GONE
        }

        // Define a ação ao clicar no item
        holder.itemView.setOnClickListener {
            onItemClick(cardItem)
        }
    }

    /**
     * Retorna o número total de itens na lista.
     * @return O número de itens na lista.
     */
    override fun getItemCount(): Int = items.size

    /**
     * Atualiza os itens do adapter com uma nova lista.
     * @param newItems A nova lista de CardItem.
     */
    fun updateItems(newItems: List<CardItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
