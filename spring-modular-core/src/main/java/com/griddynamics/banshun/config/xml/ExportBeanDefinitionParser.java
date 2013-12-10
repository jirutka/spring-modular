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
import com.griddynamics.banshun.ExportRef;
import com.griddynamics.banshun.Registry;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.io.Resource;
import org.springframework.util.ObjectUtils;
import org.w3c.dom.Element;

import static com.griddynamics.banshun.ContextParentBean.EXPORT_REF_SUFFIX;
import static com.griddynamics.banshun.config.xml.ParserUtils.*;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

public class ExportBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected String getBeanClassName(Element el) {
        return Void.class.getCanonicalName();
    }

    @Override
    protected String resolveId(Element el, AbstractBeanDefinition definition, ParserContext parserContext) {
        return el.getAttribute(REF_ATTR)
                + "$export"
                + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR
                + ObjectUtils.getIdentityHexString(definition);
    }

    @Override
    protected void doParse(Element el, ParserContext parserContext, BeanDefinitionBuilder builder) {

        BeanDefinitionRegistry registry = parserContext.getRegistry();
        Resource resource = parserContext.getReaderContext().getResource();

        String rootName = defaultIfBlank(el.getAttribute(ROOT_ATTR), DEFAULT_ROOT_FACTORY_NAME);
        String serviceIfaceName = el.getAttribute(INTERFACE_ATTR);
        String beanName = el.getAttribute(REF_ATTR);
        String serviceName = defaultIfBlank(el.getAttribute(NAME_ATTR), beanName);
        String exportBeanDefName = serviceName + EXPORT_REF_SUFFIX;

        if (registry.containsBeanDefinition(exportBeanDefName)) {
            throw new BeanCreationException("Registry already contains bean with name: " + exportBeanDefName);
        }

        Class<?> serviceIface = ParserUtils.findClassByName(serviceIfaceName, beanName, parserContext);

        BeanDefinition exportRefBeanDef = defineExportRef(serviceName, serviceIface, beanName);
        BeanDefinition exportFactoryBeanDef = defineExportFactoryBean(rootName, exportRefBeanDef, resource);

        exportFactoryBeanDef.setAttribute(EXPORT_BEAN_DEF_ATTR_NAME,
                new BeanReferenceInfo(serviceName, serviceIface, extractResourcePath(resource)));

        registry.registerBeanDefinition(exportBeanDefName, exportFactoryBeanDef);
    }

    /**
     * Creates a {@link ExportRef} bean definition.
     */
    private BeanDefinition defineExportRef(String serviceName, Class<?> serviceIface, String beanName) {
        return new RootBeanDefinition(
                ExportRef.class,
                defineExportRefConstructorArgs(serviceName, serviceIface, beanName),
                null);
    }

    /**
     * Creates arguments definition for a constructor of the {@link ExportRef} class.
     */
    private ConstructorArgumentValues defineExportRefConstructorArgs(String serviceName, Class<?> serviceIface, String beanName) {

        ConstructorArgumentValues holder = new ConstructorArgumentValues();
        holder.addIndexedArgumentValue(0, serviceName);
        holder.addIndexedArgumentValue(1, serviceIface);
        holder.addIndexedArgumentValue(2, beanName);

        return holder;
    }

    /**
     * Creates a factory method bean definition that just invokes
     * {@link Registry#export(ExportRef) export()} method on the specified registry bean.
     *
     * @param registryName
     * @param exportRef The definition of the {@link ExportRef} bean.
     * @param resource The resource that this bean definition came from.
     */
    private BeanDefinition defineExportFactoryBean(String registryName, BeanDefinition exportRef, Resource resource) {

        AbstractBeanDefinition beanDef = rootBeanDefinition(Void.class)
                .setFactoryMethod(Registry.EXPORT_METHOD_NAME)
                .setScope(SCOPE_SINGLETON)
                .setLazyInit(false)
                //.addDependsOn(exportBeanRef) TODO ?
                .getRawBeanDefinition();
        beanDef.setFactoryBeanName(registryName);
        beanDef.setConstructorArgumentValues(
                defineExportMethodArgs(exportRef));
        beanDef.setResource(resource);

        return beanDef;
    }

    /**
     * Creates arguments definition for the {@link Registry#export(ExportRef) export()} method
     * of the registry bean.
     *
     * @param exportRef The definition of the {@link ExportRef} bean.
     */
    private ConstructorArgumentValues defineExportMethodArgs(BeanDefinition exportRef) {

        ConstructorArgumentValues holder = new ConstructorArgumentValues();
        holder.addGenericArgumentValue(exportRef, ExportRef.class.getName());

        return holder;
    }
}
