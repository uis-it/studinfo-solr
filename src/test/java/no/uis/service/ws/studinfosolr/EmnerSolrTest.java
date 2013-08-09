package no.uis.service.ws.studinfosolr;

import static org.hamcrest.CoreMatchers.*;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import no.uis.fsws.proxy.EmptyStudinfoImport;
import no.uis.fsws.proxy.StudInfoImport;
import no.uis.fsws.studinfo.data.FsSemester;

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

public class EmnerSolrTest extends AbstractSolrTestCase {

  private AbstractApplicationContext appCtx;
  private Map<String, SolrServer> solrServerMap = new HashMap<String, SolrServer>();
  private StudInfoImport studinfoImport;
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
    appCtx = new ClassPathXmlApplicationContext(new String[] {"studinfo-solr.xml"}, bfParent);
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
  
  private StudInfoImport getEmneStudinfoImport() {
    
    if (this.studinfoImport == null) {
      studinfoImport = new EmptyStudinfoImport() {

        @Override
        protected Reader fsGetEmne(int institution, int faculty, int year, String semester, String language) {
          StringBuilder sb = new StringBuilder();
          // This is not a real "Emne", just a short version of DAT200_1
          sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
          sb.append("<fs-studieinfo xmlns=\"http://fsws.usit.no/schemas/studinfo\">\n");
          sb.append("  <emne sprak=\"BOKM\u00c5L\">\n");
          sb.append("    <emneid>\n");
          sb.append("      <institusjonsnr>217</institusjonsnr>\n");
          sb.append("      <emnekode>DAT200</emnekode>\n");
          sb.append("      <versjonskode>1</versjonskode>\n");
          sb.append("    </emneid>\n");
          sb.append("    <emnenavn>Algoritmer og datastrukturer</emnenavn>\n");
          sb.append("    <emnenavn_en>Algorithms and Datastructures</emnenavn_en>\n");
          sb.append("    <studiepoeng>10</studiepoeng>\n");
          sb.append("    <status-privatist>N</status-privatist>\n");
          sb.append("    <studieniva>LN</studieniva>\n");
          sb.append("    <nuskode>654122</nuskode>\n");
          sb.append("    <enkeltemneopptak>N</enkeltemneopptak>\n");
          sb.append("    <studierettkrav>J</studierettkrav>\n");
          sb.append("    <status_oblig>J</status_oblig>\n");
          sb.append("    <obligund>\n");
          sb.append("      <obligoppgave nr=\"OBL1\">Innleveringsoppgaver</obligoppgave>\n");
          sb.append("    </obligund>\n");
          sb.append("    <undervisningssemester>H\u00f8st</undervisningssemester>\n");
          sb.append("    <antall-undsemester>1</antall-undsemester>\n");
          sb.append("    <antall-forelesningstimer>4</antall-forelesningstimer>\n");
          sb.append("    <undsemester>\n");
          sb.append("      <semester nr=\"1\">H\u00f8st</semester>\n");
          sb.append("      <forstegang>2012H</forstegang>\n");
          sb.append("    </undsemester>\n");
          sb.append("    <url>http://student.uis.no/studieinformasjon/soek_i_studier1/?spraak=Bokm%C3%A5l&amp;emneID=DAT200_1&amp;year=2012</url>\n");
          sb.append("    <periode-eks>\n");
          sb.append("      <forstegang>2013H</forstegang>\n");
          sb.append("    </periode-eks>\n");
          sb.append("    <periode-und>\n");
          sb.append("      <forstegang>2013H</forstegang>\n");
          sb.append("    </periode-und>\n");
          sb.append("    <eksamenssemester>H\u00f8st</eksamenssemester>\n");
          sb.append("    <anbefalte-forkunnskaper>DAT100 Objektorientert programmering</anbefalte-forkunnskaper>\n");
          sb.append("    <inngar-i-studieprogram-liste>\n");
          sb.append("      <inngar-i-studieprogram>\n");
          sb.append("        <studieprogramkode>B-DATA</studieprogramkode>\n");
          sb.append("        <studieprogramnavn>Data - bachelorstudium i ingeni\u00f8rfag</studieprogramnavn>\n");
          sb.append("      </inngar-i-studieprogram>\n");
          sb.append("      <inngar-i-studieprogram>\n");
          sb.append("        <studieprogramkode>B-ELEKTRO</studieprogramkode>\n");
          sb.append("        <studieprogramnavn>Elektro - bachelorstudium i ingeni\u00f8rfag</studieprogramnavn>\n");
          sb.append("      </inngar-i-studieprogram>\n");
          sb.append("    </inngar-i-studieprogram-liste>\n");
          sb.append("    <inngar-i-fag-liste>\n");
          sb.append("      <inngar-i-fag>\n");
          sb.append("        <fagkode>TEKNISK</fagkode>\n");
          sb.append("      </inngar-i-fag>\n");
          sb.append("    </inngar-i-fag-liste>\n");
          sb.append("    <fagansvarlig>\n");
          sb.append("      <institusjonsnr>217</institusjonsnr>\n");
          sb.append("      <fakultetsnr>8</fakultetsnr>\n");
          sb.append("      <instituttnr>4</instituttnr>\n");
          sb.append("      <gruppenr>0</gruppenr>\n");
          sb.append("      <navn>Institutt for data- og elektroteknikk</navn>\n");
          sb.append("      <avdnavn>Det teknisk-naturvitenskapelige fakultet</avdnavn>\n");
          sb.append("    </fagansvarlig>\n");
          sb.append("    <adminansvarlig>\n");
          sb.append("      <institusjonsnr>217</institusjonsnr>\n");
          sb.append("      <fakultetsnr>8</fakultetsnr>\n");
          sb.append("      <instituttnr>4</instituttnr>\n");
          sb.append("      <gruppenr>0</gruppenr>\n");
          sb.append("      <navn>Institutt for data- og elektroteknikk</navn>\n");
          sb.append("      <avdnavn>Det teknisk-naturvitenskapelige fakultet</avdnavn>\n");
          sb.append("    </adminansvarlig>\n");
          sb.append("    <sprak-liste>\n");
          sb.append("      <sprak>\n");
          sb.append("        <sprakkode>NO</sprakkode>\n");
          sb.append("        <undervisning>J</undervisning>\n");
          sb.append("        <vurdering>N</vurdering>\n");
          sb.append("      </sprak>\n");
          sb.append("    </sprak-liste>\n");
          sb.append("    <fagperson-liste>\n");
          sb.append("      <fagperson>\n");
          sb.append("        <personid>123456</personid>\n");
          sb.append("        <fnr>23061212345</fnr>\n");
          sb.append("        <personrolle>Emneansvarlig</personrolle>\n");
          sb.append("        <personnavn>\n");
          sb.append("          <etternavn>Turing</etternavn>\n");
          sb.append("          <fornavn>Alan</fornavn>\n");
          sb.append("        </personnavn>\n");
          sb.append("        <dato-fra>2012-08-01</dato-fra>\n");
          sb.append("      </fagperson>\n");
          sb.append("    </fagperson-liste>\n");
          sb.append("    <eksamensordning>\n");
          sb.append("      <eksamensordningid>S</eksamensordningid>\n");
          sb.append("      <eksamensordningnavn>Skriftlig(e) eksamen(er)</eksamensordningnavn>\n");
          sb.append("      <default>J</default>\n");
          sb.append("      <meld_studentweb>J</meld_studentweb>\n");
          sb.append("      <lovlig-hjelpemiddel>\");\n");
          sb.append("        <hjelpemiddel>\n");
          sb.append("          <hjelpemiddelnavn>Ingen hjelpemidler tillatt. </hjelpemiddelnavn>\n");
          sb.append("          <hjelpemiddelmerknad/>\n");
          sb.append("        </hjelpemiddel>\n");
          sb.append("      </lovlig-hjelpemiddel>\n");
          sb.append("      <karregel>A - F</karregel>\n");
          sb.append("      <eksamensdel>\n");
          sb.append("        <eksamensdelnr>0</eksamensdelnr>\n");
          sb.append("        <eksamensdelnavn>En skriftlig pr\u00f8ve</eksamensdelnavn>\n");
          sb.append("        <varighet>4 Timer</varighet>\n");
          sb.append("        <eksamensform>Skriftlig eksamen</eksamensform>\n");
          sb.append("        <dato-eksamen/>\n");
          sb.append("      </eksamensdel>\n");
          sb.append("    </eksamensordning>\n");
          sb.append("    <vurdordning>\n");
          sb.append("      <vurdordningid>S</vurdordningid>\n");
          sb.append("      <vurdordningnavn>Skriftlig(e) eksamen(er)</vurdordningnavn>\n");
          sb.append("      <default>J</default>\n");
          sb.append("      <meld_studentweb>J</meld_studentweb>\n");
          sb.append("      <vurdkombinasjon niva=\"1\">\n");
          sb.append("        <vurdkombkode>S</vurdkombkode>\n");
          sb.append("        <vurdkombnavn>En skriftlig pr\u00f8ve</vurdkombnavn>\n");
          sb.append("        <vurdform>Skriftlig eksamen</vurdform>\n");
          sb.append("        <vurdkombtype>Eksamen</vurdkombtype>\n");
          sb.append("        <forstegang>2012H</forstegang>\n");
          sb.append("        <varighet>4 Timer</varighet>\n");
          sb.append("        <vurdering>J</vurdering>\n");
          sb.append("        <alle-kandidater>J</alle-kandidater>\n");
          sb.append("        <karregelkode>30</karregelkode>\n");
          sb.append("        <karregel>A - F</karregel>\n");
          sb.append("        <vurdkombtider>\n");
          sb.append("          <vurdkombtid>\n");
          sb.append("            <vurdtidkode>12</vurdtidkode>\n");
          sb.append("            <vurdtidkode_reell>12</vurdtidkode_reell>\n");
          sb.append("            <arsinkrement>0</arsinkrement>\n");
          sb.append("            <vurdstatuskode>ORD</vurdstatuskode>\n");
          sb.append("            <liketallsaar>J</liketallsaar>\n");
          sb.append("            <oddetallsaar>J</oddetallsaar>\n");
          sb.append("            <forstegang/>\n");
          sb.append("          </vurdkombtid>\n");
          sb.append("        </vurdkombtider>\n");
          sb.append("        <lovlig-hjelpemiddel>\n");
          sb.append("          <hjelpemiddel>\n");
          sb.append("            <hjelpemiddelnavn>Ingen hjelpemidler tillatt. </hjelpemiddelnavn>\n");
          sb.append("            <hjelpemiddelmerknad/>\n");
          sb.append("          </hjelpemiddel>\n");
          sb.append("        </lovlig-hjelpemiddel>\n");
          sb.append("        <vurdenheter>\n");
          sb.append("          <vurdenhet>\n");
          sb.append("            <tid>201312</tid>\n");
          sb.append("            <tid-reell>201312</tid-reell>\n");
          sb.append("            <vurdstatus>Ordin\u00e6r eksamen</vurdstatus>\n");
          sb.append("          </vurdenhet>\n");
          sb.append("        </vurdenheter>\n");
          sb.append("        <vurdkombinasjon niva=\"2\">\n");
          sb.append("          <vurdkombkode>OBL1</vurdkombkode>\n");
          sb.append("          <vurdkombnavn>Innleveringsoppgaver</vurdkombnavn>\n");
          sb.append("          <vurdkombtype>Obligatorisk \u00f8velse</vurdkombtype>\n");
          sb.append("          <forstegang>2012H</forstegang>\n");
          sb.append("          <vurdering>J</vurdering>\n");
          sb.append("          <karregelkode>5</karregelkode>\n");
          sb.append("          <karregel>Godkjent - Ikke godkjent</karregel>\n");
          sb.append("          <vurdkombtider>\n");
          sb.append("            <vurdkombtid>\n");
          sb.append("              <vurdtidkode>12</vurdtidkode>\n");
          sb.append("              <vurdtidkode_reell>12</vurdtidkode_reell>\n");
          sb.append("              <arsinkrement>0</arsinkrement>\n");
          sb.append("              <vurdstatuskode>ORD</vurdstatuskode>\n");
          sb.append("              <liketallsaar>J</liketallsaar>\n");
          sb.append("              <oddetallsaar>J</oddetallsaar>\n");
          sb.append("              <forstegang/>\n");
          sb.append("            </vurdkombtid>\n");
          sb.append("          </vurdkombtider>\n");
          sb.append("          <vurdenheter>\n");
          sb.append("            <vurdenhet>\n");
          sb.append("              <tid>201312</tid>\n");
          sb.append("              <tid-reell>201312</tid-reell>\n");
          sb.append("              <vurdstatus>Ordin\u00e6r eksamen</vurdstatus>\n");
          sb.append("            </vurdenhet>\n");
          sb.append("          </vurdenheter>\n");
          sb.append("        </vurdkombinasjon>\n");
          sb.append("      </vurdkombinasjon>\n");
          sb.append("    </vurdordning>\n");
          sb.append("    <vektingsreduksjon>\n");
          sb.append("      <redregel>\n");
          sb.append("        <studiepoeng-reduksjon>6</studiepoeng-reduksjon>\n");
          sb.append("        <periode>\n");
          sb.append("          <fradato>2012-08-01</fradato>\n");
          sb.append("          <tildato/>\n");
          sb.append("        </periode>\n");
          sb.append("        <emne>\n");
          sb.append("          <emneid>\n");
          sb.append("            <institusjonsnr>217</institusjonsnr>\n");
          sb.append("            <emnekode>TE0458</emnekode>\n");
          sb.append("            <versjonskode>1</versjonskode>\n");
          sb.append("          </emneid>\n");
          sb.append("          <emnenavn>Datastrukturer og algoritmer</emnenavn>\n");
          sb.append("        </emne>\n");
          sb.append("      </redregel>\n");
          sb.append("      <redregel>\n");
          sb.append("        <studiepoeng-reduksjon>6</studiepoeng-reduksjon>\n");
          sb.append("        <periode>\n");
          sb.append("          <fradato>2012-08-01</fradato>\n");
          sb.append("          <tildato/>\n");
          sb.append("        </periode>\n");
          sb.append("        <emne>\n");
          sb.append("          <emneid>\n");
          sb.append("            <institusjonsnr>217</institusjonsnr>\n");
          sb.append("            <emnekode>TE0458</emnekode>\n");
          sb.append("            <versjonskode>A</versjonskode>\n");
          sb.append("          </emneid>\n");
          sb.append("          <emnenavn>Datastrukturer og algoritmer</emnenavn>\n");
          sb.append("        </emne>\n");
          sb.append("      </redregel>\n");
          sb.append("      <redregel>\n");
          sb.append("        <studiepoeng-reduksjon>10</studiepoeng-reduksjon>\n");
          sb.append("        <periode>\n");
          sb.append("          <fradato>2012-08-01</fradato>\n");
          sb.append("          <tildato/>\n");
          sb.append("        </periode>\n");
          sb.append("        <emne>\n");
          sb.append("          <emneid>\n");
          sb.append("            <institusjonsnr>217</institusjonsnr>\n");
          sb.append("            <emnekode>BIE270</emnekode>\n");
          sb.append("            <versjonskode>1</versjonskode>\n");
          sb.append("          </emneid>\n");
          sb.append("          <emnenavn>Datastrukturer og algoritmer</emnenavn>\n");
          sb.append("        </emne>\n");
          sb.append("      </redregel>\n");
          sb.append("    </vektingsreduksjon>\n");
          sb.append("    <arbeidsformer><![CDATA[Forelesinger i klasserom og veiledning p\u00e5 datalab. For \u00e5 ta eksamen i kurset m\u00e5 f\u00f8lgende oppgaver v\u00e6re godkjente: oppgave 1, oppgave 2 eller oppgave 3, oppgave 4 eller oppgave 5. Siste frist for \u00e5 f\u00e5 godkjent \u00f8vinger er tre uker f\u00f8r eksamen. ]]></arbeidsformer>\n");
          sb.append("    <innhold><![CDATA[Effektivitetsanalyse. Definisjon, bruk og implementeringer av abstrakte datatyper som: Stabler, k\u00f8er, lister, trestrukturer, grafer, prioritetsk\u00f8er, hauger. Hash-teknikker. Bruk og implementering av datastrukturer som kan representere grafer. Sorterings teknikker. Rekursjon som programmeringsteknikk.]]></innhold>\n");
          sb.append("    <litteratur><![CDATA[Data Structures and Problem Solving Using Java Mark Allen WeissPublished by Addision-Wesley]]></litteratur>\n");
          sb.append("    <laringsutbytte><![CDATA[<p>Etter \u00e5 ha tatt dette emnet skal studenten: </p><p>Kunnskap </p><ul><li>Kunne bruke standard datastrukturene til \u00e5 lage effektive program. </li></ul><p> </p><p>Ferdigheter </p><ul><li>V\u00e6re i stand til \u00e5 beregne effektiviteten til algoritmer </li><li>V\u00e6re i stand til \u00e5 forst\u00e5 og lage effektive rekursive algoritmer. </li><li>V\u00e6re i stand til \u00e5 implementere effektive algoritmer for sortering og s\u00f8king. </li></ul><p> </p><p>Generell kompetanse </p><ul><li>Vite hvordan datastrukturer for lister, k\u00f8er, stabler (stack), hauger (heap), bin\u00e6re tre, og grafer kan implementeres ved hjelp av Java.</li></ul><p> </p>]]></laringsutbytte>\n");
          sb.append("    <studentevaluering><![CDATA[Skjer vanligvis gjennom skjema og/eller i samtaler etter til gjeldende retningslinjer.]]></studentevaluering>\n");
          sb.append("    <apent_for_tillegg><![CDATA[<p>Bachelor- niv\u00e5 p\u00e5 Det teknisk-naturvitenskaplige fakultetet. </p><p>Master niv\u00e5 p\u00e5 Det teknisk-naturvitenskaplige fakultetet</p>]]></apent_for_tillegg>\n");
          sb.append("  </emne>\n");
          sb.append("</fs-studieinfo>\n");
          return new StringReader(sb.toString());
        }
      };
    }
    return this.studinfoImport;
  }
}
