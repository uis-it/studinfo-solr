package no.uis.service.component.studinfosolr.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import no.uis.service.component.studinfosolr.SolrUpdater;
import no.uis.service.fsimport.StudInfoImport;
import no.uis.service.fsimport.StudInfoImport.StudinfoType;
import no.uis.service.studinfo.data.Emne;
import no.uis.service.studinfo.data.FsSemester;
import no.uis.service.studinfo.data.FsStudieinfo;
import no.uis.service.studinfo.data.FsType;
import no.uis.service.studinfo.data.Kurs;
import no.uis.service.studinfo.data.Kurskategori;
import no.uis.service.studinfo.data.Sted;
import no.uis.service.studinfo.data.Studieprogram;
import no.uis.service.studinfo.data.YESNOType;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import com.corepublish.api.Accessor;
import com.corepublish.api.Article;
import com.corepublish.api.ArticleQuery;
import com.corepublish.api.ArticleQueryResult;
import com.corepublish.api.article.element.ArticleElement;
import com.corepublish.api.article.element.ArticleElementType;
import com.corepublish.api.article.element.NewTextElement;
import com.corepublish.api.article.element.ProgramaticElement;
import com.corepublish.api.article.element.TextElement;
import com.corepublish.api.article.richtext.HtmlFragmentToken;
import com.corepublish.api.article.richtext.Token;
import com.corepublish.impl.defaultt.DefaultArticleQuery;
import com.corepublish.impl.xml.XmlAccessorHolder;
import com.corepublish.util.DomainUrl;

public class SolrUpdaterImpl implements SolrUpdater {

  private static final String CATEGORY_STUDINFO = "studinfo";

  private static final String ID_TOKEN_SEPARATOR = "_";

