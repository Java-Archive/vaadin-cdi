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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.inject.Provider;
import java.io.*;

public class DependentProvider<T> implements Provider<T>, Serializable {
  private static final long serialVersionUID = 23423413412001L;

  private           T                    instance;
  private           CreationalContext<T> creationalContext;
  private transient Bean<T>              bean;

  DependentProvider(Bean<T> bean, CreationalContext<T> creationalContext, T instance) {
    this.bean = bean;
    this.creationalContext = creationalContext;
    this.instance = instance;
  }

  @Override
  public T get() {
    return instance;
  }

  /**
   * This method will properly destroy the &#064;Dependent scoped instance.
   * It will have no effect if the bean is NormalScoped as those have their
   * own lifecycle which we must not disrupt.
   */
  public void destroy() {
    if (!BeanManagerProvider.getInstance().getBeanManager().isNormalScope(bean.getScope())) {
      bean.destroy(instance, creationalContext);
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    if (!(bean instanceof PassivationCapable)) {
      throw new NotSerializableException("Bean is not PassivationCapable: " + bean.toString());
    }
    String passivationId = ((PassivationCapable) bean).getId();
    if (passivationId == null) {
      throw new NotSerializableException(bean.toString());
    }

    out.writeLong(serialVersionUID);
    out.writeObject(passivationId);
    out.writeObject(instance);
    out.writeObject(creationalContext);
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    long oldSerialId = in.readLong();
    if (oldSerialId != serialVersionUID) {
      throw new NotSerializableException(getClass().getName() + " serialVersion does not match");
    }
    String passivationId = (String) in.readObject();
    bean = (Bean<T>) BeanManagerProvider.getInstance().getBeanManager().getPassivationCapableBean(passivationId);
    instance = (T) in.readObject();
    creationalContext = (CreationalContext<T>) in.readObject();
  }

}