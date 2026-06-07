package com.knoxhack.echoscreencore.client.parser;

import com.knoxhack.echoscreencore.client.debug.EchoScreenDiagnostics;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class EchoMarkupParser {
    public EchoPageDefinition parse(net.minecraft.resources.Identifier pageId, net.minecraft.resources.Identifier resourceId,
            String xml, EchoScreenDiagnostics diagnostics) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml == null ? "" : xml)));
            document.getDocumentElement().normalize();
            return new EchoPageDefinition(pageId, resourceId, parseElement(document.getDocumentElement(), resourceId.toString()));
        } catch (Exception exception) {
            if (diagnostics != null) {
                diagnostics.warn("markup_parse_failed", resourceId + ": " + exception.getMessage());
            }
            EchoNode fallback = EchoNode.builder("page")
                .attribute("id", "parse-error")
                .child(EchoNode.builder("empty-state")
                    .attribute("title", "ScreenCore page could not be parsed")
                    .attribute("body", exception.getMessage())
                    .build())
                .build();
            return new EchoPageDefinition(pageId, resourceId, fallback);
        }
    }

    private EchoNode parseElement(Element element, String source) {
        EchoNode.Builder builder = EchoNode.builder(element.getTagName()).source(source);
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            builder.attribute(attribute.getNodeName(), attribute.getNodeValue());
        }
        StringBuilder text = new StringBuilder();
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child instanceof Element childElement) {
                builder.child(parseElement(childElement, source));
            } else if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                String value = child.getTextContent();
                if (value != null && !value.isBlank()) {
                    if (!text.isEmpty()) {
                        text.append(' ');
                    }
                    text.append(value.strip());
                }
            }
        }
        return builder.text(text.toString()).build();
    }
}
