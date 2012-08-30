package no.uis.service.studinfo.data;

import org.springframework.beans.factory.FactoryBean;

public class VurdordningFactory extends Vurdordning implements FactoryBean<Vurdordning> {

  private static final long serialVersionUID = 4650074925529428083L;
  static private org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(VurdordningFactory.class);

  @Override
  public Vurdordning getObject() throws Exception {
    return this;
  }

  @Override
  public Class<?> getObjectType() {
    return Vurdordning.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
  
  
}
