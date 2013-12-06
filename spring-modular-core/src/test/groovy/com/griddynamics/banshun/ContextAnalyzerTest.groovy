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
import org.springframework.beans.factory.BeanCreationException
import spock.lang.Specification
import spock.lang.Unroll

class ContextAnalyzerTest extends Specification {

    def analyzer = new ContextAnalyzer()


    def 'put and retrieve imports'() {
        setup:
            def refs = ['bean1', 'bean2', 'bean1'].collect { name ->
                new BeanReferenceInfo(name, RootFace, 'ctx1')
            }
            def refsByName = refs.groupBy { it.beanName }
        when:
            refs.each { analyzer.putInImports(it) }
        then:
            analyzer.imports == refsByName
    }

    def 'put and retrieve exports'() {
        setup:
            def refs = ['bean1', 'bean2'].collect { name ->
                new BeanReferenceInfo(name, RootFace, 'ctx1')
            }
            def refsByName = refs.collectEntries { [(it.beanName): it] }
        when:
            refs.each { analyzer.putInExports(it) }
        then:
            analyzer.exports == refsByName
    }

    def 'throws exception when duplicated export is added'() {
        setup:
            analyzer.putInExports(new BeanReferenceInfo('bean1', RootFace, 'ctx1'))
        when:
            analyzer.putInExports(secondRef)
        then:
            thrown(BeanCreationException)
        where:
            secondRef << [
                new BeanReferenceInfo('bean1', RootFace, 'ctx1'),
                new BeanReferenceInfo('bean1', RootFace, 'ctx2'),
                new BeanReferenceInfo('bean1', String, 'ctx1'),
            ]
    }

    def 'are there imports without export'() {
        setup:
            assert ! analyzer.areThereImportsWithoutExports()
        when:
            analyzer.putInImports(new BeanReferenceInfo('bean1', RootFace, 'ctx2'))
            analyzer.putInExports(new BeanReferenceInfo('bean2', RootFace, 'ctx2'))
        then:
            analyzer.areThereImportsWithoutExports()
        when:
            analyzer.putInExports(new BeanReferenceInfo('bean1', MiddleFace, 'ctx3'))
        then:
            ! analyzer.areThereImportsWithoutExports()
    }

    def 'are there exports without import'() {
        setup:
            assert ! analyzer.areThereExportsWithoutImport()
        when:
            analyzer.putInExports(new BeanReferenceInfo('bean1', RootFace, 'ctx2'))
            analyzer.putInImports(new BeanReferenceInfo('bean2', RootFace, 'ctx2'))
        then:
            analyzer.areThereExportsWithoutImport()
        when:
            analyzer.putInImports(new BeanReferenceInfo('bean1', MiddleFace, 'ctx3'))
        then:
            ! analyzer.areThereExportsWithoutImport()
    }

    @Unroll
    def 'verify import types when #description'() {
        setup: 'add some seed'
            analyzer.putInImports(new BeanReferenceInfo('bean2', Long, 'ctx1'))
            analyzer.putInExports(new BeanReferenceInfo('bean3', String, 'ctx1'))
            assert analyzer.areImportsTypesCorrect()
        when:
            importTypes.each { type ->
                analyzer.putInImports(new BeanReferenceInfo('bean1', type, 'ctx1'))
            }
            analyzer.putInExports(new BeanReferenceInfo('bean1', exportType, 'ctx2'))
        then:
            analyzer.areImportsTypesCorrect() == result
        where:
            importTypes           | exportType | result || description
            [RootFace, RootFace]  | RootFace   | true   || 'imports and export are same'
            [RootFace]            | JustBean   | true   || 'import is supertype of export'
            [JustBean]            | RootFace   | false  || 'first import is subtype of export'
            [RootFace, JustBean]  | RootFace   | false  || 'second import is subtype of export'
            [Integer, String]     | String     | false  || 'first import differs from export'
    }

}
