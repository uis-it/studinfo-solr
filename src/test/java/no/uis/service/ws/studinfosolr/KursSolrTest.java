package no.uis.service.ws.studinfosolr;

import static org.hamcrest.CoreMatchers.*;

import java.util.HashMap;
import java.util.Map;

import no.uis.service.studinfo.data.FsSemester;
import no.uis.service.studinfo.data.FsStudieinfo;
import no.uis.service.ws.studinfosolr.impl.SolrUpdaterImpl;

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

public class KursSolrTest extends AbstractSolrTestCase {

  private FsStudieinfo fsInfo;
  private SolrServer solrServer;
  
  private BeanFactory bf;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    String solrServerUrl = System.getProperty("solr.server.url");
    if (solrServerUrl != null) {
      solrServer = new HttpSolrServer(solrServerUrl);
    } else {
      solrServer = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
    }
    bf = new ClassPathXmlApplicationContext(new String[] {
      "studinfo-solr.xml",
      "fsMock.xml"
      ,"cpmock.xml"});
    
    fsInfo = bf.getBean("kursList", FsStudieinfo.class);
  }
  
  @Test
  public void kursExists() throws Exception {
    
    SolrUpdaterImpl updater = bf.getBean("solrUpdater", SolrUpdaterImpl.class);

    Map<String, SolrServer> solrServers = new HashMap<String, SolrServer>();
    
    solrServers.put("KURS_BOKMÅL", solrServer);
    updater.setSolrServers(solrServers);
    updater.pushStudieInfo(fsInfo, 2012, FsSemester.HOST, "BOKMÅL");
    
    SolrParams params = new SolrQuery("cat:STUDINFO AND cat:KURS");
    QueryResponse response = solrServer.query(params);
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
