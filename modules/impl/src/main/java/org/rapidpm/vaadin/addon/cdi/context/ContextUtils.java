package org.rapidpm.vaadin.addon.cdi.context;

import org.rapidpm.vaadin.addon.cdi.BeanManagerProvider;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Annotation;

public class ContextUtils {

  private ContextUtils() {
    // prevent instantiation
  }

  /**
   * Checks if the context for the given scope annotation is active.
   *
   * @param scopeAnnotationClass The scope annotation (e.g. @RequestScoped.class)
   * @return If the context is active.
   */
  public static boolean isContextActive(Class<? extends Annotation> scopeAnnotationClass) {
    return isContextActive(scopeAnnotationClass, BeanManagerProvider.getInstance().getBeanManager());
  }

  /**
   * Checks if the context for the given scope annotation is active.
   *
   * @param scopeAnnotationClass The scope annotation (e.g. @RequestScoped.class)
   * @param beanManager          The {@link BeanManager}
   * @return If the context is active.
   */
  public static boolean isContextActive(Class<? extends Annotation> scopeAnnotationClass, BeanManager beanManager) {
    try {
      if (beanManager.getContext(scopeAnnotationClass) == null
          || !beanManager.getContext(scopeAnnotationClass).isActive()) {
        return false;
      }
    } catch (ContextNotActiveException e) {
      return false;
    }

    return true;
  }
}
