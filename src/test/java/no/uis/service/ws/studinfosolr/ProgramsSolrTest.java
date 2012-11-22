package no.uis.service.ws.studinfosolr;

import static org.hamcrest.CoreMatchers.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import no.uis.service.studinfo.data.FsSemester;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class ProgramsSolrTest extends AbstractSolrTestCase {

  private BeanFactory bf;
  private Map<String, SolrServer> solrServerMap = new HashMap<String, SolrServer>();
  private Properties testConfig;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    File configFile = new File(System.getProperty("user.home"), "ws-studinfo-solr.xml");
    Assume.assumeTrue(configFile.canRead());
    Properties configProps = new Properties();
    configProps.loadFromXML(new FileInputStream(configFile));
    String[] langs = configProps.getProperty("languages", "B").split("\\s+");
    for (String lang : langs) {
      String solrServerUrl = configProps.getProperty(String.format("solr.server.%s.url", lang));
      SolrServer solrServer;
      if (solrServerUrl != null) {
        if (solrServerUrl.equalsIgnoreCase("embedded")) {
          solrServer = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
        } else {
          solrServer = new HttpSolrServer(solrServerUrl);
        }
        solrServerMap.put(lang, solrServer);
      }
    }
    this.testConfig = configProps;
    
    StaticApplicationContext bfParent = new StaticApplicationContext();
    bfParent.getDefaultListableBeanFactory().registerSingleton("solrServerMap", solrServerMap);
    bfParent.refresh();
    bf = new ClassPathXmlApplicationContext(new String[] {"studinfo-solr.xml"}, bfParent);
  }
  
  @Test
  public void programBExists() throws Exception {
    testProgram("B");
  }
  
  @Test
  public void programEExists() throws Exception {
    testProgram("E");
  }
  
  @Test
  public void programNExists() throws Exception {
    testProgram("N");
  }
  
  private void testProgram(String lang) throws Exception {
    Assume.assumeNotNull(solrServerMap.get(lang));

    int year = Integer.parseInt(testConfig.getProperty("year", "2013"));
    FsSemester semester = FsSemester.stringToUisSemester(testConfig.getProperty("semester"));
    
    StudinfoSolrService service = bf.getBean("studinfoSolrService", StudinfoSolrService.class);
    service.updateSolrStudieprogram(year, semester.toString(), lang);
    
    solrServerMap.get(lang).commit();
    SolrParams params = new SolrQuery("cat:STUDINFO AND cat:STUDIEPROGRAM");
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
