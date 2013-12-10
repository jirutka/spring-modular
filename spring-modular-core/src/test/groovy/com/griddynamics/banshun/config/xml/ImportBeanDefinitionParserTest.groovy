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

import com.griddynamics.banshun.fixtures.Child
import org.springframework.beans.FatalBeanException
import spock.lang.Specification
import spock.lang.Unroll

import static com.griddynamics.banshun.Registry.LOOKUP_METHOD_NAME
import static com.griddynamics.banshun.config.xml.ParserUtils.DEFAULT_ROOT_FACTORY_NAME
import static com.griddynamics.banshun.test.TestUtils.IN_MEMORY_RESOURCE_DESC
import static com.griddynamics.banshun.test.TestUtils.getIN_MEMORY_RESOURCE_DESC
import static com.griddynamics.banshun.test.TestUtils.inMemoryBeanDefinitionRegistry
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON

@Unroll
class ImportBeanDefinitionParserTest extends Specification {

    def 'parse XML and populate bean definition'() {
        given:
            def registry = inMemoryBeanDefinitionRegistry(
                    '<bs:import id="bean1" interface="com.griddynamics.banshun.fixtures.Child" root="myRoot" />'
            )
            def beanDef = registry.getBeanDefinition('bean1')
        expect:
            with (beanDef) {
                factoryBeanName == 'myRoot'
                factoryMethodName == LOOKUP_METHOD_NAME
                lazyInit == true
                scope == SCOPE_SINGLETON
                resourceDescription == IN_MEMORY_RESOURCE_DESC
            }
            with (beanDef.constructorArgumentValues) {
                argumentCount == 2
                getIndexedArgumentValue(0, String).value == 'bean1'
                getIndexedArgumentValue(1, Class).value == Child
            }
    }

    def 'use default root name when "root" attribute missing'() {
        given:
            def registry = inMemoryBeanDefinitionRegistry(
                    '<bs:import id="bean2" interface="com.griddynamics.banshun.fixtures.Child" />'
            )
            def beanDef = registry.getBeanDefinition('bean2')
        expect:
            beanDef.factoryBeanName == DEFAULT_ROOT_FACTORY_NAME
    }

    def 'fail when required attribute missing: #name'() {
        when:
            inMemoryBeanDefinitionRegistry(xml)
        then:
           thrown(FatalBeanException)
        where:
            name        | xml
            'id'        | '<bs:import id="bean1" />'
            'interface' | '<bs:import interface="java.lang.String" />'
    }
}
