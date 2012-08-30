package no.uis.service.studinfo.data;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

@SuppressWarnings("serial")
public class VurdkombinasjonFactory extends Vurdkombinasjon implements FactoryBean<Vurdkombinasjon> {

  @Override
  public Vurdkombinasjon getObject() throws Exception {
    return this;
  }

  @Override
  public Class<?> getObjectType() {
    return Vurdkombinasjon.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
  
  public void setVurdkombinasjon(List<? extends Vurdkombinasjon> list) {
    super.unsetVurdkombinasjon();
    super.getVurdkombinasjon().addAll(list);
  }
}
