package no.uis.service.ws.studinfosolr;

import static org.hamcrest.CoreMatchers.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class EmnerSolrTest extends AbstractSolrTestCase {

  private BeanFactory bf;
  private Map<String, SolrServer> solrServerMap = new HashMap<String, SolrServer>();

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    String[] langs = {"b", "n", "e"};
    for (String lang : langs) {
      String solrServerUrl = System.getProperty("solr.server.url."+lang);
      SolrServer solrServer;
      if (solrServerUrl != null) {
        solrServer = new HttpSolrServer(solrServerUrl);
      } else {
        solrServer = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
      }
      solrServerMap.put(lang.toUpperCase(), solrServer);
    }
    
    StaticApplicationContext bfParent = new StaticApplicationContext();
    bfParent.getDefaultListableBeanFactory().registerSingleton("solrServerMap", solrServerMap);
    bfParent.refresh();
    bf = new ClassPathXmlApplicationContext(new String[] {"studinfo-solr.xml"}, bfParent);
  }
  
  @Test
  public void emneBExists() throws Exception {
    testEmne("B");
  }
  
  @Test
  public void emneEExists() throws Exception {
    testEmne("E");
  }
  
  @Test
  public void emneNExists() throws Exception {
    testEmne("N");
  }
  
  private void testEmne(String lang) throws Exception {
    StudinfoSolrService service = bf.getBean("studinfoSolrService", StudinfoSolrService.class);
    service.updateSolrEmne(2012, "HOST", lang);
    
    solrServerMap.get(lang).commit();
    SolrParams params = new SolrQuery("cat:STUDINFO AND cat:EMNE");
    QueryResponse response = solrServerMap.get(lang).query(params);
    int status = response.getStatus();
    assertThat(status, is(equalTo(0)));
    assertThat(response.getResults().getNumFound(), is(not(equalTo(Long.valueOf(0L)))));
  }
  
  
  @Override
  public String getSchemaFile() {
    return "schema-studinfo.xml";
  }

  @Override
  public String getSolrConfigFile() {
    return "solrconfig-studinfo.xml";
  }
}
