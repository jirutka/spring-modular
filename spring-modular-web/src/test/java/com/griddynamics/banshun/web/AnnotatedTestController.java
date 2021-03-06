/*
 * Copyright 2012 Grid Dynamics Consulting Services, Inc.
 *      http://www.griddynamics.com
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
package com.griddynamics.banshun.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/*")
public class AnnotatedTestController {
    @RequestMapping("/annotation-test.html")
    public ModelAndView annotationTest()  {
        String message = "Hello Spring MVC";

        ModelAndView modelAndView = new ModelAndView("testView");
        modelAndView.addObject("message", message);

        return modelAndView;
    }

    @RequestMapping("/another-annotation-test.html")
    public ModelAndView anotherAnnotationTest()  {
        String message = "Hello Another Spring MVC";

        ModelAndView modelAndView = new ModelAndView("testView");
        modelAndView.addObject("message", message);

        return modelAndView;
    }
}
