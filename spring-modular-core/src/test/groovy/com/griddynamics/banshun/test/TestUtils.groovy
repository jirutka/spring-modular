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
package com.griddynamics.banshun.test

import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader
import org.springframework.context.ApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.core.io.ByteArrayResource

final class TestUtils {

    public static final BASE_PKG = '/com/griddynamics/banshun'

    public static final IN_MEMORY_RESOURCE_DESC = 'in-memory-resource'

    static final BEANS_XML_HEAD =
        """
        <beans xmlns="http://www.springframework.org/schema/beans"
               xmlns:bs="http://www.griddynamics.com/schema/banshun"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="
               http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
               http://www.griddynamics.com/schema/banshun http://www.griddynamics.com/schema/banshun/banshun-schema.xsd">
        """

    static final BEANS_XML_TAIL =
        """
        </beans>
        """


    static ApplicationContext initContext(String location) {
        new ClassPathXmlApplicationContext("${BASE_PKG}/${location}")
    }

    static BeanDefinitionRegistry inMemoryBeanDefinitionRegistry(String xml) {
        def fullXml = BEANS_XML_HEAD + xml + BEANS_XML_TAIL

        new XmlBeanDefinitionReader(new SimpleBeanDefinitionRegistry()).with {
            it.loadBeanDefinitions(new ByteArrayResource(fullXml.bytes, IN_MEMORY_RESOURCE_DESC)); it
        }.beanFactory
    }
}
