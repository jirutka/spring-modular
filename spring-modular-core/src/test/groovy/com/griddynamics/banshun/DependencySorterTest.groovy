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

import spock.lang.Specification
import com.griddynamics.banshun.DependencySorter.Location

class DependencySorterTest extends Specification {

    def 'seamless contexts'() {
        given:
            def builder = new DependencySorterBuilder()
                .location('module1.xml')
                    .addExport('TestBean1', Integer)

                .location('module2.xml')
                    .addImport('TestBean1', Object)
                    .addExport('TestBean2', Integer)

                .location('module3.xml')
                    .addImport('TestBean1', Object)
                    .addImport('TestBean2', Object)
                    .addExport('TestBean3', Integer)

                .location('module4.xml')
                    .addImport('TestBean1', Object)
                    .addImport('TestBean2', Object)
                    .addImport('TestBean3', Object)
                .build()
            def sorter = builder.createDependencySorter()
        expect:
            sorter.sort() == builder.locations
    }

    def 'contexts with redundant exports'() {
        given:
            def builder = new DependencySorterBuilder()
                .location('module1.xml')
                    .addExport('TestBean1', Integer)
                    .addExport('Redundant1', Integer)

                .location('module2.xml')
                    .addImport('TestBean1', Object)
                    .addExport('TestBean2', Integer)
                    .addExport('Redundant2', Integer)

                .location('module3.xml')
                    .addImport('TestBean1', Object)
                    .addImport('TestBean2', Object)
                    .addExport('TestBean3', Integer)
                    .addExport('Redundant3', Integer)

                .location('module4.xml')
                    .addImport('TestBean1', Object)
                    .addImport('TestBean2', Object)
                    .addImport('TestBean3', Object)
                    .addExport('Redundant4', Integer)
                .build()
            def sorter = builder.createDependencySorter()
        expect:
            sorter.sort() == builder.locations
    }

    def 'reorder locations to solve conflicts'() {
        given:
            def builder = new DependencySorterBuilder()
                .location('module1.xml')
                    .addExport('TestBean1', Integer)

                .location('module2.xml')
                    .addImport('TestBean1', Object)
                    .addImport('TestBean3', Object)
                    .addExport('TestBean2', Integer)

                .location('module3.xml')
                    .addImport('TestBean1', Object)
                    .addExport('TestBean3', Integer)

                .location('module4.xml')
                    .addImport('TestBean1', Object)
                    .addImport('TestBean2', Object)
                    .addImport('TestBean3', Object)
                    .addExport('Redundant4', Object)
                .build()
            def sorter = builder.createDependencySorter()
        expect:
            sorter.sort() == ['module1.xml', 'module3.xml', 'module2.xml', 'module4.xml']
    }


    def 'contexts with conflicts'() {
        given:
            def builder = new DependencySorterBuilder()
                .location('module1.xml')
                    .addImport('TestBean2', Integer)
                    .addExport('TestBean1', Integer)

                .location('module2.xml')
                    .addExport('TestBean2', Integer)

                .location('module3.xml')
                    .addImport('TestBean1', Object)
                    .addImport('TestBean4', Object)
                    .addExport('TestBean3', Integer)

                .location('module4.xml')
                    .addImport('TestBean1', Object)
                    .addImport('TestBean2', Object)
                    .addImport('TestBean3', Object)
                    .addExport('TestBean4', Integer)
                    .addExport('Redundant4', Integer)

                .location('module5.xml')
                    .addImport('TestBean1', Object)
                    .addImport('TestBean2', Object)
                    .addImport('TestBean3', Object)
                    .addExport('Redundant5', Integer)
                .build()
            def sorter = builder.createDependencySorter()
        when:
            sorter.prohibitCycles = false
            def actual = sorter.sort()
        then:
            actual == ['module2.xml', 'module1.xml', 'module3.xml', 'module4.xml', 'module5.xml']
            sorter.conflictContextGroup == ['module3.xml', 'module4.xml']
    }



    ////// Builder //////

    static class DependencySorterBuilder {
        final modules = new ArrayList<Location>()

        ModuleBuilder location(name) {
            new ModuleBuilder(locationName: name)
        }

        DependencySorter createDependencySorter() {
            new DependencySorter(locations, imports, exports)
        }

        String[] getLocations() {
            modules*.locationName
        }

        Map<String, BeanReferenceInfo> getExports() {
            modules*.exportBeans.flatten().collectEntries{ [it.beanName, it] }
        }

        Map<String, List<BeanReferenceInfo>> getImports() {
            modules*.importBeans.flatten().groupBy{ it.beanName }
        }

        class ModuleBuilder {
            private locationName = ''
            private imports = new HashSet()
            private exports = new HashSet()

            def addExport(String beanName, Class<?> beanIface) {
                exports.add(new BeanReferenceInfo(beanName, beanIface, locationName))
                return this
            }

            def addImport(String beanName, Class<?> beanIface) {
                imports.add(new BeanReferenceInfo(beanName, beanIface, locationName))
                return this
            }

            def location(name) {
                build()
                return new ModuleBuilder(locationName: name)
            }

            def build() {
                DependencySorterBuilder.this.modules << new Location(locationName, imports, exports)
                DependencySorterBuilder.this
            }
        }
    }
}
