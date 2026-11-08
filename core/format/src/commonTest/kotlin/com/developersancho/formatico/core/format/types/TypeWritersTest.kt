package com.developersancho.formatico.core.format.types

import com.developersancho.formatico.core.format.convert.dataOf
import com.developersancho.formatico.core.format.convert.lines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypeWritersTest {
    /** A profile whose names are no names in most languages: keywords, a digit first, words of other cases. */
    private val profile = dataOf(
        """
        {
          "first_name": "Elif", "class": "gold", "default": true, "2fa": false, "self": "me",
          "Content-Type": "application/json", "user_id": 7, "userId": 8, "avatar_url": "https://elif.dev/a.png",
          "tags": [1, "two"], "history": [], "extra": {"in": 1, "is": "x", "for": null}, "a\"b": "quote"
        }
        """.trimIndent()
    )

    @Test
    fun kotlinNamesAMemberInLowerCamelCaseItsJsonNameASerialNameWhereItIsAnother() {
        val types = typesOf(profile, TypeLanguage.Kotlin, "Profile")
        assertEquals(
            lines(
                "import kotlinx.serialization.SerialName",
                "import kotlinx.serialization.Serializable",
                "import kotlinx.serialization.json.JsonElement",
                "",
                "@Serializable",
                "data class Profile(",
                "    @SerialName(\"first_name\") val firstName: String,",
                "    val `class`: String,",
                "    val default: Boolean,",
                "    @SerialName(\"2fa\") val _2fa: Boolean,",
                "    val self: String,",
                "    @SerialName(\"Content-Type\") val contentType: String,",
                "    @SerialName(\"user_id\") val userId: Int,",
                "    @SerialName(\"userId\") val userId2: Int,",
                "    @SerialName(\"avatar_url\") val avatarUrl: String,",
                "    val tags: List<JsonElement>,",
                "    val history: List<JsonElement>,",
                "    val extra: Extra,",
                "    @SerialName(\"a\\\"b\") val aB: String",
                ")",
                "",
                "@Serializable",
                "data class Extra(",
                "    val `in`: Int,",
                "    val `is`: String,",
                "    val `for`: JsonElement? = null",
                ")"
            ),
            types.text
        )
        assertEquals(
            listOf(TypeNote.OnlyNull(listOf("for")), TypeNote.Mixed(listOf("tags")), TypeNote.EmptyList(listOf("history"))),
            types.notes
        )
    }

    @Test
    fun kotlinEscapesASerialNamesDollarAndNamesAMemberOfNoLettersField() {
        assertContainsLines(
            textOf("""{"${'$'}ref": "x", "!!": 1, "_id": "y"}""", TypeLanguage.Kotlin),
            "    @SerialName(\"\\${'$'}ref\") val ref: String,",
            "    @SerialName(\"!!\") val field: Int,",
            "    @SerialName(\"_id\") val id: String"
        )
    }

    @Test
    fun kotlinImportsOnlyWhatItsTypesUse() {
        assertEquals(
            lines("import kotlinx.serialization.Serializable", "", "@Serializable", "data class Root(", "    val a: Int", ")"),
            textOf("""{"a": 1}""", TypeLanguage.Kotlin)
        )
    }

    @Test
    fun typeScriptKeepsTheJsonNamesQuotingThoseThatAreNoIdentifiers() {
        assertEquals(
            lines(
                "export interface Profile {",
                "  first_name: string;",
                "  class: string;",
                "  default: boolean;",
                "  \"2fa\": boolean;",
                "  self: string;",
                "  \"Content-Type\": string;",
                "  user_id: number;",
                "  userId: number;",
                "  avatar_url: string;",
                "  tags: unknown[];",
                "  history: unknown[];",
                "  extra: Extra;",
                "  \"a\\\"b\": string;",
                "}",
                "",
                "export interface Extra {",
                "  in: number;",
                "  is: string;",
                "  for: unknown;",
                "}"
            ),
            typesOf(profile, TypeLanguage.TypeScript, "Profile").text
        )
        assertContainsLines(
            textOf("""{"": 1, "a b": 2, "${'$'}ref": 3, "_x": 4, "ünlü": 5, "1a": 6}""", TypeLanguage.TypeScript),
            "  \"\": number;",
            "  \"a b\": number;",
            "  ${'$'}ref: number;",
            "  _x: number;",
            "  ünlü: number;",
            "  \"1a\": number;"
        )
    }

    @Test
    fun swiftListsItsCodingKeysWhereANameIsAnotherAndWritesJsonValueOnceAtTheEnd() {
        val text = typesOf(profile, TypeLanguage.Swift, "Profile").text
        val structs = lines(
            "import Foundation",
            "",
            "struct Profile: Codable {",
            "    let firstName: String",
            "    let `class`: String",
            "    let `default`: Bool",
            "    let _2fa: Bool",
            "    let `self`: String",
            "    let contentType: String",
            "    let userId: Int",
            "    let userId2: Int",
            "    let avatarUrl: String",
            "    let tags: [JSONValue]",
            "    let history: [JSONValue]",
            "    let extra: Extra",
            "    let aB: String",
            "",
            "    enum CodingKeys: String, CodingKey {",
            "        case firstName = \"first_name\"",
            "        case `class`",
            "        case `default`",
            "        case _2fa = \"2fa\"",
            "        case `self`",
            "        case contentType = \"Content-Type\"",
            "        case userId = \"user_id\"",
            "        case userId2 = \"userId\"",
            "        case avatarUrl = \"avatar_url\"",
            "        case tags",
            "        case history",
            "        case extra",
            "        case aB = \"a\\\"b\"",
            "    }",
            "}",
            "",
            "struct Extra: Codable {",
            "    let `in`: Int",
            "    let `is`: String",
            "    let `for`: String? // Only ever null here: its type is a guess",
            "}",
            ""
        )
        assertTrue(text.startsWith(structs + "enum JSONValue: Codable {\n"), text)
        assertEquals(1, text.split("enum JSONValue").size - 1)
        assertTrue(text.endsWith("    }\n}\n"), text)
    }

    @Test
    fun swiftWritesNoCodingKeysNorJsonValueWhereNoneIsNeeded() {
        val text = textOf("""{"id": 1, "name": "a", "tags": ["x"]}""", TypeLanguage.Swift)
        assertEquals(
            lines(
                "import Foundation",
                "",
                "struct Root: Codable {",
                "    let id: Int",
                "    let name: String",
                "    let tags: [String]",
                "}"
            ),
            text
        )
    }

    @Test
    fun goAlignsItsFieldsAndUpperCasesItsInitialisms() {
        assertEquals(
            lines(
                "package profile",
                "",
                "type Profile struct {",
                "\tFirstName   string `json:\"first_name\"`",
                "\tClass       string `json:\"class\"`",
                "\tDefault     bool   `json:\"default\"`",
                "\tField2fa    bool   `json:\"2fa\"`",
                "\tSelf        string `json:\"self\"`",
                "\tContentType string `json:\"Content-Type\"`",
                "\tUserID      int    `json:\"user_id\"`",
                "\tUserID2     int    `json:\"userId\"`",
                "\tAvatarURL   string `json:\"avatar_url\"`",
                "\tTags        []any  `json:\"tags\"`",
                "\tHistory     []any  `json:\"history\"`",
                "\tExtra       Extra  `json:\"extra\"`",
                "\tAB          string `json:\"a\\\"b\"`",
                "}",
                "",
                "type Extra struct {",
                "\tIn  int    `json:\"in\"`",
                "\tIs  string `json:\"is\"`",
                "\tFor any    `json:\"for\"`",
                "}"
            ),
            typesOf(profile, TypeLanguage.Go, "Profile").text
        )
        val initialisms =
            """
            {"id": 1, "apiKey": "k", "html": "h", "uuid": "u", "ip_address": "i", "https_proxy": "p", "xml": "x", "sql": "s",
             "css": "c", "tcp": 1, "udp": 2, "uri": "u", "http": "h", "jsonData": "j", "identity": "y", "ship": "s"}
            """.trimIndent()
        assertEquals(
            listOf(
                "ID",
                "APIKey",
                "HTML",
                "UUID",
                "IPAddress",
                "HTTPSProxy",
                "XML",
                "SQL",
                "CSS",
                "TCP",
                "UDP",
                "URI",
                "HTTP",
                "JSONData",
                "Identity",
                "Ship"
            ),
            textOf(initialisms, TypeLanguage.Go).lines().filter { it.startsWith("\t") }.map { it.trim().substringBefore(' ') }
        )
    }

    @Test
    fun goNamesItsTypesWithItsInitialismsAsWell() {
        val json = """{"api": {"url": "x"}, "user_ids": [{"a": 1}]}"""
        assertEquals(listOf("Root", "API", "UserID"), declaredNames(textOf(json, TypeLanguage.Go), "type "))
        assertEquals(listOf("Root", "Api", "UserId"), declaredNames(textOf(json, TypeLanguage.Kotlin), "data class "))
    }

    @Test
    fun goMakesAnOptionalOrNullableScalarOrStructAPointerAndAnOptionalOneOmitEmpty() {
        val json = """{"rows": [{"customer": {"a": 1}, "list": [1], "mixed": 1, "n": 1, "note": null}, {"mixed": "x", "note": "y"}]}"""
        assertContainsLines(
            textOf(json, TypeLanguage.Go),
            "\tCustomer *Customer `json:\"customer,omitempty\"`",
            "\tList     []int     `json:\"list,omitempty\"`",
            "\tMixed    any       `json:\"mixed\"`",
            "\tN        *int      `json:\"n,omitempty\"`",
            "\tNote     *string   `json:\"note\"`"
        )
    }

    @Test
    fun goNamesItsPackageAfterTheRootAndQuotesATagThatHoldsABacktick() {
        assertTrue(textOf("""{"a": 1}""", TypeLanguage.Go).startsWith("package root\n"))
        assertTrue(
            typesOf("""{"a": 1}""", TypeLanguage.Go, "user profile").text.startsWith("package userprofile\n\ntype UserProfile struct {\n")
        )
        assertTrue(typesOf("""{"a": 1}""", TypeLanguage.Go, "Map").text.startsWith("package maptypes\n\ntype Map struct {\n"))
        assertEquals(
            lines("package root", "", "type Root struct {", "\tAB int \"json:\\\"a`b\\\"\"", "}"),
            textOf("""{"a`b": 1}""", TypeLanguage.Go)
        )
    }

    @Test
    fun goNamesTwoFieldsOfTheSameNameApartAndPrefixesOneThatWouldNotBeExported() {
        assertContainsLines(
            textOf("""{"user_id": 1, "userID": 2, "UserId": 3, "名前": "x", "": "y"}""", TypeLanguage.Go),
            "\tUserID  int    `json:\"user_id\"`",
            "\tUserID2 int    `json:\"userID\"`",
            "\tUserID3 int    `json:\"UserId\"`",
            "\tField名前 string `json:\"名前\"`",
            "\tField   string `json:\"\"`"
        )
    }
}
