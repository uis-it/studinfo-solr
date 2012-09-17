package no.uis.service.component.studinfosolr.impl;

import java.io.IOException;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.NotImplementedException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class CalendarSerializer extends TypeAdapter<XMLGregorianCalendar> {

  public static String convertToSolrString(XMLGregorianCalendar xcal) {
    if (xcal == null) {
      return null;
    }
    return xcal.toXMLFormat();
  }

  @Override
  public void write(JsonWriter out, XMLGregorianCalendar value) throws IOException {
    out.value(convertToSolrString(value));
  }

  @Override
  public XMLGregorianCalendar read(JsonReader in) throws IOException {
    throw new NotImplementedException();
  }
}
