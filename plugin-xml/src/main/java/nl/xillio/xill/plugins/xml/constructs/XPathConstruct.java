/**
 * Copyright (C) 2014 Xillio (support@xillio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.xillio.xill.plugins.xml.constructs;

import com.google.inject.Inject;
import nl.xillio.xill.api.components.ExpressionDataType;
import nl.xillio.xill.api.components.MetaExpression;
import nl.xillio.xill.api.construct.Argument;
import nl.xillio.xill.api.construct.Construct;
import nl.xillio.xill.api.construct.ConstructContext;
import nl.xillio.xill.api.construct.ConstructProcessor;
import nl.xillio.xill.api.data.XmlNode;
import nl.xillio.xill.api.errors.RobotRuntimeException;
import nl.xillio.xill.plugins.xml.data.XmlNodeVar;
import nl.xillio.xill.plugins.xml.services.XpathService;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Returns selected XML node(s) from XML document using XPath locator
 *
 * @author Zbynek Hochmann
 */
public class XPathConstruct extends Construct {
    @Inject
    private XpathService xpathService;

    @Override
    public ConstructProcessor prepareProcess(ConstructContext context) {
        return new ConstructProcessor(
                (element, xPath, namespaces) -> process(element, xPath, namespaces, xpathService),
                new Argument("element", ATOMIC),
                new Argument("xPath", ATOMIC),
                new Argument("namespaces", NULL, OBJECT)
        );
    }

    @SuppressWarnings("unchecked")
    static MetaExpression process(MetaExpression elementVar, MetaExpression xpathVar, MetaExpression namespacesVar, XpathService service) {
        XmlNode node = assertMeta(elementVar, "node", XmlNode.class, "XML node");

        Map<String, String> namespaces = new LinkedHashMap<>();
        if (!namespacesVar.isNull()) {
            if (namespacesVar.getType() != ExpressionDataType.OBJECT) {
                throw new RobotRuntimeException("Invalid namespace data");
            }
            for (Entry<String, MetaExpression> pair : ((Map<String, MetaExpression>) namespacesVar.getValue()).entrySet()) {
                namespaces.put(pair.getKey(), pair.getValue().getStringValue());
            }
        }

        List<MetaExpression> output = new ArrayList<>();


        Object result = service.xpath(node, xpathVar.getStringValue(), namespaces);
        // determine the kind of return: NULL, ATOMIC or LIST
        if(result instanceof NodeList) {
            NodeList resultNodeList = (NodeList) result;
            int resultCardinality = resultNodeList.getLength();

            if(resultCardinality == 0) {
                return NULL;
            }
            else if(resultCardinality == 1) {
                return getOutput(resultNodeList.item(0));
            }
            else {
                return fromValue(service.asStream(resultNodeList).map(XPathConstruct::getOutput).collect(Collectors.toList()));
            }
        } else {
            // String
            return getOutput(result);
        }
    }

    static private MetaExpression getOutput(Object value) {
        if (value instanceof String) {
            return fromValue((String) value);
        } else if (value instanceof Node) {
            Node outputNode = (Node) value;
            MetaExpression output = fromValue(outputNode.toString());
            output.storeMeta(new XmlNodeVar(outputNode));
            return output;
        } else {
            throw new RobotRuntimeException("Invalid XPath type!");
        }
    }

}
