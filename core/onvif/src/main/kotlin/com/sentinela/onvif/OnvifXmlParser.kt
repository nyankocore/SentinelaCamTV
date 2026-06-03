package com.sentinela.onvif

import java.io.StringReader
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object OnvifXmlParser {
    internal const val MAX_XML_BYTES = 512 * 1024
    private val doctypePattern = Regex("<!\\s*DOCTYPE", RegexOption.IGNORE_CASE)

    fun parseProbeMatches(xml: String): List<DiscoveredOnvifDevice> {
        val root = xml.toXmlNode()
        return root.elements("ProbeMatch").map { match ->
            DiscoveredOnvifDevice(
                endpointReference = match.firstText("Address").orEmpty(),
                types = match.firstText("Types").splitWords(),
                xAddrs = match.firstText("XAddrs").splitWords(),
                scopes = match.firstText("Scopes").splitWords(),
            )
        }
    }

    fun parseCapabilities(xml: String): OnvifCapabilities {
        val root = xml.toXmlNode()
        return OnvifCapabilities(
            mediaXAddr = root.elements("Media").firstOrNull()?.firstText("XAddr"),
            deviceXAddr = root.elements("Device").firstOrNull()?.firstText("XAddr"),
        )
    }

    fun parseProfiles(xml: String): List<OnvifMediaProfile> {
        val root = xml.toXmlNode()
        return root.elements("Profiles").mapNotNull { profile ->
            val token = profile.attribute("token").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            OnvifMediaProfile(
                token = token,
                name = profile.firstText("Name").orEmpty(),
                fixed = profile.attribute("fixed").equals("true", ignoreCase = true),
            )
        }
    }

    fun parseStreamUri(xml: String): OnvifStreamUri {
        val root = xml.toXmlNode()
        val mediaUri = root.elements("MediaUri").firstOrNull() ?: root
        return OnvifStreamUri(
            uri = mediaUri.firstText("Uri").orEmpty(),
            invalidAfterConnect = mediaUri.firstText("InvalidAfterConnect")?.toBooleanStrictOrNull() ?: false,
            invalidAfterReboot = mediaUri.firstText("InvalidAfterReboot")?.toBooleanStrictOrNull() ?: false,
            timeout = mediaUri.firstText("Timeout"),
        )
    }

    fun parseSoapFault(xml: String): OnvifFailure.SoapFault? {
        val root = runCatching { xml.toXmlNode() }.getOrNull() ?: return null
        val fault = root.elements("Fault").firstOrNull() ?: return null
        return OnvifFailure.SoapFault(
            code = fault.firstText("Value") ?: fault.firstText("Code"),
            reason = fault.firstText("Text") ?: fault.firstText("Reason"),
        )
    }

    private fun String.toXmlNode(): XmlNode {
        val bytes = toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_XML_BYTES) {
            "XML ONVIF excede o limite de tamanho."
        }
        require(!doctypePattern.containsMatchIn(this)) {
            "DOCTYPE não é permitido em XML ONVIF."
        }

        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(StringReader(this@toXmlNode))
        }
        val stack = ArrayDeque<XmlNodeBuilder>()
        var root: XmlNode? = null

        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> stack.addLast(parser.toNodeBuilder())
                XmlPullParser.TEXT,
                XmlPullParser.CDSECT,
                XmlPullParser.ENTITY_REF,
                -> stack.lastOrNull()?.appendText(parser.text.orEmpty())
                XmlPullParser.DOCDECL -> throw IllegalArgumentException("DOCTYPE não é permitido em XML ONVIF.")
                XmlPullParser.END_TAG -> {
                    val node = stack.removeLast().build()
                    val parent = stack.lastOrNull()
                    if (parent == null) {
                        root = node
                    } else {
                        parent.addChild(node)
                    }
                }
                XmlPullParser.END_DOCUMENT -> break
            }
        }

        return root ?: XmlNode(localName = "", attributes = emptyMap(), text = "", children = emptyList())
    }

    private fun XmlPullParser.toNodeBuilder(): XmlNodeBuilder {
        val attributes = mutableMapOf<String, String>()
        for (index in 0 until attributeCount) {
            attributes[getAttributeName(index).localName()] = getAttributeValue(index)
        }
        return XmlNodeBuilder(localName = name.localName(), attributes = attributes)
    }

    private fun String?.splitWords(): List<String> =
        orEmpty().split(Regex("\\s+")).filter { it.isNotBlank() }

    private fun String?.localName(): String =
        orEmpty().substringAfter(':')

    private data class XmlNode(
        val localName: String,
        val attributes: Map<String, String>,
        val text: String,
        val children: List<XmlNode>,
    ) {
        fun attribute(name: String): String =
            attributes[name].orEmpty()

        fun elements(localName: String): List<XmlNode> {
            val result = mutableListOf<XmlNode>()
            fun visit(node: XmlNode) {
                if (node.localName == localName) {
                    result += node
                }
                node.children.forEach(::visit)
            }
            visit(this)
            return result
        }

        fun firstText(localName: String): String? =
            elements(localName).firstOrNull()?.text?.trim()
    }

    private class XmlNodeBuilder(
        private val localName: String,
        private val attributes: Map<String, String>,
    ) {
        private val text = StringBuilder()
        private val children = mutableListOf<XmlNode>()

        fun appendText(value: String) {
            text.append(value)
        }

        fun addChild(node: XmlNode) {
            children += node
        }

        fun build(): XmlNode =
            XmlNode(
                localName = localName,
                attributes = attributes,
                text = text.toString(),
                children = children.toList(),
            )
    }
}
