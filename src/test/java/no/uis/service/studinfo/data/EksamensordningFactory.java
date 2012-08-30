package no.uis.service.studinfo.data;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

@SuppressWarnings("serial")
public class EksamensordningFactory extends Eksamensordning implements FactoryBean<Eksamensordning> {

  @Override
  public Eksamensordning getObject() throws Exception {
    return this;
  }

  @Override
  public Class<?> getObjectType() {
    return Eksamensordning.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
  
  public void setEksamensdel(List<Eksamensdel> list) {
    super.unsetEksamensdel();
    super.getEksamensdel().addAll(list);
  }
}
