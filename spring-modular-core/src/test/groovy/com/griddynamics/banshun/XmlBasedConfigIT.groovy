package com.griddynamics.banshun

import com.griddynamics.banshun.fixtures.Parent
import com.griddynamics.banshun.test.InMemoryXmlApplicationContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * TODO
 * - test exports with different ref and name
 * - test import without export?
 */
@Unroll
class XmlBasedConfigIT extends Specification {

    def 'modules are in #description [#className]'() {
        setup:
            def ctx0 = initParentContext(parentBeanClass, configs as String[])

        expect:
            ctx0.containsBean('root')
            def root = ctx0.getBean('root', ContextParentBean)

            root.children.size() == 3
            def (ctx1, ctx2, ctx3) = root.children

        and: 'context 1'
            ctx1.containsBean('exportA1')
            def beanA1 = ctx1.getBean('exportA1')

        and: 'context 2'
            ctx2.containsBean('exportA1')
            ctx2.getBean('exportA1').toString() == beanA1.toString()

            ctx2.containsBean('exportB1')
            def beanB1 = ctx2.getBean('exportB1')

        and: 'context 3'
            ctx3.containsBean('exportA1')
            ctx3.getBean('exportA1').toString() == beanA1.toString()

            ctx3.containsBean('exportB1')

            ctx3.containsBean('useExportB1')
            ctx3.getBean('useExportB1', Parent)
                .child.toString() == beanB1.toString()

        where:
            parentBeanClass         | configs                   || description
            ContextParentBean       | ['ctx1', 'ctx2', 'ctx3']  || 'correct order without conflicts'
            StrictContextParentBean | ['ctx1', 'ctx2', 'ctx3']  || 'correct order without conflicts'
            StrictContextParentBean | ['ctx2', 'ctx1', 'ctx3']  || 'wrong order'

            className = parentBeanClass.simpleName
    }

    def 'modules contains cyclic dependency and cycles are allowed [#className]'() {
        setup:
            def params = parentBeanClass == StrictContextParentBean ? [prohibitCycles: false] : [:]
            def ctx0 = initParentContext(params, parentBeanClass, 'ctx4', 'ctx5')

        expect:
            ctx0.containsBean('root')
            def root = ctx0.getBean('root', ContextParentBean)

            root.children.size() == 2
            def (ctx4, ctx5) = root.children

        and: 'context 4'
            ctx4.containsBean('exportD1')
            def beanD1 = ctx4.getBean('exportD1')

            ctx4.containsBean('useExportE1')
            def importE1 = ctx4.getBean('useExportE1', Parent).child

        and: 'context 5'
            ctx5.containsBean('exportD1')
            ctx5.getBean('exportD1').toString() == beanD1.toString()

            ctx5.containsBean('exportE1')
            ctx5.getBean('exportE1').toString() == importE1.toString()

        where:
            parentBeanClass << [ContextParentBean, StrictContextParentBean]
            className = parentBeanClass.simpleName
    }

    def 'one of modules fails and strict error handling is disabled [StrictContextParentBean]'() {
        setup:
            def ctx0 = initParentContext(StrictContextParentBean, 'ctx1', 'ctx6', 'ctx2', 'ctx7',
                                         strictErrorHandling: false)
            assert ctx0.containsBean('root')
            def root = ctx0.getBean('root', ContextParentBean)
        expect:
            root.children.size() == 2
            root.ignoredLocations.size() == 2
    }

    //TODO is this correct behaviour?
    def 'one of modules fails and strict error handling is disabled [ContextParentBean]'() {
        setup:
            def ctx0 = initParentContext(ContextParentBean, 'ctx1', 'ctx6', 'ctx2', 'ctx7',
                                         strictErrorHandling: false)
            assert ctx0.containsBean('root')
            def root = ctx0.getBean('root', ContextParentBean)

            assert root.children.size() == 3
            def ctx7 = root.children[2]

        when: 'try to invoke bean imported from failed module'
            ctx7.getBean('exportF1').toString()

        then:
            thrown(Exception)
    }

    def 'one of modules fails and strict error handling is enabled [#className]'() {
        when:
            initParentContext(parentBeanClass, 'ctx1', 'ctx6', 'ctx2', 'ctx7',
                              strictErrorHandling: true)
        then:
            thrown(Exception)
        where:
            parentBeanClass << [ContextParentBean, StrictContextParentBean]
            className = parentBeanClass.simpleName
    }


    static initParentContext(Map props = [:], Class parentBeanClass, String... configs) {
        def paths = configs.collect { "/com/griddynamics/banshun/it/${it}.xml" }

        def defaultProps = [configLocations: paths.join(','), strictErrorHandling: true]
        def propertyElements = (defaultProps + props).collect { key, val ->
            "<property name='${key}' value='${val}' />"
        }.join('\n')

        def xml =
            """
            <bean id="root" class="${parentBeanClass.name}">
                  ${propertyElements}
            </bean>

            <bean id="proxyCreator" class="org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator">
                <property name="customTargetSourceCreators">
                    <bean class="com.griddynamics.banshun.LookupTargetSourceCreator"/>
                </property>
            </bean>
            """
        new InMemoryXmlApplicationContext(xml)
    }
}
