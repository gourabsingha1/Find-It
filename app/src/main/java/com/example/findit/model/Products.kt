package com.example.findit.model

import android.os.Parcel
import android.os.Parcelable

class Products(
    val name: String? = null,
    val price: Double? = null,
    val location: String? = null,
    val description: String? = null,
    val productId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    var wishlist: Boolean? = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeValue(price)
        parcel.writeString(location)
        parcel.writeString(description)
        parcel.writeString(productId)
        parcel.writeValue(latitude)
        parcel.writeValue(longitude)
        parcel.writeValue(wishlist)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Products> {
        override fun createFromParcel(parcel: Parcel): Products {
            return Products(parcel)
        }

        override fun newArray(size: Int): Array<Products?> {
            return arrayOfNulls(size)
        }
    }
}