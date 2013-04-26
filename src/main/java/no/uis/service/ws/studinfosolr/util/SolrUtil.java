package no.uis.service.ws.studinfosolr.util;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;
import org.apache.solr.common.util.DateUtil;

public class SolrUtil {

  private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");
  
  private static final Logger LOG = Logger.getLogger(SolrUtil.class);

  private SolrUtil() {
  }

  public static String xmlCalToSolrDateString(XMLGregorianCalendar value) {
    return solrDate(value.toGregorianCalendar(TIME_ZONE_UTC, Locale.ENGLISH, null));
  }
  
  public static String solrDateNow() {
    return solrDate(getDefaultCalendar());    
  }

  public static Calendar getDefaultCalendar() {
    return Calendar.getInstance(TIME_ZONE_UTC, Locale.ENGLISH);
  }
  
  private static String solrDate(Calendar cal) {
    StringBuilder out = new StringBuilder();
    try {
      DateUtil.formatDate(cal.getTime(), cal, out);
    } catch(IOException e) {
      LOG.warn("xml cal: " + cal.getTime());
    }
    
    return out.toString();
  }

}
