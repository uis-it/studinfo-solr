package no.uis.service.ws.studinfosolr;

import static org.hamcrest.CoreMatchers.*;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import no.uis.service.component.fsimport.StudInfoImport;
import no.uis.service.component.fsimport.impl.AbstractStudinfoImport;
import no.uis.service.studinfo.data.FsSemester;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.After;
import org.junit.Before;
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
      service.updateSolrEmne(YEAR, SEMESTER.toString(), LANGUAGE);
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
  
  @Override
  public String getSchemaFile() {
    return "schema-studinfo.xml";
  }

  @Override
  public String getSolrConfigFile() {
    return "solrconfig-studinfo.xml";
  }
  
  private StudInfoImport getEmneStudinfoImport() {
    
    if (this.studinfoImport == null) {
      studinfoImport = new AbstractStudinfoImport() {

        @Override
        protected Reader fsGetEmne(int institution, int faculty, int year, String semester, String language) {
          StringBuilder sb = new StringBuilder();
          // This is not a real "Emne", just a short version of DAT200_1
          sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
          sb.append("<fs-studieinfo xmlns=\"http://fsws.usit.no/schemas/studinfo\">\n");
          sb.append("<emne sprak=\"BOKMÅL\">\n");
          sb.append("<emneid>\n");
          sb.append("<institusjonsnr>217</institusjonsnr>\n");
          sb.append("<emnekode>DAT200</emnekode>\n");
          sb.append("<versjonskode>1</versjonskode>\n");
          sb.append("</emneid>\n");
          sb.append("<emnenavn>Algoritmer og datastrukturer</emnenavn>\n");
          sb.append("<emnenavn_en>Algorithms and Datastructures</emnenavn_en>\n");
          sb.append("<studiepoeng>10</studiepoeng>\n");
          sb.append("<status-privatist>N</status-privatist>\n");
          sb.append("<studieniva>LN</studieniva>\n");
          sb.append("<nuskode>654122</nuskode>\n");
          sb.append("<enkeltemneopptak>N</enkeltemneopptak>\n");
          sb.append("<studierettkrav>J</studierettkrav>\n");
          sb.append("<status_oblig>J</status_oblig>\n");
          sb.append("<obligund>\n");
          sb.append("<obligoppgave nr=\"OBL1\">Innleveringsoppgaver</obligoppgave>\n");
          sb.append("</obligund>\n");
          sb.append("<undervisningssemester>Høst</undervisningssemester>\n");
          sb.append("<antall-undsemester>1</antall-undsemester>\n");
          sb.append("<antall-forelesningstimer>4</antall-forelesningstimer>\n");
          sb.append("<undsemester>\n");
          sb.append("<semester nr=\"1\">Høst</semester>\n");
          sb.append("<forstegang>2012H</forstegang>\n");
          sb.append("</undsemester>\n");
          sb.append("<url>http://student.uis.no/studieinformasjon/soek_i_studier1/?spraak=Bokm%C3%A5l&amp;emneID=DAT200_1&amp;year=2012</url>\n");
          sb.append("<periode-eks>\n");
          sb.append("<forstegang>2013H</forstegang>\n");
          sb.append("</periode-eks>\n");
          sb.append("<periode-und>\n");
          sb.append("<forstegang>2013H</forstegang>\n");
          sb.append("</periode-und>\n");
          sb.append("<eksamenssemester>Høst</eksamenssemester>\n");
          sb.append("<anbefalte-forkunnskaper>DAT100 Objektorientert programmering</anbefalte-forkunnskaper>\n");
          sb.append("<inngar-i-studieprogram>\n");
          sb.append("<studieprogramkode>B-DATA</studieprogramkode>\n");
          sb.append("<studieprogramnavn>Data - bachelorstudium i ingeniørfag</studieprogramnavn>\n");
          sb.append("</inngar-i-studieprogram>\n");
          sb.append("<inngar-i-studieprogram>\n");
          sb.append("<studieprogramkode>B-ELEKTRO</studieprogramkode>\n");
          sb.append("<studieprogramnavn>Elektro - bachelorstudium i ingeniørfag</studieprogramnavn>\n");
          sb.append("</inngar-i-studieprogram>\n");
          sb.append("<inngar-i-fag><fagkode>TEKNISK</fagkode></inngar-i-fag>\n");
          sb.append("<fagansvarlig>\n");
          sb.append("<institusjonsnr>217</institusjonsnr>\n");
          sb.append("<fakultetsnr>8</fakultetsnr>\n");
          sb.append("<instituttnr>4</instituttnr>\n");
          sb.append("<gruppenr>0</gruppenr>\n");
          sb.append("<navn>Institutt for data- og elektroteknikk</navn>\n");
          sb.append("<avdnavn>Det teknisk-naturvitenskapelige fakultet</avdnavn>\n");
          sb.append("</fagansvarlig>\n");
          sb.append("<adminansvarlig>\n");
          sb.append("<institusjonsnr>217</institusjonsnr>\n");
          sb.append("<fakultetsnr>8</fakultetsnr>\n");
          sb.append("<instituttnr>4</instituttnr>\n");
          sb.append("<gruppenr>0</gruppenr>\n");
          sb.append("<navn>Institutt for data- og elektroteknikk</navn>\n");
          sb.append("<avdnavn>Det teknisk-naturvitenskapelige fakultet</avdnavn>\n");
          sb.append("</adminansvarlig>\n");
          sb.append("<sprak-liste>\n");
          sb.append("<sprak>\n");
          sb.append("<sprakkode>NO</sprakkode>\n");
          sb.append("<undervisning>J</undervisning>\n");
          sb.append("<vurdering>N</vurdering>\n");
          sb.append("</sprak>\n");
          sb.append("</sprak-liste>\n");
          sb.append("<fagperson-liste>\n");
          sb.append("<fagperson>\n");
          sb.append("<personid>123456</personid>\n");
          sb.append("<fnr>23061212345</fnr>\n");
          sb.append("<personrolle>Emneansvarlig</personrolle>\n");
          sb.append("<personnavn>\n");
          sb.append("<etternavn>Turing</etternavn>\n");
          sb.append("<fornavn>Alan</fornavn>\n");
          sb.append("</personnavn>\n");
          sb.append("<dato-fra>2012-08-01</dato-fra>\n");
          sb.append("</fagperson>\n");
          sb.append("</fagperson-liste>\n");
          sb.append("<eksamensordning>\n");
          sb.append("<eksamensordningid>S</eksamensordningid>\n");
          sb.append("<eksamensordningnavn>Skriftlig(e) eksamen(er)</eksamensordningnavn>\n");
          sb.append("<default>J</default>\n");
          sb.append("<meld_studentweb>J</meld_studentweb>\n");
          sb.append("<lovlig-hjelpemiddel><hjelpemiddel>\n");
          sb.append("<hjelpemiddelnavn>Ingen hjelpemidler tillatt. </hjelpemiddelnavn>\n");
          sb.append("<hjelpemiddelmerknad/>\n");
          sb.append("</hjelpemiddel>\n");
          sb.append("</lovlig-hjelpemiddel>\n");
          sb.append("<karregel>A - F</karregel>\n");
          sb.append("<eksamensdel>\n");
          sb.append("<eksamensdelnr>0</eksamensdelnr>\n");
          sb.append("<eksamensdelnavn>En skriftlig prøve</eksamensdelnavn>\n");
          sb.append("<varighet>4 Timer</varighet>\n");
          sb.append("<eksamensform>Skriftlig eksamen</eksamensform>\n");
          sb.append("<dato-eksamen/>\n");
          sb.append("</eksamensdel>\n");
          sb.append("</eksamensordning>\n");
          sb.append("<vurdordning>\n");
          sb.append("<vurdordningid>S</vurdordningid>\n");
          sb.append("<vurdordningnavn>Skriftlig(e) eksamen(er)</vurdordningnavn>\n");
          sb.append("<default>J</default>\n");
          sb.append("<meld_studentweb>J</meld_studentweb>\n");
          sb.append("<vurdkombinasjon niva=\"1\">\n");
          sb.append("<vurdkombkode>S</vurdkombkode>\n");
          sb.append("<vurdkombnavn>En skriftlig prøve</vurdkombnavn>\n");
          sb.append("<vurdform>Skriftlig eksamen</vurdform>\n");
          sb.append("<vurdkombtype>Eksamen</vurdkombtype>\n");
          sb.append("<forstegang>2012H</forstegang>\n");
          sb.append("<varighet>4 Timer</varighet>\n");
          sb.append("<vurdering>J</vurdering>\n");
          sb.append("<alle-kandidater>J</alle-kandidater>\n");
          sb.append("<karregelkode>30</karregelkode>\n");
          sb.append("<karregel>A - F</karregel>\n");
          sb.append("<vurdkombtider><vurdkombtid>\n");
          sb.append("<vurdtidkode>12</vurdtidkode>\n");
          sb.append("<vurdtidkode_reell>12</vurdtidkode_reell>\n");
          sb.append("<arsinkrement>0</arsinkrement>\n");
          sb.append("<vurdstatuskode>ORD</vurdstatuskode>\n");
          sb.append("<liketallsaar>J</liketallsaar>\n");
          sb.append("<oddetallsaar>J</oddetallsaar>\n");
          sb.append("<forstegang/>\n");
          sb.append("</vurdkombtid>\n");
          sb.append("</vurdkombtider>\n");
          sb.append("<lovlig-hjelpemiddel>\n");
          sb.append("<hjelpemiddel>\n");
          sb.append("<hjelpemiddelnavn>Ingen hjelpemidler tillatt. </hjelpemiddelnavn>\n");
          sb.append("<hjelpemiddelmerknad/>\n");
          sb.append("</hjelpemiddel>\n");
          sb.append("</lovlig-hjelpemiddel>\n");
          sb.append("<vurdenheter>\n");
          sb.append("<vurdenhet>\n");
          sb.append("<tid>201312</tid>\n");
          sb.append("<tid-reell>201312</tid-reell>\n");
          sb.append("<vurdstatus>Ordinær eksamen</vurdstatus>\n");
          sb.append("</vurdenhet>\n");
          sb.append("</vurdenheter>\n");
          sb.append("<vurdkombinasjon niva=\"2\">\n");
          sb.append("<vurdkombkode>OBL1</vurdkombkode>\n");
          sb.append("<vurdkombnavn>Innleveringsoppgaver</vurdkombnavn>\n");
          sb.append("<vurdkombtype>Obligatorisk øvelse</vurdkombtype>\n");
          sb.append("<forstegang>2012H</forstegang>\n");
          sb.append("<vurdering>J</vurdering>\n");
          sb.append("<karregelkode>5</karregelkode>\n");
          sb.append("<karregel>Godkjent - Ikke godkjent</karregel>\n");
          sb.append("<vurdkombtider>\n");
          sb.append("<vurdkombtid>\n");
          sb.append("<vurdtidkode>12</vurdtidkode>\n");
          sb.append("<vurdtidkode_reell>12</vurdtidkode_reell>\n");
          sb.append("<arsinkrement>0</arsinkrement>\n");
          sb.append("<vurdstatuskode>ORD</vurdstatuskode>\n");
          sb.append("<liketallsaar>J</liketallsaar>\n");
          sb.append("<oddetallsaar>J</oddetallsaar>\n");
          sb.append("<forstegang/>\n");
          sb.append("</vurdkombtid>\n");
          sb.append("</vurdkombtider>\n");
          sb.append("<vurdenheter>\n");
          sb.append("<vurdenhet>\n");
          sb.append("<tid>201312</tid>\n");
          sb.append("<tid-reell>201312</tid-reell>\n");
          sb.append("<vurdstatus>Ordinær eksamen</vurdstatus>\n");
          sb.append("</vurdenhet>\n");
          sb.append("</vurdenheter>\n");
          sb.append("</vurdkombinasjon>\n");
          sb.append("</vurdkombinasjon>\n");
          sb.append("</vurdordning>\n");
          sb.append("<vektingsreduksjon>\n");
          sb.append("<redregel>\n");
          sb.append("<studiepoeng-reduksjon>6</studiepoeng-reduksjon>\n");
          sb.append("<periode>\n");
          sb.append("<fradato>2012-08-01</fradato>\n");
          sb.append("<tildato/>\n");
          sb.append("</periode>\n");
          sb.append("<emne>\n");
          sb.append("<emneid>\n");
          sb.append("<institusjonsnr>217</institusjonsnr>\n");
          sb.append("<emnekode>TE0458</emnekode>\n");
          sb.append("<versjonskode>1</versjonskode>\n");
          sb.append("</emneid>\n");
          sb.append("<emnenavn>Datastrukturer og algoritmer</emnenavn>\n");
          sb.append("</emne>\n");
          sb.append("</redregel>\n");
          sb.append("<redregel>\n");
          sb.append("<studiepoeng-reduksjon>6</studiepoeng-reduksjon>\n");
          sb.append("<periode>\n");
          sb.append("<fradato>2012-08-01</fradato>\n");
          sb.append("<tildato/>\n");
          sb.append("</periode>\n");
          sb.append("<emne>\n");
          sb.append("<emneid>\n");
          sb.append("<institusjonsnr>217</institusjonsnr>\n");
          sb.append("<emnekode>TE0458</emnekode>\n");
          sb.append("<versjonskode>A</versjonskode>\n");
          sb.append("</emneid>\n");
          sb.append("<emnenavn>Datastrukturer og algoritmer</emnenavn>\n");
          sb.append("</emne>\n");
          sb.append("</redregel>\n");
          sb.append("<redregel>\n");
          sb.append("<studiepoeng-reduksjon>10</studiepoeng-reduksjon>\n");
          sb.append("<periode>\n");
          sb.append("<fradato>2012-08-01</fradato>\n");
          sb.append("<tildato/>\n");
          sb.append("</periode>\n");
          sb.append("<emne>\n");
          sb.append("<emneid>\n");
          sb.append("<institusjonsnr>217</institusjonsnr>\n");
          sb.append("<emnekode>BIE270</emnekode>\n");
          sb.append("<versjonskode>1</versjonskode>\n");
          sb.append("</emneid>\n");
          sb.append("<emnenavn>Datastrukturer og algoritmer</emnenavn>\n");
          sb.append("</emne>\n");
          sb.append("</redregel>\n");
          sb.append("</vektingsreduksjon>\n");
          sb.append("<arbeidsformer><![CDATA[Forelesinger i klasserom og veiledning på datalab. For å ta eksamen i kurset må følgende oppgaver være godkjente: oppgave 1, oppgave 2 eller oppgave 3, oppgave 4 eller oppgave 5. Siste frist for å få godkjent øvinger er tre uker før eksamen. ]]></arbeidsformer>\n");
          sb.append("<innhold><![CDATA[Effektivitetsanalyse. Definisjon, bruk og implementeringer av abstrakte datatyper som: Stabler, køer, lister, trestrukturer, grafer, prioritetskøer, hauger. Hash-teknikker. Bruk og implementering av datastrukturer som kan representere grafer. Sorterings teknikker. Rekursjon som programmeringsteknikk.]]></innhold>\n");
          sb.append("<litteratur><![CDATA[Data Structures and Problem Solving Using Java Mark Allen WeissPublished by Addision-Wesley]]></litteratur>\n");
          sb.append("<laringsutbytte><![CDATA[<p>Etter å ha tatt dette emnet skal studenten: </p><p>Kunnskap </p><ul><li>Kunne bruke standard datastrukturene til å lage effektive program. </li></ul><p> </p><p>Ferdigheter </p><ul><li>Være i stand til å beregne effektiviteten til algoritmer </li><li>Være i stand til å forstå og lage effektive rekursive algoritmer. </li><li>Være i stand til å implementere effektive algoritmer for sortering og søking. </li></ul><p> </p><p>Generell kompetanse </p><ul><li>Vite hvordan datastrukturer for lister, køer, stabler (stack), hauger (heap), binære tre, og grafer kan implementeres ved hjelp av Java.</li></ul><p> </p>]]></laringsutbytte>\n");
          sb.append("<studentevaluering><![CDATA[Skjer vanligvis gjennom skjema og/eller i samtaler etter til gjeldende retningslinjer.]]></studentevaluering>\n");
          sb.append("<apent_for_tillegg><![CDATA[<p>Bachelor- nivå på Det teknisk-naturvitenskaplige fakultetet. </p><p>Master nivå på Det teknisk-naturvitenskaplige fakultetet</p>]]></apent_for_tillegg>\n");
          sb.append("</emne>\n");
          sb.append("</fs-studieinfo>\n");
          return new StringReader(sb.toString());
        }
        
        @Override
        protected Reader fsGetKurs(int institution, String language) {
          return null;
        }

        @Override
        protected Reader fsGetStudieprogram(int institution, int faculty, int year, String semester, boolean includeEP,
            String language)
        {
          return null;
        }
      };
    }
    return this.studinfoImport;
  }
}