  static private org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SolrUpdaterImpl.class);

  private Map<String, SolrServer> solrServers;

  @Override
  public void pushStudieInfo(FsStudieinfo info, int year, FsSemester semester, String language) throws Exception {
    pushPrograms(info.getStudieprogram(), language);
    pushCourses(info.getKurs(), language);
    pushSubjects(info.getEmne(), language);
  }
  
  private void pushPrograms(List<Studieprogram> programs, String language) throws Exception {
    for (Studieprogram program : programs) {
      pushStudieprogramToSolr(program);
    }
  }

  private void pushSubjects(List<Emne> subjects, String language) throws Exception {
    for (Emne emne : subjects) {
      pushEmneToSolr(emne);
    }
  }

  private void pushCourses(List<Kurs> courses, String language) throws Exception {

    
    Map<String, CPArticleInfo> descriptionCache = new HashMap<String, CPArticleInfo>();
    fillDescriptionCache(descriptionCache);
    courseDescriptionCache.set(descriptionCache);
    try {
      
      cleanIndex(StudinfoType.KURS, courses);
      for (Kurs kurs : courses) {
        pushKursToSolr(kurs);
      }
    } finally {
      courseDescriptionCache.remove();
    }
  }

  private void cleanIndex(StudinfoType studinfoType, List<? extends FsType> infos) throws SolrServerException, IOException {
    Set<String> languages = new HashSet<String>();

    for (FsType type : infos) {
      languages.add(type.getSprak());
    }
    for (String lang : languages) {
      SolrServer solrServer = getSolrServer(lang, studinfoType);
      solrServer.deleteByQuery("*:*");
    }
  }

  private void pushStudieprogramToSolr(Studieprogram prog) {
    //SolrInputDocument doc = new SolrInputDocument();
    
  }

  private void pushKursToSolr(Kurs kurs) throws SolrServerException, IOException {
    SolrInputDocument doc = new SolrInputDocument();

    String courseId = kurs.getKursid().getKurskode() + ID_TOKEN_SEPARATOR + kurs.getKursid().getTidkode();
    doc.addField("id", "kurs" + ID_TOKEN_SEPARATOR + courseId);
    
    addCategories(doc, StudinfoType.KURS);

    doc.addField("name", kurs.getKursnavn());
    for (Kurskategori kursKat : kurs.getKurskategoriListe()) {
      doc.addField("course_category_code", kursKat.getKurskategorikode());
    }

    doc.addField("course_code_s", kurs.getKursid().getKurskode());
    doc.addField("course_time_code_s", kurs.getKursid().getTidkode());
    String date = dateToSolrString(kurs.getDatoFristSoknad());
    if (date != null) {
      doc.addField("application_deadline_dt", date);
    }
    date = dateToSolrString(kurs.getDatoPubliseresFra());
    if (date != null) {
      doc.addField("publish_from_dt", date);
    }
    date = dateToSolrString(kurs.getDatoPubliseresTil());
    if (date != null) {
      doc.addField("publish_to_dt", date);
    }
    doc.addField("course_contact_s", kurs.getEmail());
    
    CPArticleInfo cpinfo = courseDescriptionCache.get().get(courseId);
    if (cpinfo != null) {
      doc.addField("cp_article_id_l", cpinfo.getArticleId());
      doc.addField("cp_category_id_l", cpinfo.getCategoryId());
      doc.addField("text", cpinfo.getText());
    }

    getSolrServer(kurs.getSprak(), StudinfoType.KURS).add(doc, 3000);
  }

  private void pushEmneToSolr(Emne emne) throws SolrServerException, IOException {
    SolrInputDocument doc = new SolrInputDocument();
    
    String emneId = formatTokens(emne.getEmneid().getInstitusjonsnr(), emne.getEmneid().getEmnekode(), emne.getEmneid().getVersjonskode());
    doc.addField("id", formatTokens("emne", emneId));
    
    addCategories(doc, StudinfoType.EMNE);
    
    String adminAnsvarlig = null;
    String fagAnsvarlig = null;
    for (Sted sted : emne.getSted()) {
      if (sted.getType().equals("adminansvarlig")) {
        adminAnsvarlig = getStedCode(sted);
      } else if (sted.getType().equals("fagansvarlig")) {
        fagAnsvarlig = getStedCode(sted);
      }
    }
    if (adminAnsvarlig != null) {
      doc.addField("adminansvarlig_s", adminAnsvarlig);
    }
    if (fagAnsvarlig != null) {
      doc.addField("fagansvarlig_s", fagAnsvarlig);
    }
    doc.addField("emnenavn_s", emne.getEmnenavn());
    
    doc.addField("antall-undsemester_i", emne.getAntallUndsemester());
    doc.addField("eksamensemester_s", emne.getEksamenssemester());
    doc.addField("inngar-i-studieprogram_ms", emne.getInngarIStudieprogram());
    doc.addField("inngar-i-fag_ms", emne.getInngarIFag());
    doc.addField("nuskode_s", emne.getNuskode());
    doc.addField("periode-eks-start_s", emne.getPeriodeEks().getForstegang());
    doc.addField("periode-eks-end_s", emne.getPeriodeEks().getSistegang());
    doc.addField("periode-und-start_s", emne.getPeriodeUnd().getForstegang());
    doc.addField("periode-und-end_s", emne.getPeriodeUnd().getSistegang());
    doc.addField("sprak_s", emne.getSprak());
    doc.addField("status-oblig_b", isTrue(emne.getStatusOblig()));
    doc.addField("status-privatist_b", isTrue(emne.getStatusPrivatist()));
    doc.addField("studieniva_s", emne.getStudieniva());
    doc.addField("studiepoeng_i", emne.getStudiepoeng());
    doc.addField("undervisningssemester_s", emne.getUndervisningssemester());
    doc.addField("emnetype_s", emne.getEmnetype());
    //doc.addField(", value)

    SolrServer solrServer = getSolrServer(emne.getSprak(), StudInfoImport.StudinfoType.EMNE);
    solrServer.add(doc, 3000);
  }

  private SolrServer getSolrServer(String sprak, StudinfoType studinfoType) {
    
    return solrServers.get(formatTokens(studinfoType.name(), sprak));
  }

  private Boolean isTrue(YESNOType yn) {
    return (yn != null && yn.equals(YESNOType.J) ? Boolean.TRUE: Boolean.FALSE);
  }
  
  private String getStedCode(Sted sted) {
    
    return formatTokens(sted.getInstitusjonsnr().intValue(), sted.getFakultetsnr().intValue(), sted.getInstituttnr().intValue(), sted.getGruppenr().intValue());
    
  }

  private String formatTokens(Object... tokens) {
    StringBuilder sb = new StringBuilder();
    
    for (Object o : tokens) {
      if (sb.length() == 0) {
        sb.append(ID_TOKEN_SEPARATOR);
      }
      sb.append(tokens);
    }
    return sb.toString();
  }
  
  private static void addCategories(SolrInputDocument doc, StudinfoType infoType) {
    doc.addField("cat", CATEGORY_STUDINFO);
    doc.addField("cat", infoType.toString());
  }
  
  private static String dateToSolrString(XMLGregorianCalendar xcal) {
    if (xcal != null) {
      return String.format("%d-%02d-%02dT%02d:%02d:%02dZ", xcal.getYear(), xcal.getMonth(), xcal.getDay(), xcal.getHour(), xcal.getMinute(), xcal.getSecond());
    }
    return null;
  }

  private void fillDescriptionCache(Map<String, CPArticleInfo> descriptionCache) {
    Accessor cpAccessor = null;
    try {
      cpAccessor = xmlAccessorHolder.getAccessor(cpUrl);
    } catch(Exception ex) {
      log.warn("CorePublish access disabled", ex);
      return;
    }
    if (cpAccessor == null) {
      log.warn("CorePublish access disabled");
      return;
    }
    ArticleQuery articleQuery = new DefaultArticleQuery();
    articleQuery.includeCategoryId(evuCategoryId, true);
    articleQuery.includeTemplateIds(10, 11); // EVU article and EVU category article template
    List<ArticleQueryResult> aqr = cpAccessor.getArticleQueryResult(articleQuery);
    
    List<Integer> articleIds = cpAccessor.getArticleIds(aqr);
  
    for (Integer articleId : articleIds) {
      
      try {
        Article article = cpAccessor.getArticle(articleId.intValue());
        List<ArticleElement> elements = article.getArticleElements().getElements();
        String kursKode = null;
        String tidKode = null;
        String mainText = "";
        for (ArticleElement articleElement : elements) {
          String pName = articleElement.getProgrammaticName();
          String value = getStringValue(articleElement, cpAccessor);
          if (pName.equalsIgnoreCase("kurskode")) {
            kursKode = value;
          } else if (pName.equalsIgnoreCase("tidskode")) {
            tidKode = value;
          } else if (pName.equalsIgnoreCase("maintext")) {
            mainText = value;
          }
        }
        if (kursKode != null && tidKode != null) {
          CPArticleInfo cpinfo = new CPArticleInfo(formatTokens(kursKode, tidKode));
          cpinfo.setText(mainText);
          cpinfo.setArticleId(article.getId());
          cpinfo.setCategoryId(article.getMainCategoryId());
          descriptionCache.put(cpinfo.getId(), cpinfo);
        }
      } catch(Exception ex) {
        log.warn("Problem with article " + articleId, ex);
      }
    }
  }

  private String getStringValue(ArticleElement ae, Accessor cpAccessor) {
    ArticleElementType aeType = ae.getArticleElementType();
    
    String value;
    switch(aeType) {
      case PROGRAMMATIC:
        ProgramaticElement progElem = (ProgramaticElement)ae;
        value = cpAccessor.getProgrammaticElementObjectValue(progElem);
        break;
        
      case TEXT:
        TextElement txt = (TextElement)ae;
        value = txt.getText();
        break;
        
      case NEWTEXT:
        NewTextElement ntxt = (NewTextElement)ae;
        List<Token> tokens = ntxt.getContent().getTokens();
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
          if (token instanceof HtmlFragmentToken) {
            HtmlFragmentToken html = (HtmlFragmentToken)token;
            sb.append(html.getHtml());
            sb.append(' ');
          }
        }
        value = sb.toString();
        break;
        
      default:
        value = "";
        break;
    }
    return value;
  }

  private ThreadLocal<Map<String, CPArticleInfo>> courseDescriptionCache = new ThreadLocal<Map<String, CPArticleInfo>>() {
    @Override
    protected Map<String, CPArticleInfo> initialValue() {
      return Collections.emptyMap();
    }
  };
  
  private DomainUrl cpUrl;
  private XmlAccessorHolder xmlAccessorHolder;
  private int evuCategoryId = 5793;

  public void setSolrServers(Map<String, SolrServer> solrServers) {
    this.solrServers = solrServers;
  }
  
  public void setCpUrl(DomainUrl cpUrl) {
    this.cpUrl = cpUrl; 
  }
  
  public void setCpAccessorHolder(XmlAccessorHolder holder) {
    this.xmlAccessorHolder = holder;
  }
  
  public void setEvuCategoryId(int evuCategoryId) {
    this.evuCategoryId = evuCategoryId;
  }

  
  private static class CPArticleInfo {
    private final String id;
    private String text;
    private int articleId;
    private int categoryId; 
    
    public CPArticleInfo(String cacheId) {
      this.id = cacheId;
    }

    public void setText(String mainText) {
      this.text = mainText;
    }

    public void setArticleId(int id) {
      this.articleId = id;
    }

    public void setCategoryId(int evuCategoryId) {
      this.categoryId = evuCategoryId;
    }

    public String getId() {
      return id;
    }

    public String getText() {
      return text;
    }

    public int getArticleId() {
      return articleId;
    }

    public int getCategoryId() {
      return categoryId;
    }
  }
}
