/*
 * Copyright 2012 Grid Dynamics Consulting Services, Inc.
 *      http://www.griddynamics.com
 *
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
package com.griddynamics.banshun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.griddynamics.banshun.config.xml.ParserUtils.EXPORT_BEAN_DEF_ATTR_NAME;
import static com.griddynamics.banshun.config.xml.ParserUtils.IMPORT_BEAN_DEF_ATTR_NAME;

public class ContextAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ContextAnalyzer.class);

    private Map<String, BeanReferenceInfo> exports = new HashMap<>();
    private Map<String, List<BeanReferenceInfo>> imports = new HashMap<>();


    public Map<String, List<BeanReferenceInfo>> getImports() {
        return imports;
    }

    public void addImport(BeanDefinition beanDefinition) {
        putInImports(extractImportReference(beanDefinition));
    }

    public Map<String, BeanReferenceInfo> getExports() {
        return exports;
    }

    public void addExport(BeanDefinition beanDefinition) throws BeanCreationException {
        putInExports(extractExportReference(beanDefinition));
    }

    public boolean areThereImportsWithoutExports() {
        boolean hasInvalidImports = false;
        
        for (String serviceName : imports.keySet()) {
            if (!exports.containsKey(serviceName)) {
                hasInvalidImports = true;
                String location = imports.get(serviceName).get(0).getLocation();
                log.error("Unsatisfied import found in {}: there is no service with name '{}'", location, serviceName);
            }
        }
        
        return hasInvalidImports;
    }

    public boolean areThereExportsWithoutImport() {
        boolean hasUnusedExports = false;

        for (String serviceName : exports.keySet()) {
            if (!imports.containsKey(serviceName)) {
                hasUnusedExports = true;
                log.warn("Unused service detected: '{}' is exported but never imported", serviceName);
            }
        }

        return hasUnusedExports;
    }

    public boolean areImportsTypesCorrect() {
        boolean importsTypesAreCorrect = true;
        
        for (String exportName : exports.keySet()) {
            if (imports.containsKey(exportName)) {
                Class<?> exportIface = exports.get(exportName).getServiceInterface();

                for (BeanReferenceInfo importRef : imports.get(exportName)) {
                    Class<?> importIface = importRef.getServiceInterface();
                    
                    if (!importIface.isAssignableFrom(exportIface)) {
                       importsTypesAreCorrect = false;
                       log.error("Imported bean {} from location {} must implement same interface that appropriate" +
                                 "exported bean {} or subinterface but no superclass or superinterface",
                               new Object[]{exportName, importRef.getLocation(), exportName});
                    }
                }
            }
        }
        
        return importsTypesAreCorrect;
    }


    protected BeanReferenceInfo extractImportReference(BeanDefinition beanDefinition) {
        if (!beanDefinition.hasAttribute(IMPORT_BEAN_DEF_ATTR_NAME)) {
            throw new IllegalArgumentException("BeanDefinition does not contain attribute: " + IMPORT_BEAN_DEF_ATTR_NAME);
        }
        return (BeanReferenceInfo) beanDefinition.getAttribute(IMPORT_BEAN_DEF_ATTR_NAME);
    }

    protected BeanReferenceInfo extractExportReference(BeanDefinition beanDefinition) {
        if (!beanDefinition.hasAttribute(EXPORT_BEAN_DEF_ATTR_NAME)) {
            throw new IllegalArgumentException("BeanDefinition does not contain attribute: " + EXPORT_BEAN_DEF_ATTR_NAME);
        }
        return (BeanReferenceInfo) beanDefinition.getAttribute(EXPORT_BEAN_DEF_ATTR_NAME);
    }

    //TODO should be private or protected
    public void putInImports(BeanReferenceInfo importRefInfo) {
        String serviceName = importRefInfo.getServiceName();

        if (imports.containsKey(serviceName)) {
            imports.get(serviceName).add(importRefInfo);
        } else {
            List<BeanReferenceInfo> refInfoList = new ArrayList<>();
            refInfoList.add(importRefInfo);
            imports.put(serviceName, refInfoList);
        }
    }

    //TODO should be private or protected
    public void putInExports(BeanReferenceInfo exportRefInfo) {
        String serviceName = exportRefInfo.getServiceName();

        if (exports.containsKey(serviceName)) {
            throw new BeanCreationException(String.format(
                    "Double export was defined: %s in context %s. Previous export was in context %s",
                    serviceName, exportRefInfo.getLocation(), exports.get(serviceName).getLocation()));
        } else {
            exports.put(serviceName, exportRefInfo);
        }
    }
}
