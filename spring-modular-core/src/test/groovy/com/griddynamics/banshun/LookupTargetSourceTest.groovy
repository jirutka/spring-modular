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
package com.griddynamics.banshun

import com.griddynamics.banshun.fixtures.JustBean
import com.griddynamics.banshun.fixtures.MiddleFace
import com.griddynamics.banshun.fixtures.RootFace
import org.springframework.beans.factory.BeanNotOfRequiredTypeException
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class LookupTargetSourceTest extends Specification {

    def rootContext = Mock(ApplicationContext)
    def exportTargetSource = Mock(ExportTargetSource)
    def serviceName = 'service1'
    def exportProxyName = 'service1_targetSource'
    def lookupTargetSource = new LookupTargetSource(serviceName, MiddleFace, exportProxyName, rootContext)


    def 'find target source in context and return its target'() {
        setup:
            rootContext.containsBean(_) >> true
            exportTargetSource.getTargetClass() >> type
        when:
            def actual = lookupTargetSource.getTarget()
        then:
            1 * rootContext.getBean(exportProxyName, *_) >> exportTargetSource
            1 * exportTargetSource.getTarget() >> expected
        and:
            actual == expected
        where:
            expected                        | type
            new JustBean()                  | JustBean
            new JustBean()                  | MiddleFace
            new MiddleFace(){ String name } | MiddleFace
    }

    def 'return cached instance when invoked again'() {
        setup:
            rootContext.containsBean(_) >> true
            exportTargetSource.getTargetClass() >> MiddleFace
            def expected = new JustBean()

        when: 'invoked for the first time'
            lookupTargetSource.getTarget()

        then: 'obtain target source and the bean'
            1 * rootContext.getBean(exportProxyName, *_) >> exportTargetSource
            1 * exportTargetSource.getTarget() >> expected

        when: 'invoked again'
            def actual = lookupTargetSource.getTarget()

        then: 'just return already obtained bean'
           0 * rootContext._
           actual == expected
    }

    def 'throw exception when root context does not contain target bean'() {
        setup:
            rootContext.containsBean(_) >> false
        when:
            lookupTargetSource.getTarget()
        then:
            thrown(NoSuchBeanDefinitionException)
    }

    def 'throw exception when export and import interfaces are not compatible'() {
        setup:
            rootContext.containsBean(_) >> true
            rootContext.getBean(*_) >> exportTargetSource
            exportTargetSource.getTargetClass() >> exportType
        when:
            lookupTargetSource.getTarget()
        then:
            def ex = thrown(BeanNotOfRequiredTypeException)
            ex.beanName == serviceName
            ex.requiredType == MiddleFace
            ex.actualType == exportType
        where:
            exportType << [RootFace, String]
    }
}
