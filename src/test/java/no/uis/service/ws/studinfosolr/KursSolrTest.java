package no.uis.service.ws.studinfosolr;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import java.io.Reader;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import no.uis.fsws.studinfo.StudInfoImport;
import no.uis.fsws.studinfo.data.FsSemester;
import no.uis.fsws.studinfo.impl.EmptyStudinfoImport;
import no.uis.service.ws.studinfosolr.util.SolrUtil;

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
  private StudInfoImport studinfoImport;

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
  
  private StudInfoImport getKursStudinfoImport() {
    if (studinfoImport == null) {
      studinfoImport = new EmptyStudinfoImport() {

        @Override
        protected Reader fsGetKurs(int institution, String language) {
          StringBuilder sb = new StringBuilder();
          sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
          sb.append("<fs-studieinfo xmlns=\"http://fsws.usit.no/schemas/studinfo\">");
          sb.append("  <kurs sprak=\"BOKM\u00c5L\">");
          sb.append("    <kursid>");
          sb.append("      <kurskode>DIGIMATTE</kurskode>");
          sb.append("      <tidkode>2012-2013</tidkode>");
          sb.append("    </kursid>");
          sb.append("    <kursnavn>Digitale verkt\u00f8y og unders\u00f8kende arbeidsformer i matematikk</kursnavn>");
          sb.append("    <fagansvarlig>");
          sb.append("      <institusjonsnr>217</institusjonsnr>");
          sb.append("      <fakultetsnr>3</fakultetsnr>");
          sb.append("      <instituttnr>6</instituttnr>");
          sb.append("      <gruppenr>0</gruppenr>");
          sb.append("      <navn>UiS Pluss: Etter- og videreutdanning</navn>");
          sb.append("      <avdnavn>Ledelse og stab</avdnavn>");
          sb.append("    </fagansvarlig>");
          sb.append("    <adminansvarlig>");
          sb.append("      <institusjonsnr>217</institusjonsnr>");
          sb.append("      <fakultetsnr>3</fakultetsnr>");
          sb.append("      <instituttnr>6</instituttnr>");
          sb.append("      <gruppenr>0</gruppenr>");
          sb.append("      <navn>UiS Pluss: Etter- og videreutdanning</navn>");
          sb.append("      <avdnavn>Ledelse og stab</avdnavn>");
          sb.append("    </adminansvarlig>");
          sb.append("    <dato-opptak-fra>2012-03-01</dato-opptak-fra>");
          sb.append("    <dato-opptak-til>2012-06-15</dato-opptak-til>");
          sb.append("    <dato-frist-soknad>2012-06-15</dato-frist-soknad>");
          sb.append("    <email>info@uis.no</email>");
          sb.append("    <fjernundervisning>N</fjernundervisning>");
          sb.append("    <desentral-undervisning>N</desentral-undervisning>");
          sb.append("    <nettbasert-undervisning>N</nettbasert-undervisning>");
          sb.append("    <kan-tilbys>N</kan-tilbys>");
          sb.append("    <skal-avholdes>N</skal-avholdes>");
          sb.append("    <dato-publiseres-fra>2012-03-01</dato-publiseres-fra>");
          sb.append("    <dato-publiseres-til>2012-12-30</dato-publiseres-til>");
          sb.append("    <kurskategori-liste>");
          sb.append("    <kurskategorikode>SKOLE</kurskategorikode>");
          sb.append("    <kurskategorinavn>Skole og barnehage</kurskategorinavn>");
          sb.append("    </kurskategori-liste>");
          sb.append("  </kurs>");
          sb.append("</fs-studieinfo>");
          
          return new StringReader(sb.toString());
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
