module org.rapidpm.vaadin.cdi_addon {
  requires org.apache.meecrowave.specs;
  requires rapidpm.dependencies.core.logger;
  requires flow.server;
  requires java.naming;

  exports org.rapidpm.vaadin.addon.cdi;
  exports org.rapidpm.vaadin.addon.cdi.annotation;
  exports org.rapidpm.vaadin.addon.cdi.context;

}