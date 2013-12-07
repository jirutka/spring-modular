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
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanNotOfRequiredTypeException
import spock.lang.Specification

class ExportTargetSourceTest extends Specification {

    def beanFactory = Mock(BeanFactory)
    def targetSource = new ExportTargetSource(new ExportRef('service1', MiddleFace, 'bean1', beanFactory))


    def 'find target bean in context and return it'() {
        setup:
            beanFactory.getType('bean1') >> type
        when:
            def actual = targetSource.getTarget()
        then:
            1 * beanFactory.getBean('bean1') >> expected
            actual == expected
        where:
            expected                        | type
            new JustBean()                  | JustBean
            new JustBean()                  | MiddleFace
            new MiddleFace(){ String name } | MiddleFace
    }

    def 'return cached instance when invoked again'() {
        setup:
            beanFactory.getType('bean1') >> MiddleFace
            def expected = new JustBean()

        when: 'invoked for the first time'
            targetSource.getTarget()

        then: 'obtain bean from context'
            1 * beanFactory.getBean('bean1') >> expected

        when: 'invoked again'
            def actual = targetSource.getTarget()

        then: 'just return already obtained bean'
            0 * beanFactory._
            actual == expected
    }

    def 'throw exception when service interface is not assignable from bean type'() {
        setup:
            beanFactory.getType('bean1') >> type
            beanFactory.getBean('bean1') >> bean
        when:
            targetSource.getTarget()
        then:
            def ex = thrown(BeanNotOfRequiredTypeException)
            ex.beanName == 'bean1'
            ex.requiredType == MiddleFace
            ex.actualType == type
        where:
            bean              | type
            new RootFace(){}  | RootFace
           'string'          | String
    }
}
