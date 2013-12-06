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

import com.griddynamics.banshun.fixtures.MiddleFace
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration('autoproxy/root-context.xml')
class AutoProxyTest extends Specification {

    @Autowired Registry registry

    def "bean's method getName should be intercepted"() {
        when:
            def bean = registry.lookup('someBean', MiddleFace)
        then:
            bean.name == 'AroundMethod: Schrodinger'
    }


    ////// Fixtures //////

    static class AroundMethod implements MethodInterceptor {
        def invoke(MethodInvocation methodInvocation) {
            "AroundMethod: ${methodInvocation.proceed()}" as String
        }
    }
}
