package no.uis.service.component.studinfosolr;

import static org.hamcrest.CoreMatchers.*;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

import no.uis.service.component.studinfopdf.Messages;
import no.uis.service.component.studinfosolr.impl.SolrUpdaterImpl;
import no.uis.service.studinfo.data.FsSemester;
import no.uis.service.studinfo.data.FsStudieinfo;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class EmnerSolrTest extends AbstractSolrTestCase {

  private FsStudieinfo fsInfo;
  private SolrServer solrServer;
  
  private BeanFactory bf;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    String solrServerUrl = System.getProperty("solr.server.url");
    if (solrServerUrl != null) {
      solrServer = new HttpSolrServer("http://search-test01.uis.no/solr/studinfo-nb/");
    } else { 
      solrServer = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
    }
    bf = new ClassPathXmlApplicationContext(new String[] {
      "studinfo-solr.xml",
      "fsMock.xml"
      ,"cpmock.xml"});
    
    fsInfo = bf.getBean("emneList", FsStudieinfo.class);
  }
  
  @Test
  public void emneExists() throws Exception {
    
    SolrUpdaterImpl updater = bf.getBean("solrUpdater", SolrUpdaterImpl.class);

    Map<String, SolrServer> solrServers = new HashMap<String, SolrServer>();
    
    Messages.init("BOKMÅL", null);
    
    solrServers.put("EMNE_BOKMÅL", solrServer);
    updater.setSolrServers(solrServers);
    updater.pushStudieInfo(fsInfo, 2012, FsSemester.HOST, "BOKMÅL");
    
    SolrParams params = new SolrQuery("cat:studinfo AND cat:EMNE");
    QueryResponse response = solrServer.query(params);
    printResponse(response);
    int status = response.getStatus();
    assertThat(status, is(equalTo(0)));
    assertThat(response.getResults().getNumFound(), is(not(equalTo(Long.valueOf(0L)))));
  }

  private void printResponse(QueryResponse response) {
    ListIterator<SolrDocument> docIter = response.getResults().listIterator();
    while (docIter.hasNext()) {
      
      SolrDocument doc = docIter.next();
      for (Map.Entry<String, Object> entry : doc) {
        System.out.println(entry);
      }
    }
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
