/**
 * Copyright © 2013 Sven Ruppert (sven.ruppert@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rapidpm.vaadin.addon.cdi;

import org.rapidpm.dependencies.core.logger.HasLogger;
import org.rapidpm.vaadin.addon.cdi.annotation.NormalUIScoped;
import org.rapidpm.vaadin.addon.cdi.context.*;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import java.lang.annotation.Annotation;

/**
 * CDI Extension needed to register Vaadin scopes to the runtime.
 */
public class VaadinExtension implements Extension, HasLogger {

    private VaadinServiceScopedContext serviceScopedContext;
    private UIScopedContext uiScopedContext;

    public void initializeContexts(@Observes AfterDeploymentValidation adv, BeanManager beanManager) {
        serviceScopedContext.init(beanManager);
        uiScopedContext.init(beanManager);
    }

    void afterBeanDiscovery(
            @Observes final AfterBeanDiscovery afterBeanDiscovery,
            final BeanManager beanManager) {
        serviceScopedContext = new VaadinServiceScopedContext(beanManager);
        uiScopedContext = new UIScopedContext(beanManager);
        addContext(afterBeanDiscovery, serviceScopedContext, null);
        addContext(afterBeanDiscovery,
                new VaadinSessionScopedContext(beanManager), null);
        addContext(afterBeanDiscovery, uiScopedContext, NormalUIScoped.class);
    }

    private void addContext(AfterBeanDiscovery afterBeanDiscovery,
                            AbstractContext context,
                            Class<? extends Annotation> additionalScope) {
        afterBeanDiscovery.addContext(
                new ContextWrapper(context, context.getScope()));
        if (additionalScope != null) {
            afterBeanDiscovery.addContext(
                    new ContextWrapper(context, additionalScope));
        }
        logger().info("{} registered for Vaadin CDI " + context.getClass().getSimpleName());
    }
}
