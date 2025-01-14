package pt.ipt.arcanedex_app.viewmodel

import androidx.lifecycle.ViewModel
import pt.ipt.arcanedex_app.data.CardItem

class SharedCardItemViewModel : ViewModel() {
    var selectedCardItem: CardItem? = null
}
