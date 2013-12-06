/*
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
package com.griddynamics.banshun.config.xml

import com.griddynamics.banshun.ExportRef
import org.springframework.beans.FatalBeanException
import org.springframework.beans.factory.config.BeanDefinition
import spock.lang.Specification

import static com.griddynamics.banshun.ContextParentBean.EXPORT_REF_SUFFIX
import static com.griddynamics.banshun.config.xml.ParserUtils.DEFAULT_ROOT_FACTORY_NAME
import static com.griddynamics.banshun.test.TestUtils.inMemoryBeanDefinitionRegistry

class ExportBeanDefinitionParserTest extends Specification {

    def 'parse XML and populate bean definition'() {
        given:
            def registry = inMemoryBeanDefinitionRegistry(
                    '<bs:export name="exportBean1" ref="bean1" interface="java.lang.String" root="myRoot" />'
            )
            def beanDef = registry.getBeanDefinition('exportBean1' + EXPORT_REF_SUFFIX)
        expect:
            with (beanDef) {
                beanClassName == Void.name
                factoryBeanName == 'myRoot'
                factoryMethodName == 'export'
                lazyInit == false
                scope == SCOPE_SINGLETON

                def constrArgValues = beanDef.constructorArgumentValues
                constrArgValues.argumentCount == 1
                constrArgValues.getGenericArgumentValue(ExportRef).value instanceof BeanDefinition

                def innerBeanDef = constrArgValues.getGenericArgumentValue(ExportRef).value as BeanDefinition
                innerBeanDef.beanClassName == ExportRef.name
                innerBeanDef.lazyInit == false

                def innerConstrArgValues = innerBeanDef.constructorArgumentValues
                innerConstrArgValues.argumentCount == 2
                innerConstrArgValues.getGenericArgumentValue(String).value == 'exportBean1'  // TODO is this correct?
                innerConstrArgValues.getGenericArgumentValue(Class).value == String
            }
    }

    def 'use default root name when "root" attribute missing'() {
        given:
            def beanFactory = inMemoryBeanDefinitionRegistry(
                    '<bs:export name="exportBean1" ref="bean1" interface="java.lang.String" />'
            )
            def beanDef = beanFactory.getBeanDefinition('exportBean1' + EXPORT_REF_SUFFIX)
        expect:
            beanDef.factoryBeanName == DEFAULT_ROOT_FACTORY_NAME
    }


    //TODO weird...?
    def 'use "ref" as export name when "name" attribute missing'() {
        given:
            def beanFactory = inMemoryBeanDefinitionRegistry(
                    '<bs:export ref="bean1" interface="java.lang.String" />'
            )
            def beanDef = beanFactory.getBeanDefinition('bean1' + EXPORT_REF_SUFFIX)
        expect:
            def innerBeanDef = beanDef.constructorArgumentValues.getGenericArgumentValue(ExportRef).value as BeanDefinition

            innerBeanDef.constructorArgumentValues.getGenericArgumentValue(String).value == 'bean1'
    }

    def 'fail when required attribute missing: #name'() {
        when:
            inMemoryBeanDefinitionRegistry(xml)
        then:
            thrown(FatalBeanException)
        where:
            name        | xml
            'ref'       | '<bs:export interface="java.lang.String" />'
            'interface' | '<bs:export ref="bean1" />'
    }
}
