package no.uis.service.ws.studinfosolr;

import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import lombok.SneakyThrows;

import no.uis.fsws.proxy.EmptyStudinfoImport;
import no.uis.fsws.proxy.StudInfoImport;
import no.uis.fsws.studinfo.data.FsSemester;
import no.uis.service.ws.studinfosolr.mock.EmptyStudinfoProxy;
import no.usit.fsws.schemas.studinfo.FsStudieinfo;
import no.usit.fsws.schemas.studinfo.Sprakkode;
import no.usit.fsws.schemas.studinfo.Studieprogram;
import no.usit.fsws.schemas.studinfo.StudinfoProxy;
import no.usit.fsws.schemas.studinfo.Terminkode;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.XmlStreamReader;
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
  private StudinfoProxy studinfoImport;
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
  
  private StudinfoProxy getStudieprogramImport() {
    if (this.studinfoImport == null) {
      studinfoImport = new EmptyStudinfoProxy() {

        @Override
        public List<Studieprogram> getStudieprogrammerForOrgenhet(XMLGregorianCalendar arstall, Terminkode terminkode,
            Sprakkode sprak, int institusjonsnr, Integer fakultetsnr, Integer instituttnr, Integer gruppenr, boolean medUPinfo)
        {
          String xml = "/test-studieprogram.xml";
          FsStudieinfo sinfo = unmarshal(xml);

          return sinfo.getStudieprogram();
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
