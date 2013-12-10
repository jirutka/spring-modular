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

import com.griddynamics.banshun.BeanReferenceInfo
import com.griddynamics.banshun.ExportRef
import org.springframework.beans.FatalBeanException
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.BeanDefinitionStoreException
import org.springframework.beans.factory.config.BeanDefinition
import spock.lang.Specification

import static com.griddynamics.banshun.ContextParentBean.EXPORT_REF_SUFFIX
import static com.griddynamics.banshun.Registry.EXPORT_METHOD_NAME
import static com.griddynamics.banshun.config.xml.ParserUtils.DEFAULT_ROOT_FACTORY_NAME
import static com.griddynamics.banshun.config.xml.ParserUtils.EXPORT_BEAN_DEF_ATTR_NAME
import static com.griddynamics.banshun.test.TestUtils.IN_MEMORY_RESOURCE_DESC
import static com.griddynamics.banshun.test.TestUtils.inMemoryBeanDefinitionRegistry

class ExportBeanDefinitionParserTest extends Specification {

    def 'parse XML and populate bean definition'() {
        given:
            def registry = inMemoryBeanDefinitionRegistry(
                    '<bs:export name="export1" ref="bean1" interface="java.lang.String" root="myRoot" />'
            )
            def beanDef = registry.getBeanDefinition('export1' + EXPORT_REF_SUFFIX)
            def expectedRefInfo = new BeanReferenceInfo('export1', String, IN_MEMORY_RESOURCE_DESC)
        expect:
            with (beanDef) {
                beanClassName == Void.name
                factoryBeanName == 'myRoot'
                factoryMethodName == EXPORT_METHOD_NAME
                lazyInit == false
                scope == SCOPE_SINGLETON
                resourceDescription == IN_MEMORY_RESOURCE_DESC
                getAttribute(EXPORT_BEAN_DEF_ATTR_NAME) == expectedRefInfo

                def constrArgValues = beanDef.constructorArgumentValues
                constrArgValues.argumentCount == 1
                constrArgValues.getGenericArgumentValue(ExportRef).value instanceof BeanDefinition

                def innerBeanDef = constrArgValues.getGenericArgumentValue(ExportRef).value as BeanDefinition
                innerBeanDef.beanClassName == ExportRef.name
                innerBeanDef.lazyInit == false

                def innerConstrArgValues = innerBeanDef.constructorArgumentValues
                innerConstrArgValues.argumentCount == 3
                innerConstrArgValues.getIndexedArgumentValue(0, String).value == 'export1'
                innerConstrArgValues.getIndexedArgumentValue(1, Class).value == String
                innerConstrArgValues.getIndexedArgumentValue(2, String).value == 'bean1'
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

    def 'use "ref" as service name when "name" attribute missing'() {
        given:
            def beanFactory = inMemoryBeanDefinitionRegistry(
                    '<bs:export ref="bean1" interface="java.lang.String" />'
            )
            def beanDef = beanFactory.getBeanDefinition('bean1' + EXPORT_REF_SUFFIX)
        expect:
            def innerBeanDef = beanDef.constructorArgumentValues.getGenericArgumentValue(ExportRef).value as BeanDefinition

            innerBeanDef.constructorArgumentValues.getIndexedArgumentValue(0, String).value == 'bean1'
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

    def 'fail when context already contains bean of name NAME-export-ref'() {
        when:
            inMemoryBeanDefinitionRegistry("""
                    <bean id="bean1${EXPORT_REF_SUFFIX}" class="java.lang.String" />
                    <bs:export ref="bean1" interface="java.lang.String" />
            """)
        then:
            def ex = thrown(BeanDefinitionStoreException)
            ex.rootCause instanceof BeanCreationException
    }
}
