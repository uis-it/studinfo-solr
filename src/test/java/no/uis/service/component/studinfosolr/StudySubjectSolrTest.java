package no.uis.service.component.studinfosolr;

import static org.hamcrest.CoreMatchers.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.uis.service.component.studinfosolr.impl.SolrUpdaterImpl;
import no.uis.service.studinfo.data.Emne;
import no.uis.service.studinfo.data.FsSemester;
import no.uis.service.studinfo.data.FsStudieinfo;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class StudySubjectSolrTest extends AbstractSolrTestCase {

  private FsStudieinfo fsInfo;
  private SolrServer solrServer;


  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
//    solrServer = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
    solrServer = new HttpSolrServer("http://search-test01.uis.no/solr/studinfo-nb/");
    ApplicationContext bf = new ClassPathXmlApplicationContext(new String[] {
      "fsMock.xml"
      ,"cpmock.xml"});
    
    fsInfo = bf.getBean("emneList", FsStudieinfo.class);
  }
  
  @Test
  public void emneExist() throws Exception {
    
    SolrUpdaterImpl updater = new SolrUpdaterImpl();
    Map<String, SolrServer> solrServers = new HashMap<String, SolrServer>();
    
    solrServers.put("EMNE_BOKMÅL", solrServer);
    updater.setSolrServers(solrServers);
    updater.pushStudieInfo(fsInfo, 2012, FsSemester.HOST, "BOKMÅL");
    
    SolrParams params = new SolrQuery("cat:EMNE AND cat:studinfo");
    QueryResponse response = solrServer.query(params);
    int status = response.getStatus();
    assertThat(status, is(equalTo(0)));
    assertThat(response.getResults().getNumFound(), is(equalTo(1L)));
  }
  
  @Override
  public String getSchemaFile() {
    return "schema-emne.xml";
  }

  @Override
  public String getSolrConfigFile() {
    return "solrconfig-emne.xml";
  }
}
