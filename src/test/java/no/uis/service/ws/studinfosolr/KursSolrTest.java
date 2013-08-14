package no.uis.service.ws.studinfosolr;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import java.io.Reader;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import lombok.SneakyThrows;

import no.uis.fsws.proxy.EmptyStudinfoImport;
import no.uis.fsws.proxy.StudInfoImport;
import no.uis.fsws.studinfo.data.FsSemester;
import no.uis.service.ws.studinfosolr.mock.EmptyStudinfoProxy;
import no.uis.service.ws.studinfosolr.util.SolrUtil;
import no.usit.fsws.schemas.studinfo.FsStudieinfo;
import no.usit.fsws.schemas.studinfo.Kurs;
import no.usit.fsws.schemas.studinfo.Sprakkode;
import no.usit.fsws.schemas.studinfo.StudinfoProxy;
import no.usit.fsws.schemas.studinfo.Terminkode;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.DateUtil;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class KursSolrTest extends AbstractSolrTestCase {

  private static final String LANGUAGE = "B";
  private static final FsSemester SEMESTER = FsSemester.HOST;
  private static final int YEAR = 2013;
  
  private AbstractApplicationContext appCtx;
  private Map<String, SolrServer> solrServerMap = new HashMap<String, SolrServer>();
  private StudinfoProxy studinfoImport;

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
    bfParent.getDefaultListableBeanFactory().registerSingleton("fsStudInfoImport", getKursStudinfoImport());
    bfParent.getDefaultListableBeanFactory().registerSingleton("employeeNumberResolver", new EmployeeNumberResolver()  {
      
      @Override
      public String findEmployeeNumber(String fnr) {
        // TODO Auto-generated method stub
        return null;
      }
    });
    bfParent.refresh();
    appCtx = new ClassPathXmlApplicationContext(new String[] {"studinfo-solr.xml"}, bfParent);
    appCtx.registerShutdownHook();
  }
  
  private StudinfoProxy getKursStudinfoImport() {
    if (studinfoImport == null) {
      studinfoImport = new EmptyStudinfoProxy() {
        

        @Override
        @SneakyThrows
        public List<Kurs> getKurs(XMLGregorianCalendar arstall, Terminkode terminkode, Sprakkode sprak, int institusjonsnr,
            Integer fakultetsnr, Integer instituttnr, Integer gruppenr)
        {
          String xml = "/test-kurs.xml";
          FsStudieinfo sinfo = unmarshal(xml);

          return sinfo.getKurs();
        }
      };
    }
    return studinfoImport;
  }

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    appCtx.close();
  }
  
  @Test
  public void kursExists() throws Exception {
    
    StudinfoSolrService service = appCtx.getBean("studinfoSolrService", StudinfoSolrService.class);
    try {
      service.updateSolrKurs(YEAR, SEMESTER.toString(), LANGUAGE, SolrType.WWW);
    } catch(SolrUpdateException e) {
      if (e.getCause() instanceof AssumptionViolatedException) {
        throw (AssumptionViolatedException)e.getCause();
      } else {
        throw e;
      }
    }
    
    solrServerMap.get(LANGUAGE).commit();
    SolrParams params = new SolrQuery("cat:STUDINFO AND cat:KURS");
    QueryResponse response = solrServerMap.get(LANGUAGE).query(params);
    int status = response.getStatus();
    assertThat(status, is(equalTo(0)));
    assertThat(response.getResults().getNumFound(), is(1L));
    final SolrDocument doc1 = response.getResults().get(0);
    assertThat(doc1.containsKey("datoPubliseresFra_dt"), is(true));
    final Object pubDato = doc1.getFieldValue("datoPubliseresFra_dt");
    assertThat(pubDato, is(instanceOf(Date.class)));
    
    Appendable out = new StringBuilder();
    DateUtil.formatDate(((Date)pubDato), SolrUtil.getDefaultCalendar(), out);
    assertThat(out.toString(), is("2012-03-01T00:00:00Z"));
  }
}
