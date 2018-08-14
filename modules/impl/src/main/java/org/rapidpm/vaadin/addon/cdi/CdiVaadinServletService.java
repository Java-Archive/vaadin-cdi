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

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.*;
import org.rapidpm.dependencies.core.logger.HasLogger;
import org.rapidpm.vaadin.addon.cdi.annotation.VaadinServiceEnabled;
import org.rapidpm.vaadin.addon.cdi.context.VaadinSessionScopedContext;

import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.spi.BeanManager;
import java.util.Optional;

import static org.rapidpm.vaadin.addon.cdi.BeanLookup.SERVICE;

/**
 * Servlet service implementation for Vaadin CDI.
 * <p>
 * This class creates and initializes a @{@link VaadinServiceEnabled}
 * {@link Instantiator}.
 *
 * @see CdiVaadinServlet
 */
public class CdiVaadinServletService extends VaadinServletService implements HasLogger {

  private final BeanManager beanManager;

  public CdiVaadinServletService(CdiVaadinServlet servlet,
                                 DeploymentConfiguration configuration,
                                 BeanManager beanManager) {
    super(servlet, configuration);
    this.beanManager = beanManager;
  }


  /**
   * Static listener class,
   * to avoid registering the whole service instance.
   */
  private static class Listener implements SessionDestroyListener, HasLogger {

    @Override
    public void sessionDestroy(SessionDestroyEvent sessionDestroyEvent) {
      if (VaadinSessionScopedContext.guessContextIsUndeployed()) {
        // Happens on tomcat when it expires sessions upon undeploy.
        // beanManager.getPassivationCapableBean returns null for passivation id,
        // so we would get an NPE from AbstractContext.destroyAllActive
        logger().warning("VaadinSessionScoped context does not exist. " +
                         "Maybe application is undeployed." +
                         " Can't destroy VaadinSessionScopedContext.");
        return;
      }
      logger().info("VaadinSessionScopedContext destroy");
      VaadinSessionScopedContext.destroy(sessionDestroyEvent.getSession());
    }
  }

  @Override
  public void init() throws ServiceException {
    addSessionDestroyListener(new Listener());
    addServiceDestroyListener(this::fireCdiDestroyEvent);
    super.init();
  }

  @Override
  public CdiVaadinServlet getServlet() {
    return (CdiVaadinServlet) super.getServlet();
  }

  @Override
  protected Optional<Instantiator> loadInstantiators()
      throws ServiceException {
    Optional<Instantiator> instantiatorOptional = lookup(Instantiator.class);
    if (instantiatorOptional.isPresent()) {
      Instantiator instantiator = instantiatorOptional.get();
      if (!instantiator.init(this)) {
//        Class unproxiedClass = ProxyUtils.getUnproxiedClass(instantiator.getClass());
        throw new ServiceException(
            "Cannot init VaadinService because "
            + instantiator.getClass().getName() + " CDI bean init()"
            + " returned false.");
      }
    } else {
      throw new ServiceException(
          "Cannot init VaadinService "
          + "because no CDI instantiator bean found."
      );
    }
    return instantiatorOptional;
  }

  private <T> Optional<T> lookup(Class<T> type) throws ServiceException {
    try {
      T instance = new BeanLookup<>(beanManager, type, SERVICE).lookup();
      return Optional.ofNullable(instance);
    } catch (AmbiguousResolutionException e) {
      throw new ServiceException(
          "There are multiple eligible CDI " + type.getSimpleName()
          + " beans.", e);
    }
  }

  private void fireCdiDestroyEvent(ServiceDestroyEvent event) {
    try {
      beanManager.fireEvent(event);
    } catch (Exception e) {
      // During application shutdown on TomEE 7,
      // beans are lost at this point.
      // Does not throw an exception, but catch anything just to be sure.
      logger().warning("Error at destroy event distribution with CDI." + e);
    }
  }
}
