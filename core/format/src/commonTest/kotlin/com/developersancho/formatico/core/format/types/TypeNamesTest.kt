package com.developersancho.formatico.core.format.types

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TypeNamesTest {
    @Test
    fun theRootIsNamedAfterTheDocumentsStemInPascalCase() {
        assertEquals("Order", rootTypeName("order.json"))
        assertEquals("UserProfile", rootTypeName("user-profile.yaml"))
        assertEquals("UserProfile", rootTypeName("user_profile.toml"))
        assertEquals("LineItems", rootTypeName("/Users/elif/Downloads/line items.csv"))
        assertEquals("OrderV2", rootTypeName("C:\\docs\\order.v2.json"))
        assertEquals("Env", rootTypeName(".env"))
        assertEquals("README", rootTypeName("README"))
        assertEquals("ŞehirListesi", rootTypeName("şehir listesi.json"))
    }

    @Test
    fun aRootWithNoNameOrNothingUsableInItIsRoot() {
        assertEquals("Root", rootTypeName(null))
        assertEquals("Root", rootTypeName(""))
        assertEquals("Root", rootTypeName("---.json"))
        assertEquals("Root", typeNameOf("  "))
    }

    @Test
    fun theNumbersAtTheStartOfANameMoveToItsEndAndTypeGoesBeforeADigitLeft() {
        assertEquals("Sales2026", rootTypeName("2026 sales.csv"))
        assertEquals("Q4Form", typeNameOf("Q4 form"))
        assertEquals("ReportFinal20261107", typeNameOf("2026 11 07 report final"))
        assertEquals("Type2fa", typeNameOf("2fa"))
        assertEquals("Type2026", rootTypeName("2026.csv"))
        assertEquals("Covid19Cases", typeNameOf("covid19 cases"))
    }

    @Test
    fun aTypedNameIsItsWordsInPascalCase() {
        assertEquals("MyOrder", typeNameOf("my order"))
        assertEquals("OrderDTO", typeNameOf("orderDTO"))
        assertEquals("UserProfile", typeNameOf("USER_PROFILE"))
        assertEquals("API", typeNameOf("API"))
        assertEquals("HTTPServer", typeNameOf("HTTPServer"))
        assertEquals("ContentType", typeNameOf("Content-Type"))
        assertEquals("Order", typeNameOf("Order"))
        assertEquals("OrderLine", typeNameOf("  order..line!! "))
    }

    @Test
    fun aTypeNameIsMadeTheSameAgain() {
        listOf("Order", "UserProfile", "Sales2026", "Type2fa", "HTTPServer", "OrderDTO", "API", "Root").forEach {
            assertEquals(it, typeNameOf(it))
        }
    }

    @Test
    fun theTypesAreSavedInAFileNamedAsEachLanguageNamesIt() {
        assertEquals("Order.kt", typesFileName("Order", TypeLanguage.Kotlin))
        assertEquals("order.ts", typesFileName("Order", TypeLanguage.TypeScript))
        assertEquals("Order.swift", typesFileName("Order", TypeLanguage.Swift))
        assertEquals("order.go", typesFileName("Order", TypeLanguage.Go))
        assertEquals("UserProfile.kt", typesFileName("UserProfile", TypeLanguage.Kotlin))
        assertEquals("userProfile.ts", typesFileName("UserProfile", TypeLanguage.TypeScript))
        assertEquals("UserProfile.swift", typesFileName("UserProfile", TypeLanguage.Swift))
        assertEquals("userprofile.go", typesFileName("UserProfile", TypeLanguage.Go))
        assertEquals("httpServer.ts", typesFileName("HTTPServer", TypeLanguage.TypeScript))
        assertEquals("Root.kt", typesFileName("", TypeLanguage.Kotlin))
        assertEquals("sales2026.go", typesFileName("2026 sales", TypeLanguage.Go))
        assertEquals("order.schema.json", typesFileName("Order", TypeLanguage.JsonSchema))
        assertEquals("user-profile.schema.json", typesFileName("UserProfile", TypeLanguage.JsonSchema))
    }

    @Test
    fun aPluralIsMadeSingularInSimpleEnglish() {
        val plurals = mapOf(
            "items" to "item",
            "addresses" to "address",
            "categories" to "category",
            "boxes" to "box",
            "statuses" to "status",
            "buses" to "bus",
            "houses" to "house",
            "matches" to "match",
            "wishes" to "wish",
            "caches" to "cache",
            "movies" to "movie",
            "ties" to "tie",
            "people" to "person",
            "children" to "child",
            "indices" to "index",
            "menus" to "menu",
            "emojis" to "emoji",
            "ids" to "id",
            "photos" to "photo",
            "Items" to "Item",
            "ITEMS" to "ITEM",
            "CATEGORIES" to "CATEGORY",
            "People" to "Person"
        )
        plurals.forEach { (plural, one) -> assertEquals(one, singular(plural), plural) }
    }

    @Test
    fun aWordThatReadsAsNoPluralHasNoSingular() {
        listOf("status", "class", "address", "analysis", "bonus", "data", "news", "series", "species", "list", "is", "s").forEach {
            assertNull(singular(it), it)
        }
    }

    @Test
    fun anArraysItemsAreNamedAfterTheMemberSingularised() {
        val json =
            """
            {"items": [{"a": 1}], "line_items": [{"b": 1}], "addresses": [{"c": 1}], "categories": [{"d": 1}],
             "status": [{"e": 1}], "people": [{"f": 1}], "data": [{"g": 1}], "": [{"h": 1}], "2026": {"i": 1}, "!": {"j": 1}}
            """.trimIndent()
        assertEquals(
            listOf("Root", "Item", "LineItem", "Address", "Category", "StatusItem", "Person", "DataItem", "Item2", "Type2026", "Value"),
            declaredNames(textOf(json, TypeLanguage.TypeScript), "export interface ")
        )
    }

    @Test
    fun anObjectsTypeIsNamedAfterItsMemberInPascalCase() {
        val json = """{"customer": {"a": 1}, "shipping_address": {"b": 1}, "billing-info": {"c": 1}, "2026 sales": {"d": 1}}"""
        assertEquals(
            listOf("Root", "Customer", "ShippingAddress", "BillingInfo", "Sales2026"),
            declaredNames(textOf(json, TypeLanguage.Kotlin), "data class ")
        )
    }
}
