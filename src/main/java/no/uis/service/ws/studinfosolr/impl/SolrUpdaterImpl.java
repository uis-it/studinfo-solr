package no.uis.service.ws.studinfosolr.impl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.datatype.XMLGregorianCalendar;

import no.uis.service.component.studinfopdf.convert.CollectionConverter;
import no.uis.service.component.studinfopdf.convert.InngarIStudieprogramConverter;
import no.uis.service.component.studinfopdf.convert.ObligoppgaveConverter;
import no.uis.service.component.studinfopdf.convert.StringConverter;
import no.uis.service.component.studinfopdf.convert.StringConverterUtil;
import no.uis.service.fsimport.StudInfoImport.StudinfoType;
import no.uis.service.fsimport.impl.AcceptAllEmne;
import no.uis.service.fsimport.impl.AcceptAllStudieprogram;
import no.uis.service.fsimport.impl.StudinfoFilter;
import no.uis.service.fsimport.util.PropertyInfo;
import no.uis.service.fsimport.util.PropertyInfoUtils;
import no.uis.service.fsimport.util.Studinfos;
import no.uis.service.studinfo.data.Emne;
import no.uis.service.studinfo.data.Emneid;
import no.uis.service.studinfo.data.Fagperson;
import no.uis.service.studinfo.data.FsSemester;
import no.uis.service.studinfo.data.FsYearSemester;
import no.uis.service.studinfo.data.InngarIStudieprogram;
import no.uis.service.studinfo.data.Kurs;
import no.uis.service.studinfo.data.Kursid;
import no.uis.service.studinfo.data.Kurskategori;
import no.uis.service.studinfo.data.Obligoppgave;
import no.uis.service.studinfo.data.Personnavn;
import no.uis.service.studinfo.data.Sprak;
import no.uis.service.studinfo.data.Sted;
import no.uis.service.studinfo.data.Studieprogram;
import no.uis.service.studinfo.data.Utdanningsplan;
import no.uis.service.ws.studinfosolr.SolrUpdater;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;

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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;

@ManagedResource(
  objectName="uis:service=ws-studinfo-solr,component=updater",
  description="SolrUpdater",
  log=false
)
public class SolrUpdaterImpl implements SolrUpdater {

  private static final int COMMIT_WITHIN = 3000;

  private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");

  private static final String CATEGORY_STUDINFO = "STUDINFO";

