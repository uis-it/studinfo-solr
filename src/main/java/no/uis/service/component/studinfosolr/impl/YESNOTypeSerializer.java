package no.uis.service.component.studinfosolr.impl;

import java.io.IOException;

import javax.xml.datatype.XMLGregorianCalendar;

import no.uis.service.studinfo.data.YESNOType;

import org.apache.commons.lang.NotImplementedException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class YESNOTypeSerializer extends TypeAdapter<YESNOType> {

  public static String convertToSolrString(XMLGregorianCalendar xcal) {
    if (xcal == null) {
      return null;
    }
    return xcal.toXMLFormat();
  }

  @Override
  public void write(JsonWriter out, YESNOType value) throws IOException {
    if (value != null) {
      out.value(value.equals(YESNOType.J) ? true : false);
    } else {
      out.nullValue();
    }
  }

  @Override
  public YESNOType read(JsonReader in) throws IOException {
    throw new NotImplementedException();
  }
}
