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

import com.griddynamics.banshun.fixtures.RootFace
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class LookupTargetSourceCreatorTest extends Specification {

    def context = Mock(ApplicationContext)
    def creator = new LookupTargetSourceCreator(context: context)


    def 'create target source'() {
        when:
            def targetSource = creator.getTargetSource(RootFace, 'bean1_beanDef')
        then:
            targetSource != null
            targetSource.context == context
            targetSource.targetBeanName == 'bean1_targetSource'
            targetSource.targetClass == RootFace
    }
}
