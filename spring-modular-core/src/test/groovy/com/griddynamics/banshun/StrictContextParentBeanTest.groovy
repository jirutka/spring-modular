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

class StrictContextParentBeanTest extends ContextParentBeanTest {

    def 'skip failed contexts'() {
        setup:
            def parentBean = initAndGetRegistry('skipcontext/root-ctx.xml') as StrictContextParentBean
            def expected = [2, 3, 4, 5, 6].collect { "ctx${it}.xml" }
        expect:
            parentBean.ignoredLocations.size() == expected.size()
            parentBean.ignoredLocations.each { location ->
                assert expected.find { location.endsWith(it) }, "Location '$location' in not expected here."
            }
            parentBean.children.size() == 2
    }

    def 'analyze dependencies'() {

    }
}
