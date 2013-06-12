/*
 Copyright 2012-2013 University of Stavanger, Norway

 Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

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
