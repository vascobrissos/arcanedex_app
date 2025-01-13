package com.example.arcanedex_app.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CardItem(
    val Id: Int,
    val Name: String,
    val Img: String?,
    val Lore: String
) : Parcelable
