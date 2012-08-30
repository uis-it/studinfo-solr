package no.uis.service.component.studinfosolr;


import static org.junit.Assert.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DateTimeSolrTest {

  private static final String VAAR = String.valueOf(new char[] {'V', '\u00c5', 'R'});
  private static SolrServer solrServer;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
      new String[] {
       "cpmock.xml"
       ,"solrMock.xml"
       }, 
     true);

    solrServer = (SolrServer)context.getBean("solrServer");
  }
  
  @Test
  public void testDateLowerLimit() throws Exception {
    
    SolrQuery params = new SolrQuery("publish_from_dt:[* TO NOW/DAY] AND publish_to_dt:[NOW/DAY TO *]");
    QueryResponse response = solrServer.query(params);
    assertFalse(response.getResults().isEmpty());
  }
  
  @Test
  public void testDateUpperLimit() throws Exception {

    SolrQuery params = new SolrQuery("publish_from_dt:[* TO NOW/DAY] AND publish_to_dt:[NOW/DAY TO *]");
    QueryResponse response = solrServer.query(params);
    assertFalse(response.getResults().isEmpty());
  }
}
