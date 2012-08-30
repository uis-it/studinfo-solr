package no.uis.service.studinfo.data;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

@SuppressWarnings("serial")
public class FreeTextFactory extends FreeText implements FactoryBean<FreeText> {

  @Override
  public FreeText getObject() throws Exception {
    return this;
  }

  @Override
  public Class<?> getObjectType() {
    return FreeText.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
  
  public void setContent(List<?> list) {
    super.unsetContent();
    super.getContent().addAll(list);
  }

  public static FreeText valueOf(String value) {
    FreeTextFactory ftf = new FreeTextFactory();
    ftf.getContent().add(value);
    return ftf;
  }
}
