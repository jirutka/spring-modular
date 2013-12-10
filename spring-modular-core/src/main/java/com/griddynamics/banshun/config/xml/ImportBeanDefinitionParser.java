/*
 * Copyright 2012 Grid Dynamics Consulting Services, Inc.
 *      http://www.griddynamics.com
 *
 * Copyright 2013 Jakub Jirutka <jakub@jirutka.cz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.griddynamics.banshun.config.xml;

import com.griddynamics.banshun.BeanReferenceInfo;
import com.griddynamics.banshun.Registry;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.io.Resource;
import org.w3c.dom.Element;

import static com.griddynamics.banshun.config.xml.ParserUtils.*;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

public class ImportBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected String getBeanClassName(Element el) {
        return el.getAttribute(INTERFACE_ATTR);
    }

    @Override
    protected void doParse(Element el, ParserContext parserContext, BeanDefinitionBuilder builder) {

        Resource resource = parserContext.getReaderContext().getResource();

        String rootName = defaultIfBlank(el.getAttribute(ROOT_ATTR), DEFAULT_ROOT_FACTORY_NAME);
        String serviceIfaceName = el.getAttribute(INTERFACE_ATTR);
        String serviceName = el.getAttribute(ID_ATTR);

        Class<?> serviceIface = ParserUtils.findClassByName(serviceIfaceName, el.getAttribute(ID_ATTR), parserContext);

        AbstractBeanDefinition beanDef = builder.getRawBeanDefinition();
        beanDef.setFactoryBeanName(rootName);
        beanDef.setFactoryMethodName(Registry.LOOKUP_METHOD_NAME);
        beanDef.setConstructorArgumentValues(
                defineLookupMethodArgs(serviceName, serviceIface));
        beanDef.setLazyInit(true);
        beanDef.setScope(SCOPE_SINGLETON);
        beanDef.setResource(resource);

        beanDef.setAttribute(IMPORT_BEAN_DEF_ATTR_NAME,
                new BeanReferenceInfo(serviceName, serviceIface, extractResourcePath(resource)));
    }

    /**
     * Creates arguments definition for the {@link Registry#lookup(String, Class) lookup()} method
     * of the registry bean.
     */
    private ConstructorArgumentValues defineLookupMethodArgs(String serviceName, Class<?> serviceIface) {

        ConstructorArgumentValues holder = new ConstructorArgumentValues();
        holder.addIndexedArgumentValue(0, serviceName);
        holder.addIndexedArgumentValue(1, serviceIface);

        return holder;
    }
}
