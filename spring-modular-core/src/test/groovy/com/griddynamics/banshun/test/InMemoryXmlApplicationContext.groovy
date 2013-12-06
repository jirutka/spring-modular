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
package com.griddynamics.banshun.test

import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource

class InMemoryXmlApplicationContext extends ClassPathXmlApplicationContext {

    private final Resource inMemoryXml


    public InMemoryXmlApplicationContext(String xml) {
        this(xml, true);
    }

    public InMemoryXmlApplicationContext(String xml, boolean addBeansTags) {
        def fullXml = addBeansTags ? TestUtils.BEANS_XML_HEAD + xml + TestUtils.BEANS_XML_TAIL : xml

        this.inMemoryXml = new ByteArrayResource(fullXml.bytes)
        refresh()
    }

    @Override
    protected Resource[] getConfigResources() {
        [ inMemoryXml ]
    }
}
