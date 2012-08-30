package no.uis.service.studinfo.data;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

@SuppressWarnings("serial")
public class EmneFactory extends Emne implements FactoryBean<Emne> {

  @Override
  public Emne getObject() throws Exception {
    return this;
  }

  @Override
  public Class<?> getObjectType() {
    return Emne.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
  
  public void setUndsemester(List<? extends Undsemester> list) {
    super.unsetUndsemester();
    super.getUndsemester().addAll(list);
  }
  
  public void setSted(List<? extends Sted> list) {
    super.unsetSted();
    super.getSted().addAll(list);
  }
  
  public void setEksamensordning(List<? extends Eksamensordning> list) {
    super.unsetEksamensordning();
    super.getEksamensordning().addAll(list);
  }
  
  public void setVurdordning(List<? extends Vurdordning> list) {
    super.unsetVurdordning();
    super.getVurdordning().addAll(list);
  }

  public void setObligUndaktTilleggsinfo(String value) {
    super.setObligUndaktTilleggsinfo(FreeTextFactory.valueOf(value));
  }

  public void setArbeidsformer(String value) {
    super.setArbeidsformer(FreeTextFactory.valueOf(value));
  }
}
