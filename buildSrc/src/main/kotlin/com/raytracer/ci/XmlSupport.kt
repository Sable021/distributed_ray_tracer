package com.raytracer.ci

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

internal object XmlSupport {

    fun parse(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            // JaCoCo reports carry a DOCTYPE; never resolve external DTDs (offline + safe).
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isValidating = false
        }
        return factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
    }

    fun Element.elementsByTag(tag: String): List<Element> {
        val nodes = getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    fun Element.childElements(tag: String): List<Element> {
        val out = mutableListOf<Element>()
        var child: Node? = firstChild
        while (child != null) {
            if (child.nodeType == Node.ELEMENT_NODE && child.nodeName == tag) {
                out.add(child as Element)
            }
            child = child.nextSibling
        }
        return out
    }

    fun Element.intAttr(name: String): Int =
        getAttribute(name).takeIf { it.isNotEmpty() }?.toInt() ?: 0
}
