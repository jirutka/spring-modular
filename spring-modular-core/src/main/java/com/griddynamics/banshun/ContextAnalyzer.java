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
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ContextAnalyzer.class);

    private Map<String, BeanReferenceInfo> exports = new HashMap<>();
    private Map<String, List<BeanReferenceInfo>> imports = new HashMap<>();


    public Map<String, List<BeanReferenceInfo>> getImports() {
        return imports;
    }

    public void addImport(BeanDefinition beanDefinition, String location) throws ClassNotFoundException {
        BeanReferenceInfo importReferenceInfo = parseLookupOrExportRefArg(beanDefinition, location);
        
        putInImports(importReferenceInfo);
    }

    public Map<String, BeanReferenceInfo> getExports() {
        return exports;
    }

    public void addExport(BeanDefinition beanDefinition, String location) throws ClassNotFoundException, BeanCreationException {
        BeanReferenceInfo exportReferenceInfo = getExportReference(beanDefinition, location);

        putInExports(exportReferenceInfo);
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


    protected BeanReferenceInfo parseLookupOrExportRefArg(BeanDefinition beanDefinition, String location) throws ClassNotFoundException {
        String serviceName = extractServiceName(beanDefinition);
        Class<?> serviceIface = extractServiceInterface(beanDefinition);

        return new BeanReferenceInfo(serviceName, serviceIface, location);
    }

    protected String extractServiceName(BeanDefinition beanDefinition) {
        String serviceName = null;

        ConstructorArgumentValues.ValueHolder valueHolder = beanDefinition
                .getConstructorArgumentValues().getIndexedArgumentValue(0, null);
        Object beanNameValueHolder = valueHolder.getValue();

        if (beanNameValueHolder instanceof RuntimeBeanNameReference) {
            serviceName = ((RuntimeBeanNameReference)valueHolder.getValue()).getBeanName();
        } else if (beanNameValueHolder instanceof TypedStringValue) {
            serviceName = ((TypedStringValue)valueHolder.getValue()).getValue();
        } else if (beanNameValueHolder instanceof String) {
            serviceName = (String)beanNameValueHolder;
        }

        return serviceName;
    }

    protected BeanReferenceInfo getExportReference(BeanDefinition beanDefinition, String location) throws ClassNotFoundException {

        ConstructorArgumentValues argumentValues = beanDefinition.getConstructorArgumentValues();

        ValueHolder valueHolder = argumentValues.getArgumentValue(0, BeanDefinitionHolder.class);
        BeanDefinition exportRefBeanDefinition;
        if (valueHolder != null) {
            BeanDefinitionHolder holder = (BeanDefinitionHolder) valueHolder.getValue();
            exportRefBeanDefinition = holder.getBeanDefinition();
        } else {
            exportRefBeanDefinition = (BeanDefinition) (argumentValues.getGenericArgumentValues().get(0)).getValue();
        }

        return parseLookupOrExportRefArg(exportRefBeanDefinition, location);
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


    private Class<?> extractServiceInterface(BeanDefinition beanDefinition) throws ClassNotFoundException {
        Class<?> serviceIface = null;

        ConstructorArgumentValues.ValueHolder valueHolder = beanDefinition
                .getConstructorArgumentValues().getIndexedArgumentValue(1, null);

        Object serviceIfaceName = valueHolder.getValue();

        if (serviceIfaceName instanceof TypedStringValue) {
            serviceIface = Class.forName(((TypedStringValue)serviceIfaceName).getValue());
        } else if (serviceIfaceName instanceof Class) {
            serviceIface = (Class<?>) serviceIfaceName;
        }

        return serviceIface;
    }
}
