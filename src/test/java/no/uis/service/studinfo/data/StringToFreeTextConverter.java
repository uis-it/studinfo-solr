package no.uis.service.studinfo.data;

import org.springframework.core.convert.converter.Converter;

public class StringToFreeTextConverter implements Converter<String, FreeText>{

  @Override
  public FreeText convert(String source) {
    FreeText ft = new FreeText();
    ft.getContent().add(source);
    return ft;
  }

}
