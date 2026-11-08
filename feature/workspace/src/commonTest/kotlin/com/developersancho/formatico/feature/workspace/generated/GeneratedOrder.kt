// The Kotlin Types writes of the boards' order.json, word for word: GeneratedKotlinTextTest checks it is the generator's
// text. Compiled here, on every target, with kotlinx.serialization, it shows that the Kotlin Formatico writes compiles;
// GeneratedKotlinTest that it reads the document it was written of.
package com.developersancho.formatico.feature.workspace.generated

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Order(
    val id: String,
    val status: String,
    val createdAt: String,
    val total: Double,
    val currency: String,
    val paid: Boolean,
    val coupon: JsonElement? = null,
    val customer: Customer,
    val theme: Theme,
    val items: List<Item>,
    val shipping: Shipping
)

@Serializable
data class Customer(
    val name: String,
    val email: String,
    val avatar: String,
    val website: String
)

@Serializable
data class Theme(
    val accent: String
)

@Serializable
data class Item(
    val sku: String,
    val name: String,
    val qty: Int,
    val price: Double
)

@Serializable
data class Shipping(
    val method: String,
    val eta: String,
    val address: Address
)

@Serializable
data class Address(
    val city: String,
    val country: String
)
