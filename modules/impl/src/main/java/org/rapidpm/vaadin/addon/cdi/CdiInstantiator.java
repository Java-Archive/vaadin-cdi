/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.rapidpm.vaadin.addon.cdi;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.rapidpm.dependencies.core.logger.HasLogger;
import org.rapidpm.vaadin.addon.cdi.annotation.VaadinServiceEnabled;
import org.rapidpm.vaadin.addon.cdi.annotation.VaadinServiceScoped;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.rapidpm.vaadin.addon.cdi.BeanLookup.SERVICE;


/**
 * Default CDI instantiator.
 * <p>
 * Can be overridden by a @{@link VaadinServiceEnabled}
 * CDI Alternative/Specializes, or can be customized with a Decorator.
 *
 * @see Instantiator
 */
@VaadinServiceScoped
@VaadinServiceEnabled
public class CdiInstantiator implements Instantiator, HasLogger {

  private static final String CANNOT_USE_CDI_BEANS_FOR_I18N
      = "Cannot use CDI beans for I18N, falling back to the default behavior.";
  private static final String FALLING_BACK_TO_DEFAULT_INSTANTIATION
      = "Falling back to default instantiation.";

  private AtomicBoolean       i18NLoggingEnabled = new AtomicBoolean(true);
  private DefaultInstantiator delegate;

  @Inject private BeanManager beanManager;

  @Override
  public boolean init(VaadinService service) {
    delegate = new DefaultInstantiator(service);
    return delegate.init(service)
           && service instanceof CdiVaadinServletService;
  }

  @Override
  public <T> T getOrCreate(Class<T> type) {
    return new BeanLookup<>(beanManager, type)
        .setUnsatisfiedHandler((BeanLookup.UnsatisfiedHandler) () ->
            logger().info(("'" + type.getName() + "' is not a CDI bean. "
                           + FALLING_BACK_TO_DEFAULT_INSTANTIATION)))
        .setAmbiguousHandler(e ->
                                 logger().info("Multiple CDI beans found. "
                                               + FALLING_BACK_TO_DEFAULT_INSTANTIATION + " - " + e.getMessage())

        )
        .lookupOrElseGet(() -> {
          final T              instance          = delegate.getOrCreate(type);
          BeanManager          beanManager       = CDI.current().getBeanManager();
          CreationalContext<T> creationalContext = beanManager.createCreationalContext(null);

          AnnotatedType<T>   annotatedType   = beanManager.createAnnotatedType((Class<T>) instance.getClass());
          InjectionTarget<T> injectionTarget = beanManager.createInjectionTarget(annotatedType);
          injectionTarget.inject(instance, creationalContext);
          //TODO POSTCONSTRUCT - check if this is needed !!
          injectionTarget.postConstruct(instance);
          //TODO POSTCONSTRUCT - check if this is needed !!
          return instance;
        });
  }

  @Override
  public I18NProvider getI18NProvider() {
    final BeanLookup<I18NProvider> lookup =
        new BeanLookup<>(beanManager, I18NProvider.class, SERVICE);
    if (i18NLoggingEnabled.compareAndSet(true, false)) {
      lookup
          .setUnsatisfiedHandler(() ->
                                     logger().info("Can't find any bean implementing '" + I18NProvider.class.getSimpleName() + "'. "
                                                   + CANNOT_USE_CDI_BEANS_FOR_I18N))
          .setAmbiguousHandler(e ->
                                   logger().warning("Found more beans for I18N. "
                                                    + CANNOT_USE_CDI_BEANS_FOR_I18N, e));
    } else {
      lookup.setAmbiguousHandler(e -> { });
    }
    return lookup.lookupOrElseGet(delegate::getI18NProvider);
  }

  @Override
  public Stream<VaadinServiceInitListener> getServiceInitListeners() {
    return Stream.concat(
        delegate.getServiceInitListeners(),
        Stream.of(beanManager::fireEvent)
    );
  }

}
