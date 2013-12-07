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
import com.griddynamics.banshun.fixtures.RootFace
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import spock.lang.Ignore
import spock.lang.Specification

class LookupTargetSourceTest extends Specification {

    def rootContext = Mock(ApplicationContext)
    def exportTargetSource = Mock(ExportTargetSource)
    def beanName = 'bean1'
    def exportProxyName = 'bean1_targetSource'
    def lookupTargetSource = new LookupTargetSource(beanName, exportProxyName, RootFace, rootContext)
    def expected = new JustBean()


    def 'throw exception when root context does not contain target bean'() {
        setup:
            rootContext.containsBean(_) >> false
        when:
            lookupTargetSource.getTarget()
        then:
            thrown(NoSuchBeanDefinitionException)
    }

    @Ignore('getTarget() needs some refactoring')
    def 'find target source in context and return its target'() {
        setup:
            rootContext.containsBean(_) >> true
        when:
            def actual = lookupTargetSource.getTarget()
        then:
            1 * rootContext.getBean(exportProxyName, *_) >> exportTargetSource
            1 * exportTargetSource.getTarget() >> expected
        and:
            actual == expected
    }

    @Ignore('getTarget() needs some refactoring')
    def 'return cached instance when invoked again'() {
        setup:
            rootContext.containsBean(_) >> true

        when: 'invoked for the first time'
            lookupTargetSource.getTarget()

        then: 'obtain target source and the bean'
            1 * rootContext.getBean(exportProxyName, *_) >> exportTargetSource
            1 * exportTargetSource.getTarget() >> expected

        when: 'invoked again'
            def actual = lookupTargetSource.getTarget()

        then: 'just return already obtained bean'
            actual == expected
    }
}
