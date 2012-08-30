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
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class StudySubjectSolrTest extends AbstractSolrTestCase {

  private List<Emne> emneList;
  private SolrServer solrServer;


  @SuppressWarnings("unchecked")
  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    solrServer = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
    ApplicationContext bf = new ClassPathXmlApplicationContext(new String[] {
      "emnerMock.xml"
      ,"cpmock.xml"});
    
    emneList = bf.getBean("emneList", List.class);
  }
  
  @Test
  public void emneExist() throws Exception {
    
    SolrUpdaterImpl updater = new SolrUpdaterImpl();
    Map<String, SolrServer> solrServers = new HashMap<String, SolrServer>();
    
    solrServers.put("EMNE_BOKMÅL", solrServer);
    updater.setSolrServers(solrServers);
    FsStudieinfo info = new FsStudieinfo();
    info.getEmne().addAll(emneList);
    updater.pushStudieInfo(info, 2012, FsSemester.HOST, "BOKMÅL");
    
    SolrParams params = new SolrQuery("cat:emne AND cat:studinfo");
    QueryResponse response = solrServer.query(params);
    int status = response.getStatus();
    assertThat(status, is(equalTo(0)));
    assertThat(response.getResults().getNumFound(), is(equalTo(1L)));
  }
  
  @Override
  public String getSchemaFile() {
    return "schema.xml";
  }

  @Override
  public String getSolrConfigFile() {
    return "solrconfig.xml";
  }

//  @Override
//  public String getSolrHome() {
//    return "src/test/resources/solr1";
//  }
}