  private static final char ID_TOKEN_SEPARATOR = '_';
  
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SolrUpdaterImpl.class);
  
  private static final ToString SPRAK_TO_STRING = new ToString() {

    @Override
    public String toString(Object o) {
      if (o instanceof Sprak) {
        return ((Sprak)o).getSprakkode();
      }
      return null;
    }
  };

  private static final ToString INNGAR_I_STUDIEPROGRAM_TO_STRING = new ToString() {

    @Override
    public String toString(Object o) {
      if (o instanceof InngarIStudieprogram) {
        return ((InngarIStudieprogram)o).getStudieprogramkode();
      }
      return null;
    }
  };

  private static final ToString FAGPERSON_TO_STRING = new ToString() {

    @Override
    public String toString(Object o) {
      if (o instanceof Fagperson) {
        return ((Fagperson)o).getPersonid().toString();
      }
      return null;
    }
  };

  private Map<String, SolrServer> solrServers;
  
  private Map<String, String> solrFieldMapping;

  private DomainUrl cpUrl;
  private int siteId; 
  private XmlAccessorHolder xmlAccessorHolder;
  private int evuCategoryId = 5793;
  private int[] evuTemplateIds;

  private Gson gson;
  private Map<Class<?>, TypeAdapter<?>> typeAdapters;
  
  private StudinfoFilter<Emne> emneFilter = new AcceptAllEmne();
  private StudinfoFilter<Studieprogram> studieprogramFilter = new AcceptAllStudieprogram();

  private static ThreadLocal<CatalogContext> context = new ThreadLocal<CatalogContext>();
  
  @PostConstruct
  public void init() {
    GsonBuilder builder = new GsonBuilder();
    if (typeAdapters != null) {
      for (Map.Entry<Class<?>, TypeAdapter<?>> adapter : typeAdapters.entrySet()) {
        builder.registerTypeAdapter(adapter.getKey(), adapter.getValue());
      }
    }
    gson = builder.create();
    StringConverterUtil.registerConverter(new InngarIStudieprogramConverter(), InngarIStudieprogram.class);
    StringConverter collectionConverter = new CollectionConverter();
    StringConverterUtil.registerConverter(collectionConverter, List.class);
    StringConverterUtil.registerConverter(collectionConverter, Set.class);
    StringConverterUtil.registerConverter(new ObligoppgaveConverter(), Obligoppgave.class);
  }

  public void setSolrFieldMapping(Map<String, String> mapping) {
    this.solrFieldMapping = mapping;
  }

  public void setTypeAdapters(Map<Class<?>, TypeAdapter<?>> typeAdapters) {
    this.typeAdapters = typeAdapters;
  }

  public void setEmneFilter(StudinfoFilter<Emne> emneFilter) {
    this.emneFilter = emneFilter;
  }

  public void setStudieprogramFilter(StudinfoFilter<Studieprogram> studieprogramFilter) {
    this.studieprogramFilter = studieprogramFilter;
  }

  
  @Override
  public void pushCourses(List<Kurs> courses, int year, FsSemester semester, String language) throws Exception {
    context.set(new CatalogContext(new FsYearSemester(year, semester)));
    try {
      Map<String, CPArticleInfo> descriptionCache = new HashMap<String, CPArticleInfo>();
      fillDescriptionCache(descriptionCache);
      courseDescriptionCache.set(descriptionCache);
      try {
        for (Kurs kurs : courses) {
          pushKursToSolr(kurs);
        }
      } finally {
        courseDescriptionCache.remove();
      }
    } finally {
      context.remove();
    }
  }

  @Override
  public void pushSubjects(List<Emne> subjects, int year, FsSemester semester, String language) throws Exception {
    context.set(new CatalogContext(new FsYearSemester(year, semester)));
    HashMap<Integer, Personnavn> fagpersons = new HashMap<Integer, Personnavn>();
    try {
      for (Emne emne : subjects) {
        if (emneFilter.accept(emne)) {
          pushEmneToSolr(emne);
          addFagPersonsToMap(fagpersons, emne);
        } else {
          String emneid = formatTokens(emne.getEmneid().getEmnekode(), emne.getEmneid().getVersjonskode());
          log.info(String.format("Skipping \"%s\" due to filter %s", emneid, emneFilter.getClass().getName()));
        }
      }
      pushFagpersonsToSolr(language, fagpersons);
    } finally {
      context.remove();
    }
  }

  @Override
  public void pushPrograms(List<Studieprogram> programs, int year, FsSemester semester, String language) throws Exception {
    context.set(new CatalogContext(new FsYearSemester(year, semester)));
    
    try {
      for (Studieprogram program : programs) {
        if (studieprogramFilter.accept(program)) {
          pushProgramToSolr(program);
        } else {
          log.info(String.format("Skipping \"%s\" due to filter %s", program.getStudieprogramkode(), studieprogramFilter.getClass().getName()));
        }
      }
    } finally {
      context.remove();
    }
  }

  @Override
  @ManagedOperation(description="Delete documents of solr index (given by language) by solr query")
  @ManagedOperationParameters({
    @ManagedOperationParameter(name="language", description="one-letter language code: (B)okmål, (E)ngelsk or (N)ynorsk"),
    @ManagedOperationParameter(name="query", description="Solr query for documents to delete")
  })
  public void deleteByQuery(String language, String query) throws Exception {
    SolrServer solrServer = getSolrServer(language);
    solrServer.deleteByQuery(query, COMMIT_WITHIN);
  }
  
  private void pushProgramToSolr(Studieprogram prog) throws Exception {
    
    // clean utdanningsplan
    if (prog.isSetUtdanningsplan()) {
      Studinfos.cleanUtdanningsplan(prog.getUtdanningsplan(), prog.getStudieprogramkode(), context.get().getStartYearSemester(), Studinfos.numberOfSemesters(prog));
    }

    Map<String, Object> beanmap = getBeanMap(prog, "/studieprogram");
    updateDocuments(StudinfoType.STUDIEPROGRAM, prog.getSprak(), beanmap, null, "/studieprogram");
  }

  private void addFagPersonsToMap(HashMap<Integer, Personnavn> fagpersons, Emne emne) {
    for (Fagperson fagPerson : emne.getFagpersonListe()) {
      fagpersons.put(fagPerson.getPersonid().intValue(), fagPerson.getPersonnavn());
    }
  }
  
  private void pushFagpersonsToSolr(String language, HashMap<Integer, Personnavn> fagpersons) throws Exception {
    String fPersonId = getSolrFieldName("/fagperson", "personid");
    String fFornavn = getSolrFieldName("/fagperson", "fornavn");
    String fEtternavn = getSolrFieldName("/fagperson", "etternavn");
    for (Entry<Integer, Personnavn> entry : fagpersons.entrySet()) {
      SolrInputDocument doc = new SolrInputDocument();
      doc.addField("id", "fagperson_"+entry.getKey());
      doc.addField("cat", "fagperson");
      addFieldToDoc(doc, entry.getKey(), fPersonId);
      addFieldToDoc(doc, entry.getValue().getFornavn(), fFornavn);
      addFieldToDoc(doc, entry.getValue().getEtternavn(), fEtternavn);
      updateDocument(language, doc);
    }
  }
  
  private void updateDocuments(StudinfoType infoType, String infoLanguage, Map<String, ?> beanProps, String parentDocId, String path) throws Exception {
    SolrInputDocument doc = new SolrInputDocument();
    String docId = createId(infoType, beanProps, path);
    doc.addField("id", docId);
    addCategories(doc, infoType);
    addFieldToDoc(doc, context.get().getCurrentYearSemester().getYear(), getSolrFieldName(path, "year"));
    addFieldToDoc(doc, context.get().getCurrentYearSemester().getSemester().toString(), getSolrFieldName(path, "semester"));
    for (Entry<String, ?> entry : beanProps.entrySet()) {
      String propName = getSolrFieldName(path, entry.getKey());
      if (propName != null) {
        addFieldToDoc(doc, entry.getValue(), propName);
      }
    }
    updateDocument(infoLanguage, doc);
  }

  private void addFieldToDoc(SolrInputDocument doc, Object value, String solrFieldName) {
    if (value instanceof Collection) {
      Collection<?> coll = (Collection<?>)value;
      for (Object elem : coll) {
        addFieldToDoc(doc, elem, solrFieldName);
      }
    } else {
      String stringValue = String.valueOf(value);
      
      doc.addField(solrFieldName, stringValue);
    }
  }

  private String getSolrFieldName(String path, String propName) {
    String solrFieldName = solrFieldMapping.get(path + '/' + propName);
    if (solrFieldName == null) {
      solrFieldName = solrFieldMapping.get(propName);
      if (solrFieldName == null) {
        solrFieldName = propName + "_s";
      }
    }
    if (solrFieldName.isEmpty()) {
      solrFieldName = null;
    }
    return solrFieldName;
  }

  private Map<String, Object> getBeanMap(Object fsType, String path) {
    Class<?> klass = fsType.getClass();
    List<PropertyInfo> pinfos = PropertyInfoUtils.getPropertyInfos(klass);
    Map<String, Object> map = new HashMap<String, Object>();
    for (PropertyInfo pi : pinfos) {
      String name = pi.getPropName();
      Object value = getValue(fsType, pi);
      if (value != null) {
        String newPath = path + "/" + name;
        addValue(map, name, value, newPath);
      }
    }
    return map;
  }

  private String createId(StudinfoType infoType, Map<String, ?> beanmap, String path) {
    switch(infoType) {
      case STUDIEPROGRAM:
        if (path.equals("/studieprogram")) {
          return formatTokens(infoType, beanmap.get("studieprogramkode"), context.get().getCurrentYearSemester().getYear(), context.get().getCurrentYearSemester().getSemester());
        }
        break;
      case EMNE:
        if (path.equals("/emne")) {
          return formatTokens(infoType, beanmap.get("emneid"), context.get().getCurrentYearSemester().getYear(), context.get().getCurrentYearSemester().getSemester());
        }
        break;
      case KURS:
        if (path.equals("/kurs")) {
          return formatTokens(infoType, beanmap.get("kurskode"), beanmap.get("tidkode"));
        }
        break;
        
    }
    return UUID.randomUUID().toString();
  }

  private void pushKursToSolr(Kurs kurs) throws Exception {
    Map<String, Object> beanmap = getBeanMap(kurs, "/kurs");
    
    String kurskode = formatTokens(beanmap.get("kurskode"), beanmap.get("tidkode"));
    beanmap.put("kursid", kurskode);
    CPArticleInfo articleInfo = courseDescriptionCache.get().get(kurskode);
    if (articleInfo != null) {
      beanmap.put("cp_article_id", articleInfo.getArticleId());
      beanmap.put("cp_category_id", articleInfo.getCategoryId());
      beanmap.put("cp_text", articleInfo.getText());
    }
    updateDocuments(StudinfoType.KURS, kurs.getSprak(), beanmap, null, "/kurs");
  }

  private void pushEmneToSolr(Emne emne) throws Exception {
    
    // vurdering
    Studinfos.cleanVurderingsordning(emne, context.get().getStartYearSemester());
    
    // forkunnskaper
    Map<String, Object> forkunnskap = Studinfos.forkunnskap(emne);
    if (forkunnskap != null) {
      emne.addProperty("forkunnskapskrav", forkunnskap);
    }
    
    // anbefalte forkunnskaper
    Map<String, Object> anbForkunn = Studinfos.anbefalteForkunnskaper(emne);
    if (anbForkunn != null) {
      emne.addProperty("anbefalteForkunnskaper", anbForkunn);
    }
    
    // obligatorisk undervisning
    Map<String, Object> obligund = Studinfos.obligund(emne);
    if (obligund != null) {
      emne.addProperty("obligund", obligund);
    }
    
    // apent for
    Map<String, Object> apenFor = Studinfos.apenFor(emne);
    if (apenFor != null) {
      emne.addProperty("apenFor", apenFor);
    }
    
    Map<String, Object> beanmap = getBeanMap(emne, "/emne");
    if (!beanmap.containsKey("kortsam")) {
      
      Object kortsam = beanmap.get("intro");
      if (kortsam == null || kortsam.toString().isEmpty()) {
        kortsam = beanmap.get("innhold");
      }
      if (kortsam != null) {
        beanmap.put("kortsam", kortsam);
      }
    }
    beanmap.put("emneidKode", emne.getEmneid().getEmnekode());
    beanmap.put("emneidVersion", emne.getEmneid().getVersjonskode());
    
    updateDocuments(StudinfoType.EMNE, emne.getSprak(), beanmap, null, "/emne");
  }

  private void updateDocument(String lang, SolrInputDocument doc) throws SolrServerException, IOException {
    SolrServer solrServer = getSolrServer(lang);
    doc.addField("timestamp", solrDate(Calendar.getInstance(TIME_ZONE_UTC)));
    solrServer.add(doc, COMMIT_WITHIN);
  }

  private SolrServer getSolrServer(String sprak) {

    return solrServers.get(sprak.substring(0, 1));
  }

  private static void addCategories(SolrInputDocument doc, StudinfoType infoType) {
    doc.addField("cat", CATEGORY_STUDINFO);
    doc.addField("cat", infoType.toString());
  }

  private void fillDescriptionCache(Map<String, CPArticleInfo> descriptionCache) {
    Accessor cpAccessor = null;
    if (cpUrl != null) {
      try {
        cpAccessor = xmlAccessorHolder.getAccessor(cpUrl);
      } catch(Exception ex) {
        log.info("Corepublish", ex);
      }
    }
    if (cpAccessor == null) {
      log.warn("CorePublish access disabled");
      return;
    }
    ArticleQuery articleQuery = new DefaultArticleQuery().includeCategoryId(evuCategoryId, true).includeTemplateIds(evuTemplateIds);
    articleQuery.setSiteList(this.siteId);
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
          if (pName.equalsIgnoreCase("pluss_course_code")) {
            kursKode = getStringValue(articleElement, cpAccessor);
          } else if (pName.equalsIgnoreCase("pluss_time_code")) {
            tidKode = getStringValue(articleElement, cpAccessor);
          } else if (pName.equalsIgnoreCase("maintext")) {
            mainText = getStringValue(articleElement, cpAccessor);
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
    switch (aeType)
      {
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

  public void setSolrServers(Map<String, SolrServer> solrServers) {
    this.solrServers = solrServers;
  }

  @ManagedOperation(description="get Server Map as String")
  public String getSolrServersAsString() {
    StringBuilder sb = new StringBuilder();
    
    for (Entry<String, SolrServer> entry : solrServers.entrySet()) {
      sb.append(entry.getKey());
      sb.append(" = ");
      SolrServer server = entry.getValue();
      if (server instanceof HttpSolrServer) {
        sb.append(((HttpSolrServer)server).getBaseURL());
      } else {
        sb.append(server.toString());
      }
      sb.append("\n");
    }
    return sb.toString();
  }
  
  @ManagedOperation(description="set the Solr server for a specific language. If the URL is null or empty, the solrserver for the given language is removed")
  @ManagedOperationParameters({
    @ManagedOperationParameter(name="lang", description="language code: B, E or N"),
    @ManagedOperationParameter(name="url", description="URL to the solr core"),
    @ManagedOperationParameter(name="username", description="optional username for the Solr core"),
    @ManagedOperationParameter(name="password", description="optional password for the Solr core")
  })
  public void assignSolrServer(String lang, String url, String username, String password) throws MalformedURLException {
    if (url == null || url.trim().isEmpty()) {
      solrServers.remove(lang);
    } else {
      SolrServerFactory serverFactory = new SolrServerFactory(); 
      serverFactory.setUrl(new URL(url));
      if (username != null && !username.trim().isEmpty()) {
        serverFactory.setUsername(username);
      }
      if (password != null && !password.trim().isEmpty()) {
        serverFactory.setPassword(password);
      }
      solrServers.put(lang, serverFactory.getObject());
    }
  }
  
  @ManagedOperation(description="Set Cp Url")
  @ManagedOperationParameters({
    @ManagedOperationParameter(name="cpUrl", description="Corepublish URL: <url>|domainId")
  })
  public void setCpUrl(String cpUrl) {
    this.cpUrl = DomainUrlAdapter.valueOf(cpUrl);
  }

  @ManagedOperation(description="Get the corepublish URL")
  public String getCpUrl() {
    return this.cpUrl.toString();
  }
  
  @ManagedOperation(description="Set siteId")
  public void setSiteId(int siteId) {
    this.siteId = siteId;
  }
  
  @ManagedOperation(description="Get siteId")
  public int getSiteId() {
    return this.siteId;
  }
  
  public void setCpAccessorHolder(XmlAccessorHolder holder) {
    this.xmlAccessorHolder = holder;
  }

  @ManagedOperation(description="Set Course category root Id")
  @ManagedOperationParameters({
    @ManagedOperationParameter(name="evuCategoryId", description="Category Id of EVU Courses Root")
  })
  public void setEvuCategoryId(int evuCategoryId) {
    this.evuCategoryId = evuCategoryId;
  }

  @ManagedOperation(description="Get Course category root Id")
  public int getEvuCategoryId() {
    return evuCategoryId;
  }

  private synchronized int[] _getEvuTemplateIds() {
    return evuTemplateIds;
  }

  private synchronized void _setEvuTemplateIds(int[] ids) {
    this.evuTemplateIds = ids;
  }

  @ManagedOperation(description="Set Course Article Template IDs")
  @ManagedOperationParameters({
    @ManagedOperationParameter(name="csvString", description="comma-separated list of article template ids")
  })
  public void setEvuTemplateIds(String csvString) {
    String[] strArray = csvString.split("\\s*,\\s*");
    int[] ids = new int[strArray.length];
    int i = 0;
    for (String s : strArray) {
      ids[i++] = Integer.parseInt(s);
    }
    _setEvuTemplateIds(ids);
  }

  @ManagedOperation(description="Get Course Article Template IDs")
  public String getEvuTemplateIds() {
    int[] ids = _getEvuTemplateIds();
    if (ids == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (int i : ids) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(i);
    }
    return sb.toString();
  }
  
  private void addValue(Map<String, Object> map, String propName, Object value, String path) {
    if (value == null) {
      return;
    }
    if (value instanceof String) {
      map.put(propName, value);
    
    } else if (value.getClass().isPrimitive()) {
      map.put(propName, value);
    
    } else if (value instanceof Number) {
      map.put(propName, value);
    
    } else if(value instanceof Utdanningsplan) {
      Utdanningsplan uplan = (Utdanningsplan)value;

      if (uplan.isSetPlaninformasjon()) {
        uplan.unsetPlaninformasjon();
      }
      map.put(propName, gson.toJson(value));
      
    } else if (value instanceof Emneid) {
      Emneid emneid = (Emneid)value;
      map.put(propName, formatTokens(emneid.getInstitusjonsnr(), emneid.getEmnekode(), emneid.getVersjonskode()));
      if (path.equals("/emne/emneid")) {
        map.put("emnekode", formatTokens(emneid.getEmnekode(), emneid.getVersjonskode()));
      }
      
    } else if (value instanceof Kursid) {
      Kursid kursid = (Kursid)value;
      map.put("kurskode", kursid.getKurskode());
      map.put("tidkode", kursid.getTidkode());
    
    } else if (propName.equals("kurskategoriListe")) {
      @SuppressWarnings("unchecked")
      List<Kurskategori> kkList = (List<Kurskategori>)value;
      List<String> kkkList = new ArrayList<String>(kkList.size());
      for (Kurskategori kk : kkList) {
        kkkList.add(kk.getKurskategorikode());
      }
      map.put(propName, kkkList);
      
    } else if (propName.equals("fagpersonListe")) {
      map.put(propName, createJsonArray(value));
      
      map.put(propName+"_", createStringArray(value, new HashSet<String>(), FAGPERSON_TO_STRING));
      
    } else if (value instanceof Sted) { // fagansvarlig, adminansvarlig
      map.put(propName, gson.toJson(value));
      addStedValue(map, propName+"_", (Sted)value);
    
    } else if(propName.equals("inngarIStudieprogram")) {
      map.put(propName, gson.toJson(value));
      
      map.put(propName+"_", createStringArray(value, null, INNGAR_I_STUDIEPROGRAM_TO_STRING));
    
    } else if (propName.equals("sprakListe")) {
      map.put("sprakListe", gson.toJson(value));
      Collection<String> usprak = createStringArray(value, new HashSet<String>(), SPRAK_TO_STRING);

      if (usprak.isEmpty()) {
        usprak.add("NO");
      }
      map.put(propName+"_", usprak);
      
    } else if(propName.equals("inngarIFag")) {
      map.put(propName, createStringArray(value, null, null));
    
    } else if (path.startsWith("/kurs/dato")) {
      map.put(propName, xmlCalToSolrDateString((XMLGregorianCalendar)value));
      
    } else {
      map.put(propName, gson.toJson(value));
    }
  }

  private void addStedValue(Map<String, Object> map, String prefix, Sted sted) {
    map.put(prefix+"institusjonsnr", sted.getInstitusjonsnr());
    map.put(prefix+"fakultetsnr", sted.getFakultetsnr());
    map.put(prefix+"instituttnr", sted.getInstituttnr());
    map.put(prefix+"gruppenr", sted.getGruppenr());
  }

  private String xmlCalToSolrDateString(XMLGregorianCalendar value) {
    return solrDate(value.toGregorianCalendar(TIME_ZONE_UTC, null, null));
  }

  private String solrDate(Calendar cal) {
    StringBuilder out = new StringBuilder();
    try {
      DateUtil.formatDate(cal.getTime(), cal, out);
    } catch(IOException e) {
      log.warn("xml cal: " + cal.getTime());
    }
    
    return out.toString();
  }
  
  private Collection<String> createStringArray(Object value, Collection<String> target, ToString ts) {
    if (!(value instanceof Collection)) {
      return Collections.emptyList();
    }
    Collection<?> coll = (Collection<?>)value;
    if (target == null) {
      target = new ArrayList<String>(coll.size());
    }
    for (Object o : coll) {
      String sval = ts == null ? String.valueOf(o) : ts.toString(o);
      if (sval != null) {
        target.add(sval);
      }
    }
    return target;
  }
  
  private List<String> createJsonArray(Object value) {
    Collection<?> coll = (Collection<?>)value;
    List<String> jsonArray = new ArrayList<String>(coll.size());
    for (Object o : coll) {
      jsonArray.add(gson.toJson(o));
    }
    return jsonArray;
  }

  private static Object getValue(Object fsType, PropertyInfo pi) {
    try {
      Method mIsSet = pi.getIsSet();
      if (mIsSet != null) {
        if ((boolean)mIsSet.invoke(fsType) == false) {
          return null;
        }
      }
      
      Method mGet = pi.getGet();
      if (mGet != null && mGet.getParameterTypes().length == 0) {
        return mGet.invoke(fsType);
      }
    } catch(Exception ex) {
      log.error("get " + pi.getPropName(), ex);
    }
    return null;
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

  private String formatTokens(Object... tokens) {
    StringBuilder sb = new StringBuilder();

    for (Object o : tokens) {
      if (sb.length() > 0) {
        sb.append(ID_TOKEN_SEPARATOR);
      }
      sb.append(o);
    }
    return sb.toString();
  }

  // TODO move to studinfo-util
  protected static class CatalogContext {

    private final FsYearSemester currentSemester;
    
    /**
     * start semester for the current semester.  
     */
    private FsYearSemester startSemester;

    public CatalogContext(FsYearSemester currentSemester) {
      this.currentSemester = currentSemester;
      this.startSemester = Studinfos.getStartYearSemester(currentSemester);
    }

    public FsYearSemester getCurrentYearSemester() {
      return currentSemester;
    }
    
    public FsYearSemester getStartYearSemester() {
      return startSemester;
    }
  }

  @PreDestroy
  public void destroy() throws Exception {
    xmlAccessorHolder.destroy();
  }

  private static class DomainUrlAdapter extends DomainUrl {

    private static final long serialVersionUID = 1L;

    public DomainUrlAdapter(String cpUrl, int domainId) {
      super(cpUrl, domainId);
    }
    
    private static DomainUrlAdapter valueOf(String domainUrl) {
      String[] urlParts = domainUrl.split("\\|");
      return new DomainUrlAdapter(urlParts[0], Integer.parseInt(urlParts[1]));
    }
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getUrl());
      sb.append('|');
      sb.append(this.getDomainId());
      return sb.toString();
    }
  }

  private interface ToString {
    String toString(Object o);
  }
  
}
