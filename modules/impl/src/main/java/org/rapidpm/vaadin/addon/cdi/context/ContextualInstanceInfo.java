package org.rapidpm.vaadin.addon.cdi.context;

import javax.enterprise.context.spi.CreationalContext;
import java.io.Serializable;

public class ContextualInstanceInfo<T> implements Serializable {
  private static final long serialVersionUID = 6384932199958645324L;

  /**
   * The actual Contextual Instance in the context
   */
  private T contextualInstance;

  /**
   * We need to store the CreationalContext as we need it for
   * properly destroying the contextual instance via
   * {@link javax.enterprise.context.spi.Contextual#destroy(Object, CreationalContext)}
   */
  private CreationalContext<T> creationalContext;

  /**
   * @return the CreationalContext of the bean
   */
  public CreationalContext<T> getCreationalContext() {
    return creationalContext;
  }

  /**
   * @param creationalContext the CreationalContext of the bean
   */
  public void setCreationalContext(CreationalContext<T> creationalContext) {
    this.creationalContext = creationalContext;
  }

  /**
   * @return the contextual instance itself
   */
  public T getContextualInstance() {
    return contextualInstance;
  }

  /**
   * @param contextualInstance the contextual instance itself
   */
  public void setContextualInstance(T contextualInstance) {
    this.contextualInstance = contextualInstance;
  }

}