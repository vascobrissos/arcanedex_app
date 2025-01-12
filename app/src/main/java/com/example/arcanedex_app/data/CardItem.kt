package com.example.arcanedex_app.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CardItem(
    val name: String,
    val imageUrl: String?
) : Parcelable