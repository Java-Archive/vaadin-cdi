package org.rapidpm.vaadin.addon.cdi.m01;

import org.rapidpm.vaadin.addon.cdi.annotation.VaadinSessionScoped;

@VaadinSessionScoped
public class VaadinSessionClickCounter implements ClickCounter {

  private Integer countedClicks = 0;

  @Override
  public Integer count() {
    return ++countedClicks;
  }
}
