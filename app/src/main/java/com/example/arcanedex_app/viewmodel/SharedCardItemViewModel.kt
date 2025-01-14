package com.example.arcanedex_app.viewmodel

import androidx.lifecycle.ViewModel
import com.example.arcanedex_app.data.CardItem

class SharedCardItemViewModel : ViewModel() {
    var selectedCardItem: CardItem? = null
}
