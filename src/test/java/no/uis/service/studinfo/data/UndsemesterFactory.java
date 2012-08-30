package no.uis.service.studinfo.data;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

@SuppressWarnings("serial")
public class UndsemesterFactory extends Undsemester implements FactoryBean<Undsemester> {

  @Override
  public Undsemester getObject() throws Exception {
    return this;
  }

  @Override
  public Class<?> getObjectType() {
    return Undsemester.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
  
  public void setSemester(List<Semester> list) {
    super.unsetSemester();
    super.getSemester().addAll(list);
  }
}
