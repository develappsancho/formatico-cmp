package com.developersancho.formatico.core.format.xml

import com.developersancho.formatico.core.format.text.TextRange

/** A node of an XML document, where it is written. */
sealed class XmlNode {
    /** The text of the node, from its first character to its last. */
    abstract val range: TextRange
}

/**
 * An element.
 *
 * @property range from the `<` of its start tag to the `>` of its end tag, or of its tag where it closes itself. An
 *   element that is never closed ends where what was read of it ends.
 * @property name its qualified name as it is written: `xs:element`.
 * @property nameRange where its name stands in its start tag.
 * @property attributes its attributes in the order they are written, one given twice included.
 * @property children what it holds, in order: elements, text, comments and processing instructions.
 * @property selfClosing whether it is written as one tag that closes itself: `<br/>`.
 */
class XmlElement(
    override val range: TextRange,
    val name: String,
    val nameRange: TextRange,
    val attributes: List<XmlAttribute>,
    val children: List<XmlNode>,
    val selfClosing: Boolean
) : XmlNode()

/**
 * An attribute of an element.
 *
 * @property range from its name to the end of its value.
 * @property name its qualified name as it is written: `android:layout_width`.
 * @property nameRange where its name stands.
 * @property raw its value as it is written, its quotes included; empty where it has none.
 * @property value its value: the quotes taken off and the references read. A tab or a line break written in it is read
 *   as a space, as XML reads it; one written as a reference stays itself.
 */
class XmlAttribute(val range: TextRange, val name: String, val nameRange: TextRange, val raw: String, val value: String)

/**
 * Text, or a CDATA section.
 *
 * @property raw the text as it is written: a CDATA section with its `<![CDATA[` and `]]>`.
 * @property value the text with its references read, or a CDATA section's content as it is. A line break is read as
 *   `\n`, however it is written, as XML reads it.
 * @property cdata whether it is a CDATA section.
 */
class XmlText(override val range: TextRange, val raw: String, val value: String, val cdata: Boolean) : XmlNode()

/** A comment, from its `<!--` to its `-->`. */
class XmlComment(override val range: TextRange) : XmlNode()

/**
 * A processing instruction, the XML declaration `<?xml …?>` among them.
 *
 * @property target the name after its `<?`: `xml` for the declaration, `xml-stylesheet`.
 */
class XmlProcessingInstruction(override val range: TextRange, val target: String) : XmlNode()

/** A document type declaration, `<!DOCTYPE …>`, its internal subset included: kept as it is written, and not read. */
class XmlDoctype(override val range: TextRange) : XmlNode()
