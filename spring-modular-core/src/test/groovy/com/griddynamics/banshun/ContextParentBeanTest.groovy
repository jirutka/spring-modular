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
import com.griddynamics.banshun.test.TestUtils
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.ContextStoppedEvent
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.core.io.ClassPathResource
import spock.lang.Specification

import static com.griddynamics.banshun.test.TestUtils.BASE_PKG
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE

class ContextParentBeanTest extends Specification {

    static wbase = "${BASE_PKG}/wildcards"

    def locations = [ "${BASE_PKG}/ctx1.xml", "${BASE_PKG}/ctx2.xml" ] as String[]

    def beanFactory = Mock(DefaultListableBeanFactory)

    def rootContext = Mock(AbstractApplicationContext, {
        it.beanFactory >> beanFactory
        it.getResources(_) >> { String location ->
            new ClassPathResource(location)
        }
    })

    def parentBeanSpy = Spy(ContextParentBean, {
        it.applicationContext = rootContext
        it.resultConfigLocations = locations
    })


    def 'resolve locations with wildcards'() {
        setup:
            def parentBean = initAndGetRegistry('wildcards/root-ctx.xml')
            def expected = contextIds.collect { id -> "ctx${id}.xml" }
        when:
            parentBean.configLocations = locations as String[]
            parentBean.afterPropertiesSet()
        then:
            parentBean.resultConfigLocations.size() == expected.size()

            parentBean.resultConfigLocations.each { location ->
                assert expected.find { location.endsWith(it) },
                        "Location '$location' in not expected here."
            }
        where:
            locations                                                           || contextIds
            [ "$wbase/*7.xml" ]                                                 || [7]
            [ "$wbase/*/*6.xml" ]                                               || [6]
            [ "$wbase/sub1/ctx5*.xml" ]                                         || [5]
            [ "classpath:$wbase/ctx7.xml", "classpath:$wbase/sub2/ctx*.xml" ]   || [7, 2, 4, 6]
            [ "$wbase/ctx7.xml", "classpath:$wbase/ctx7.xml" ]                  || [7]
            [ "$wbase/sub1/ctx*", "$wbase/ctx8.xml" ]                           || [1, 3, 5, 8]
            [ "$wbase/sub1/ctx1.xml", "$wbase/sub2/*.xml",
              "$wbase/sub1/*.xml", "$wbase/sub2/ctx6.xml", "$wbase/ctx*.xml" ]  || 1..8
    }

    def 'exclude locations'() {
        setup:
            def parentBean = initAndGetRegistry('wildcards/root-ctx.xml')
            def expected = contextIds.collect { id -> "ctx${id}.xml" }
        when:
            parentBean.excludeConfigLocations = excludeLocations
            parentBean.configLocations = locations
            parentBean.afterPropertiesSet()
        then:
            parentBean.resultConfigLocations.size() == contextIds.size()

            parentBean.resultConfigLocations.each { location ->
                assert expected.find { location.endsWith(it) }, "Location '$location' is not expected here."
            } || true
        where:
            locations                                                       | excludeLocations       || contextIds
            [ "$wbase/*/*.xml" ]                                            | [ "$wbase/ctx*.xml" ]  || 1..6
            [ "$wbase/sub1/*.xml", "$wbase/sub2/*.xml", "$wbase/ctx*.xml" ] | [ "$wbase/ctx*.xml" ]  || 1..6
            [ "$wbase/ctx*" ]                                               | [ "$wbase/ctx*.xml" ]  || []
    }


    def 'export bean for the first time'() {
        setup:
            def registry = new ContextParentBean(applicationContext: rootContext)
            def childBeanFactory = Mock(BeanFactory)
            def exportRef = new ExportRef('export1', RootFace, 'bean1', childBeanFactory)
            ExportTargetSource targetSource = null
        when:
            registry.export(exportRef)
        then:
            1 * rootContext.containsBean('export1_targetSource') >> false
            1 * beanFactory.registerSingleton('export1_targetSource', { targetSource = it })
        and:
            targetSource.targetBeanName == 'bean1'
            targetSource.targetClass == RootFace
            targetSource.beanFactory == childBeanFactory
    }

    def 'export already exported bean'() {
        setup:
            def registry = new ContextParentBean(applicationContext: rootContext)
        when:
            registry.export(new ExportRef('bean1', RootFace, 'export1'))
        then:
            1 * rootContext.containsBean(_) >> true
            0 * beanFactory._
    }

