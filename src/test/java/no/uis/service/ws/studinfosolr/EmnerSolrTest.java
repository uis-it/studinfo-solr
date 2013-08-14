package no.uis.service.ws.studinfosolr;

import static org.hamcrest.CoreMatchers.*;

import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import lombok.Cleanup;
import lombok.SneakyThrows;

import no.uis.fsws.proxy.EmptyStudinfoImport;
import no.uis.fsws.proxy.StudInfoImport;
import no.uis.fsws.studinfo.data.FsSemester;
import no.uis.fsws.studinfo.data.FsYearSemester;
import no.uis.service.ws.studinfosolr.mock.EmptyStudinfoProxy;
import no.uis.studinfo.convert.SemesterperiodeConverter;
import no.usit.fsws.schemas.studinfo.Emne;
import no.usit.fsws.schemas.studinfo.Emneid;
import no.usit.fsws.schemas.studinfo.FsStudieinfo;
import no.usit.fsws.schemas.studinfo.Obligoppgave;
import no.usit.fsws.schemas.studinfo.Obligund;
import no.usit.fsws.schemas.studinfo.Semester;
import no.usit.fsws.schemas.studinfo.SemesterListe;
import no.usit.fsws.schemas.studinfo.Semesterperiode;
import no.usit.fsws.schemas.studinfo.Sprakkode;
import no.usit.fsws.schemas.studinfo.StudinfoProxy;
import no.usit.fsws.schemas.studinfo.Terminkode;
import no.usit.fsws.schemas.studinfo.Undsemester;
import no.usit.fsws.schemas.studinfo.UndsemesterListe;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.expression.spel.ast.OpPlus;

public class EmnerSolrTest extends AbstractSolrTestCase {

  private AbstractApplicationContext appCtx;
  private Map<String, SolrServer> solrServerMap = new HashMap<String, SolrServer>();
  private StudinfoProxy studinfoImport;
  private static final String LANGUAGE = "B";
  private static final FsSemester SEMESTER = FsSemester.HOST;
  private static final int YEAR = 2013;

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
    bfParent.getDefaultListableBeanFactory().registerSingleton("fsStudInfoImport", getEmneStudinfoImport());
    bfParent.getDefaultListableBeanFactory().registerSingleton("employeeNumberResolver", getEmployeeNumberResolver());
    bfParent.refresh();
    appCtx = new ClassPathXmlApplicationContext(new String[]
      { "studinfo-solr.xml" }, bfParent);
  }

  private EmployeeNumberResolver getEmployeeNumberResolver() {
    return new EmployeeNumberResolver() {

      @Override
      public String findEmployeeNumber(String fnr) {
        return "1234567";
      }
    };
  }

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    appCtx.close();
  }

  @Test
  public void emneExists() throws Exception {

    StudinfoSolrService service = appCtx.getBean("studinfoSolrService", StudinfoSolrService.class);
    try {
      service.updateSolrEmne(null, YEAR, SEMESTER.toString(), LANGUAGE, SolrType.WWW);
    } catch(SolrUpdateException e) {
      if (e.getCause() instanceof AssumptionViolatedException) {
        throw (AssumptionViolatedException)e.getCause();
      } else {
        throw e;
      }
    }

    solrServerMap.get(LANGUAGE).commit();

    // Emne exists
    SolrParams params = new SolrQuery("cat:STUDINFO AND cat:EMNE");
    QueryResponse response = solrServerMap.get(LANGUAGE).query(params);
    assertThat(response.getStatus(), is(equalTo(0)));
    assertThat(response.getResults().getNumFound(), is(1L));
    SolrDocument doc1 = response.getResults().get(0);
    assertThat(doc1, is(instanceOf(SolrDocument.class)));

    final Object emnenavn = doc1.getFieldValue("emnenavn_s");
    assertThat(emnenavn, is(instanceOf(String.class)));
    assertThat((String)doc1.getFieldValue("emnenavn_s"), is("Algoritmer og datastrukturer"));

    Assume.assumeTrue(appCtx.containsBean("employeeNumberResolver"));
    // Fagperson exists
    params = new SolrQuery("cat:fagperson");
    response = solrServerMap.get(LANGUAGE).query(params);
    assertThat(response.getStatus(), is(equalTo(0)));
    assertThat(response.getResults().getNumFound(), is(1L));
    doc1 = response.getResults().get(0);
    assertThat(doc1, is(instanceOf(SolrDocument.class)));

    final Object fornavn = doc1.getFieldValue("fornavn_s");
    assertThat(fornavn, is(instanceOf(String.class)));
    assertThat((String)fornavn, is("Alan"));
    assertThat(doc1.containsKey("ansattnummer_s"), is(true));
  }

  private StudinfoProxy getEmneStudinfoImport() {

    if (this.studinfoImport == null) {
      studinfoImport = new EmptyStudinfoProxy() {

        @Override
        @SneakyThrows
        public List<Emne> getEmnerForOrgenhet(XMLGregorianCalendar arstall, Terminkode terminkode, Sprakkode sprak,
            int institusjonsnr, Integer fakultetsnr, Integer instituttnr, Integer gruppenr)
        {

          String xml = "/test-emne.xml";
          FsStudieinfo sinfo = unmarshal(xml);

          return sinfo.getEmne();
        }
      };
    }
    return this.studinfoImport;
  }
}
