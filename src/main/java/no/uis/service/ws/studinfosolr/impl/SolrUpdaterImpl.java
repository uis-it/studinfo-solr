package no.uis.service.ws.studinfosolr.impl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
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
import no.uis.service.studinfo.data.FsSemester;
import no.uis.service.studinfo.data.FsStudieinfo;
import no.uis.service.studinfo.data.FsYearSemester;
import no.uis.service.studinfo.data.InngarIStudieprogram;
import no.uis.service.studinfo.data.Kurs;
import no.uis.service.studinfo.data.Kursid;
import no.uis.service.studinfo.data.Kurskategori;
import no.uis.service.studinfo.data.Obligoppgave;
import no.uis.service.studinfo.data.Studieprogram;
import no.uis.service.studinfo.data.Utdanningsplan;
import no.uis.service.ws.studinfosolr.SolrUpdater;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;

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

public class SolrUpdaterImpl implements SolrUpdater {

  private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");

  private static final String CATEGORY_STUDINFO = "STUDINFO";

  private static final char ID_TOKEN_SEPARATOR = '_';
  
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SolrUpdaterImpl.class);

  private Map<String, SolrServer> solrServers;
  
  private Map<String, String> solrFieldMapping;

  private boolean purgeIndexBeforeUpdate;
  
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
  public void pushStudieInfo(FsStudieinfo info, int year, FsSemester semester, String language) throws Exception {
    
    context.set(new CatalogContext(new FsYearSemester(year, semester)));
    
    try {
      if (info.isSetStudieprogram()) {
        if (purgeIndexBeforeUpdate) {
          cleanIndex(StudinfoType.STUDIEPROGRAM, language);
        }
        pushPrograms(info.getStudieprogram(), language);
      }
      if(info.isSetEmne()) {
        if (purgeIndexBeforeUpdate) {
          cleanIndex(StudinfoType.EMNE, language);
        }
        pushSubjects(info.getEmne(), language);
      }
      if (info.isSetKurs()) {
        if (purgeIndexBeforeUpdate) {
          cleanIndex(StudinfoType.KURS, language);
        }
        pushCourses(info.getKurs(), language);
      }
    } finally {
      context.remove();
    }
  }

  private void pushPrograms(List<Studieprogram> programs, String language) throws Exception {
    for (Studieprogram program : programs) {
      if (studieprogramFilter.accept(program)) {
        pushProgramToSolr(program);
      } else {
        log.info(String.format("Skipping \"%s\" due to filter %s", program.getStudieprogramkode(), studieprogramFilter.getClass().getName()));
      }
    }
  }

  private void pushSubjects(List<Emne> subjects, String language) throws Exception {
    for (Emne emne : subjects) {
      if (emneFilter.accept(emne)) {
        pushEmneToSolr(emne);
      } else {
        String emneid = formatTokens(emne.getEmneid().getEmnekode(), emne.getEmneid().getVersjonskode());
        log.info(String.format("Skipping \"%s\" due to filter %s", emneid, emneFilter.getClass().getName()));
      }
    }
  }

  private void pushCourses(List<Kurs> courses, String language) throws Exception {

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
  }

  private void cleanIndex(StudinfoType infoType, String language) throws SolrServerException, IOException {

    SolrServer solrServer = getSolrServer(language, infoType);
    String categoryQuery = "cat:"+CATEGORY_STUDINFO+" AND cat:"+infoType.toString();
    solrServer.deleteByQuery(categoryQuery);
  }

  private void pushProgramToSolr(Studieprogram prog) throws Exception {
    
    // clean utdanningsplan
    if (prog.isSetUtdanningsplan()) {
      Studinfos.cleanUtdanningsplan(prog.getUtdanningsplan(), prog.getStudieprogramkode(), context.get().getStartYearSemester(), Studinfos.numberOfSemesters(prog));
    }

    Map<String, Object> beanmap = getBeanMap(prog, "/studieprogram");
    updateDocuments(StudinfoType.STUDIEPROGRAM, prog.getSprak(), beanmap, null, "/studieprogram");
  }

  private void updateDocuments(StudinfoType infoType, String infoLanguage, Map<String, ?> beanProps, String parentDocId, String path) throws Exception {
    SolrInputDocument doc = new SolrInputDocument();
    String docId = createId(infoType, beanProps, path);
    doc.addField("id", docId);
    doc.addField("year_i", context.get().getCurrentYearSemester().getYear());
    doc.addField("semester_s", context.get().getCurrentYearSemester().getSemester().toString());
    addCategories(doc, infoType);
    for (Entry<String, ?> entry : beanProps.entrySet()) {
      String propName = getSolrFieldName(path, entry.getKey());
      if (propName != null) {
        addFieldToDoc(path, doc, entry.getValue(), propName);
      }
    }
    updateDocument(infoType, infoLanguage, doc);
  }

  private void addFieldToDoc(String path, SolrInputDocument doc, Object value, String solrFieldName) {
    if (value instanceof Collection) {
      Collection<?> coll = (Collection<?>)value;
      for (Object elem : coll) {
        addFieldToDoc(path, doc, elem, solrFieldName);
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
      emne.addProperty("anbefalteForkunskaper", anbForkunn);
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
    
    updateDocuments(StudinfoType.EMNE, emne.getSprak(), beanmap, null, "/emne");
  }

  private void updateDocument(StudinfoType infoType, String lang, SolrInputDocument doc) throws SolrServerException, IOException {
    SolrServer solrServer = getSolrServer(lang, infoType);
    solrServer.add(doc, 3000);
  }

  private SolrServer getSolrServer(String sprak, StudinfoType studinfoType) {

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

  public void setCpUrl(DomainUrl cpUrl) {
    this.cpUrl = cpUrl;
  }

  public void setSiteId(int siteId) {
    this.siteId = siteId;
  }
  
  public void setCpAccessorHolder(XmlAccessorHolder holder) {
    this.xmlAccessorHolder = holder;
  }

  public void setEvuCategoryId(int evuCategoryId) {
    this.evuCategoryId = evuCategoryId;
  }

  public int getEvuCategoryId() {
    return evuCategoryId;
  }

  public int[] getEvuTemplateIds() {
    return evuTemplateIds;
  }

  public void setEvuTemplateIds(int[] ids) {
    this.evuTemplateIds = ids;
  }

  public void setEvuTemplateIdsFromString(String csvString) {
    String[] strArray = csvString.split("\\s*,\\s*");
    int[] ids = new int[strArray.length];
    int i = 0;
    for (String s : strArray) {
      ids[i++] = Integer.parseInt(s);
    }
    setEvuTemplateIds(ids);
  }
  
  public void setPurgeIndexBeforeUpdate(boolean purgeIndexBeforeUpdate) {
    this.purgeIndexBeforeUpdate = purgeIndexBeforeUpdate;
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
      
    } else if(propName.equals("inngarIFag")) {
      map.put(propName, createStringArray(value));
      
    } else if (path.startsWith("/kurs/dato")) {
      map.put(propName, xmlCalToSolrDateString((XMLGregorianCalendar)value));
      
    } else {
      map.put(propName, gson.toJson(value));
    }
  }

  private String xmlCalToSolrDateString(XMLGregorianCalendar value) {
    GregorianCalendar gregorianCalendar = value.toGregorianCalendar(TIME_ZONE_UTC, null, null);
    StringBuilder out = new StringBuilder();
    try {
      DateUtil.formatDate(gregorianCalendar.getTime(), gregorianCalendar, out);
    } catch(IOException e) {
      log.warn("xml cal: " + value.toXMLFormat());
    }
    
    return out.toString();
  }

  private List<String> createStringArray(Object value) {
    Collection<?> coll = (Collection<?>)value;
    List<String> jsonArray = new ArrayList<String>(coll.size());
    for (Object o : coll) {
      jsonArray.add(String.valueOf(o));
    }
    return jsonArray;
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
}
