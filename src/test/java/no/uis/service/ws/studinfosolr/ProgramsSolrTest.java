package no.uis.service.ws.studinfosolr;

import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import no.uis.fsws.studinfo.StudInfoImport;
import no.uis.fsws.studinfo.data.FsSemester;
import no.uis.fsws.studinfo.impl.AbstractStudinfoImport;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class ProgramsSolrTest extends AbstractSolrTestCase {

  private static final String LANGUAGE = "B";
  private static final FsSemester SEMESTER = FsSemester.HOST;
  private static final int YEAR = 2013;
  private StudInfoImport studinfoImport;
  private AbstractApplicationContext appCtx;
  private Map<String, SolrServer> solrServerMap = new HashMap<String, SolrServer>();

  @BeforeClass
  public static void initSolrTestHarness() throws Exception {
    initCore("solrconfig-studinfo.xml", "schema-studinfo.xml", "src/test/resources/solr", "collection1");
  }
  
  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    SolrServer solrServer = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
    solrServerMap.put(LANGUAGE, solrServer);
    
    StaticApplicationContext bfParent = new StaticApplicationContext();
    bfParent.getDefaultListableBeanFactory().registerSingleton("solrServerMap", solrServerMap);
    bfParent.getDefaultListableBeanFactory().registerSingleton("fsStudInfoImport", getStudieprogramImport());
    bfParent.getDefaultListableBeanFactory().registerSingleton("employeeNumberResolver", getEmployeeNumberResolver());
    bfParent.refresh();
    appCtx = new ClassPathXmlApplicationContext(new String[] {"studinfo-solr.xml"}, bfParent);
  }
  
  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    appCtx.close();
  }
  
  @Test
  public void programExists() throws Exception {

    StudinfoSolrService service = appCtx.getBean("studinfoSolrService", StudinfoSolrService.class);
    try {
      service.updateSolrStudieprogram(null, YEAR, SEMESTER.toString(), LANGUAGE, SolrType.WWW);
    } catch (SolrUpdateException e) {
      if (e.getCause() instanceof AssumptionViolatedException) {
        throw (AssumptionViolatedException)e.getCause();
      } else {
        throw e;
      }
    }
    
    solrServerMap.get(LANGUAGE).commit();
    SolrParams params = new SolrQuery("cat:STUDINFO AND cat:STUDIEPROGRAM");
    QueryResponse response = solrServerMap.get(LANGUAGE).query(params);
    int status = response.getStatus();
    assertThat(status, is(equalTo(0)));
    assertThat(response.getResults().getNumFound(), is(1L));
  }
  
  private StudInfoImport getStudieprogramImport() {
    if (this.studinfoImport == null) {
      studinfoImport = new AbstractStudinfoImport() {

        @Override
        protected Reader fsGetStudieprogram(int institution, int faculty, int year, String semester, boolean includeEP, String language) {
          StringWriter sw = new StringWriter();
          InputStream inStream = getClass().getResourceAsStream("/studieprogram.xml");
          try {
            IOUtils.copy(inStream, sw, "UTF-8");
          } catch(IOException e) {
            throw new RuntimeException(e);
          }
          return new StringReader(sw.toString());
        }
        
        @Override
        protected Reader fsGetKurs(int institution, String language) {
          return null;
        }

        @Override
        protected Reader fsGetEmne(int institution, int faculty, int year, String semester, String language) {
          return null;
        }
      };
    }
    return this.studinfoImport;
  }

  private EmployeeNumberResolver getEmployeeNumberResolver() {
    return new EmployeeNumberResolver() {
      
      @Override
      public String findEmployeeNumber(String fnr) {
        return "1234567";
      }
    };
  }
}
