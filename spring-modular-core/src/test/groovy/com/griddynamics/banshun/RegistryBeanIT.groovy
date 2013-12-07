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

import org.springframework.aop.TargetSource
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.BeanNotOfRequiredTypeException
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import spock.lang.Specification

import java.lang.reflect.Proxy

import static com.griddynamics.banshun.ContextParentBean.BEAN_DEF_SUFFIX
import static com.griddynamics.banshun.ContextParentBean.TARGET_SOURCE_SUFFIX
import static com.griddynamics.banshun.test.TestUtils.initContext

class RegistryBeanIT extends Specification {

    def 'import exported bean'() {
        setup:
            def ctx = initContext(location) as ClassPathXmlApplicationContext
            // there shouldn't be any exports yet due to laziness
            assert hasNoExports(ctx)
            assert hasNoImports(ctx)

        when: 'early-import proxy bean is initialized by context'
            def earlyImport = ctx.getBean('early-import')

        then: 'context should contain bean definition for the bean being imported'
            ctx.containsBeanDefinition('just-bean' + BEAN_DEF_SUFFIX)

        when: 'early-import proxy is invoked without prior export'
            earlyImport.toString()

        then: 'no such bean exception should be thrown'
            def ex = thrown(NoSuchBeanDefinitionException)
            ex.beanName == 'just-bean'

        when: 'export declaration is initialized'
            ctx.getBean('export-declaration')

        then: 'context should contain ExportTargetSource for exported bean'
            ctx.getBean('just-bean' + TARGET_SOURCE_SUFFIX, ExportTargetSource)
                    .beanName == 'just-bean'

        and: 'early-import proxy can be invoked now'
            earlyImport.toString()

        and: 'early-import should be the same when obtained from context again'
            earlyImport.is(ctx.getBean('early-import'))
            earlyImport instanceof Proxy

        when: 'late-import proxy is obtained from context'
            def lateImport = ctx.getBean('late-import')

        then: 'both proxies should refer the same bean instance'
            lateImport.toString() == earlyImport.toString()

        and: 'both proxies should be the same instances'
            lateImport.is(earlyImport)

        where:
            location << [ 'registry/exact-match-import.xml', 'registry/coarse-import.xml' ]
    }

    def 'import bean with wrong type'() {
        setup:
            def ctx = initContext('registry/illegal-concrete-import.xml')

        when: 'early-import proxy is obtained from context'
            def earlyImport = ctx.getBean('early-import')

        and: 'export declaration is initialized'
            ctx.getBean('export-declaration')

        and: 'early-import proxy is invoked'
            earlyImport.toString()

        then: 'types does not match'
            def ex = thrown(BeanNotOfRequiredTypeException)
            ex.beanName == 'just-bean'

        when: 'late-import proxy is obtained and invoked'
            def lateImport = ctx.getBean('late-import')
            lateImport.toString()

        then: 'type does not match'
            def ex2 = thrown(BeanNotOfRequiredTypeException)
            ex2.beanName == 'just-bean'
    }

    def 'misconfigured export'() {
        setup:
            def ctx = initContext('registry/wrong-export-class.xml')
            assert hasNoExports(ctx)

        when: 'early-import proxy is obtained'
            def earlyImport = ctx.getBean('early-import')

        and: 'export declaration is initialized'
            ctx.getBean('export-declaration')

        and: 'early-import proxy is invoked'
            earlyImport.toString()

        then: 'types does not match'
            thrown(BeanNotOfRequiredTypeException)

        when: 'late-import proxy is obtained and invoked'
            def lateImport = ctx.getBean('late-import')
            lateImport.toString()

        then: 'type does not match'
            thrown(BeanNotOfRequiredTypeException)
    }


    private boolean hasNoExports(ApplicationContext ctx) {
        ctx.getBeanNamesForType(TargetSource).each { name ->
            if (name.endsWith(TARGET_SOURCE_SUFFIX)) {
                return false;
            }
        }
        return true
    }

    private boolean hasNoImports(ApplicationContext ctx) {
        ctx.getBeanDefinitionNames().each { name ->
            if (name.endsWith(BEAN_DEF_SUFFIX)) {
                return false
            }
        }
        return true
    }
}