    def 'lookup bean for the first time'() {
        setup:
            def registry = new ContextParentBean(applicationContext: rootContext)
            def expectedTargetSource = new LookupTargetSource('export1', 'export1_targetSource', JustBean, rootContext)
            def expectedResult = new JustBean()
            def RootBeanDefinition registeredBeanDef
        when:
            def result = registry.lookup('export1', JustBean)
        then:
            1 * rootContext.containsBean('export1_beanDef') >> false
        then:
            1 * beanFactory.registerBeanDefinition('export1_beanDef', { registeredBeanDef = it })
        and:
            registeredBeanDef.beanClass == ProxyFactoryBean
            registeredBeanDef.role == ROLE_INFRASTRUCTURE
            registeredBeanDef.propertyValues.size() == 1

            def targetSource = registeredBeanDef.propertyValues.getPropertyValue('targetSource').value
            targetSource == expectedTargetSource
        then:
            1 * rootContext.getBean('export1_beanDef', JustBean) >> expectedResult
        and:
            result == expectedResult
    }

    def 'lookup already registered bean'() {
        setup:
            def registry = new ContextParentBean(applicationContext: rootContext)
            def expected = new JustBean()
        when:
            def actual = registry.lookup('export1', JustBean)
        then:
            1 * rootContext.containsBean('export1_beanDef') >> true
            0 * beanFactory._
            1 * rootContext.getBean('export1_beanDef', JustBean) >> expected
        and:
           actual == expected
    }

    def 'invoke children contexts initialization on ContextRefreshedEvent'() {
        given:
            def refreshEvent = new ContextRefreshedEvent(rootContext)
            def anotherContext = Mock(AbstractApplicationContext)
            def refreshEventForAnotherContext = new ContextRefreshedEvent(anotherContext)
            def anotherEvent = new ContextStoppedEvent(rootContext)

        when: 'given ContextRefreshedEvent for our context'
            parentBeanSpy.onApplicationEvent(refreshEvent)
        then:
            1 * parentBeanSpy.initializeChildContexts() >> null

        when: 'given ContextRefreshedEvent for another context'
            parentBeanSpy.onApplicationEvent(refreshEventForAnotherContext)
        then:
            0 * parentBeanSpy.initializeChildContexts() >> null

        when: 'given event for other type then ContextRefreshedEvent'
            parentBeanSpy.onApplicationEvent(anotherEvent)
        then:
            0 * parentBeanSpy.initializeChildContexts() >> null
    }

    def 'destroy children contexts'() {
        given:
            def children = [ Mock(ConfigurableApplicationContext), Mock(ConfigurableApplicationContext) ]
            def registry = new ContextParentBean(children: children)
        when:
            registry.destroy()
        then:
            1 * children[1].close()
        then:
            1 * children[0].close()
    }


    def 'initialize child contexts'() {
        setup:
            def childContext = Mock(ConfigurableApplicationContext)
            def resources = locations.collect { new ClassPathResource(it) }
        when:
            parentBeanSpy.initializeChildContexts()
        then:
            resources.each { resource ->
                1 * parentBeanSpy.createChildContext({ it == resource }, rootContext) >> childContext
            }
        and:
            parentBeanSpy.children.size() == resources.size()
    }

    def 'initialize child contexts when first fails and second succeeds'() {
        setup:
            def firstChildException = new Exception('child failed!')
            def secondChildCtx = Mock(ConfigurableApplicationContext)

        when: 'initialization is invoked with two locations'
            parentBeanSpy.initializeChildContexts()

        then: 'first child context failed'
            1 * parentBeanSpy.createChildContext(_, rootContext) >> { throw firstChildException }

        and: 'its exception is added to map'
            parentBeanSpy.nestedContextsExceptions.size() == 1
            parentBeanSpy.nestedContextsExceptions[ locations[0] ] == firstChildException

        then: 'second child context succeed'
            1 * parentBeanSpy.createChildContext(_, rootContext) >> secondChildCtx

        and: 'is added to children list'
            parentBeanSpy.children.size() == 1
            parentBeanSpy.children.contains(secondChildCtx)
    }

    def 'initialize child contexts when first fails and strict mode is enabled'() {
        setup:
            parentBeanSpy.strictErrorHandling = true

            1 * parentBeanSpy.createChildContext(_, rootContext) >> { throw new Exception() }
        when:
            parentBeanSpy.initializeChildContexts()
        then:
            thrown(RuntimeException)
    }


    protected initAndGetRegistry(String location) {
        def context = TestUtils.initContext(location)
        context.getBean('root', ContextParentBean)
    }
}
