package com.developersancho.formatico.core.format.types

import com.developersancho.formatico.core.format.convert.BoardOrder
import com.developersancho.formatico.core.format.convert.dataOf
import com.developersancho.formatico.core.format.convert.lines
import kotlin.test.Test
import kotlin.test.assertEquals

class BoardOrderTypesTest {
    private val notes = listOf(TypeNote.OnlyNull(listOf("coupon")), TypeNote.Dates(listOf("createdAt", "eta")))

    @Test
    fun theSampleIsTheBoardsOrderAsTheJsonReadsIt() {
        assertEquals(dataOf(BoardOrder), BoardOrderValue)
    }

    @Test
    fun theBoardsOrderIsSixKotlinDataClassesItsNullAJsonElement() {
        val types = typesOf(BoardOrderValue, TypeLanguage.Kotlin, "Order")
        assertEquals(
            lines(
                "import kotlinx.serialization.Serializable",
                "import kotlinx.serialization.json.JsonElement",
                "",
                "@Serializable",
                "data class Order(",
                "    val id: String,",
                "    val status: String,",
                "    val createdAt: String,",
                "    val total: Double,",
                "    val currency: String,",
                "    val paid: Boolean,",
                "    val coupon: JsonElement? = null,",
                "    val customer: Customer,",
                "    val theme: Theme,",
                "    val items: List<Item>,",
                "    val shipping: Shipping",
                ")",
                "",
                "@Serializable",
                "data class Customer(",
                "    val name: String,",
                "    val email: String,",
                "    val avatar: String,",
                "    val website: String",
                ")",
                "",
                "@Serializable",
                "data class Theme(",
                "    val accent: String",
                ")",
                "",
                "@Serializable",
                "data class Item(",
                "    val sku: String,",
                "    val name: String,",
                "    val qty: Int,",
                "    val price: Double",
                ")",
                "",
                "@Serializable",
                "data class Shipping(",
                "    val method: String,",
                "    val eta: String,",
                "    val address: Address",
                ")",
                "",
                "@Serializable",
                "data class Address(",
                "    val city: String,",
                "    val country: String",
                ")"
            ),
            types.text
        )
        assertEquals(6, types.types)
        assertEquals(notes, types.notes)
    }

    @Test
    fun theBoardsOrderIsSixTypeScriptInterfacesItsNullUnknown() {
        val types = typesOf(BoardOrderValue, TypeLanguage.TypeScript, "Order")
        assertEquals(
            lines(
                "export interface Order {",
                "  id: string;",
                "  status: string;",
                "  createdAt: string;",
                "  total: number;",
                "  currency: string;",
                "  paid: boolean;",
                "  coupon: unknown;",
                "  customer: Customer;",
                "  theme: Theme;",
                "  items: Item[];",
                "  shipping: Shipping;",
                "}",
                "",
                "export interface Customer {",
                "  name: string;",
                "  email: string;",
                "  avatar: string;",
                "  website: string;",
                "}",
                "",
                "export interface Theme {",
                "  accent: string;",
                "}",
                "",
                "export interface Item {",
                "  sku: string;",
                "  name: string;",
                "  qty: number;",
                "  price: number;",
                "}",
                "",
                "export interface Shipping {",
                "  method: string;",
                "  eta: string;",
                "  address: Address;",
                "}",
                "",
                "export interface Address {",
                "  city: string;",
                "  country: string;",
                "}"
            ),
            types.text
        )
        assertEquals(6, types.types)
        assertEquals(notes, types.notes)
    }

    @Test
    fun theBoardsOrderIsSixSwiftStructsItsNullAStringThatIsAGuess() {
        val types = typesOf(BoardOrderValue, TypeLanguage.Swift, "Order")
        assertEquals(
            lines(
                "import Foundation",
                "",
                "struct Order: Codable {",
                "    let id: String",
                "    let status: String",
                "    let createdAt: String",
                "    let total: Double",
                "    let currency: String",
                "    let paid: Bool",
                "    let coupon: String? // Only ever null here: its type is a guess",
                "    let customer: Customer",
                "    let theme: Theme",
                "    let items: [Item]",
                "    let shipping: Shipping",
                "}",
                "",
                "struct Customer: Codable {",
                "    let name: String",
                "    let email: String",
                "    let avatar: String",
                "    let website: String",
                "}",
                "",
                "struct Theme: Codable {",
                "    let accent: String",
                "}",
                "",
                "struct Item: Codable {",
                "    let sku: String",
                "    let name: String",
                "    let qty: Int",
                "    let price: Double",
                "}",
                "",
                "struct Shipping: Codable {",
                "    let method: String",
                "    let eta: String",
                "    let address: Address",
                "}",
                "",
                "struct Address: Codable {",
                "    let city: String",
                "    let country: String",
                "}"
            ),
            types.text
        )
        assertEquals(6, types.types)
        assertEquals(notes, types.notes)
    }

    @Test
    fun theBoardsOrderIsSixGoStructsInItsPackageAlignedAsGofmtAlignsThem() {
        val types = typesOf(BoardOrderValue, TypeLanguage.Go, "Order")
        assertEquals(
            lines(
                "package order",
                "",
                "type Order struct {",
                "\tID        string   `json:\"id\"`",
                "\tStatus    string   `json:\"status\"`",
                "\tCreatedAt string   `json:\"createdAt\"`",
                "\tTotal     float64  `json:\"total\"`",
                "\tCurrency  string   `json:\"currency\"`",
                "\tPaid      bool     `json:\"paid\"`",
                "\tCoupon    any      `json:\"coupon\"`",
                "\tCustomer  Customer `json:\"customer\"`",
                "\tTheme     Theme    `json:\"theme\"`",
                "\tItems     []Item   `json:\"items\"`",
                "\tShipping  Shipping `json:\"shipping\"`",
                "}",
                "",
                "type Customer struct {",
                "\tName    string `json:\"name\"`",
                "\tEmail   string `json:\"email\"`",
                "\tAvatar  string `json:\"avatar\"`",
                "\tWebsite string `json:\"website\"`",
                "}",
                "",
                "type Theme struct {",
                "\tAccent string `json:\"accent\"`",
                "}",
                "",
                "type Item struct {",
                "\tSku   string  `json:\"sku\"`",
                "\tName  string  `json:\"name\"`",
                "\tQty   int     `json:\"qty\"`",
                "\tPrice float64 `json:\"price\"`",
                "}",
                "",
                "type Shipping struct {",
                "\tMethod  string  `json:\"method\"`",
                "\tEta     string  `json:\"eta\"`",
                "\tAddress Address `json:\"address\"`",
                "}",
                "",
                "type Address struct {",
                "\tCity    string `json:\"city\"`",
                "\tCountry string `json:\"country\"`",
                "}"
            ),
            types.text
        )
        assertEquals(6, types.types)
        assertEquals(notes, types.notes)
    }
}
