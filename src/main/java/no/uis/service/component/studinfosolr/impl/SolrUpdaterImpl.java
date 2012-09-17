package no.uis.service.component.studinfosolr.impl;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.PostConstruct;

import no.uis.service.component.studinfosolr.SolrUpdater;
import no.uis.service.fsimport.StudInfoImport.StudinfoType;
import no.uis.service.studinfo.data.Emne;
import no.uis.service.studinfo.data.Emneid;
import no.uis.service.studinfo.data.Fagperson;
import no.uis.service.studinfo.data.FsSemester;
import no.uis.service.studinfo.data.FsStudieinfo;
import no.uis.service.studinfo.data.KravSammensetting;
import no.uis.service.studinfo.data.Kurs;
import no.uis.service.studinfo.data.Kurskategori;
import no.uis.service.studinfo.data.Studieprogram;
import no.uis.service.studinfo.data.Utdanningsplan;
import no.uis.service.studinfo.data.YESNOType;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.BeanUtils;

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

  private static final String CATEGORY_STUDINFO = "studinfo";

  private static final char ID_TOKEN_SEPARATOR = '_';
  
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SolrUpdaterImpl.class);

  private Map<String, SolrServer> solrServers;
  
  private Map<String, String> solrFieldMapping;

  private boolean purgeIndexBeforeUpdate;
  
  private DomainUrl cpUrl;
  private XmlAccessorHolder xmlAccessorHolder;
  private int evuCategoryId = 5793;

  private Gson gson;
  private Map<Class<?>, TypeAdapter<?>> typeAdapters;

  private static final Object[] NULL_OBJECTS = (Object[])null;

  @PostConstruct
  public void init() {
    GsonBuilder builder = new GsonBuilder();
    if (typeAdapters != null) {
      for (Map.Entry<Class<?>, TypeAdapter<?>> adapter : typeAdapters.entrySet()) {
        builder.registerTypeAdapter(adapter.getKey(), adapter.getValue());
      }
    }
    gson = builder.create();
  }

  public void setSolrFieldMapping(Map<String, String> mapping) {
    this.solrFieldMapping = mapping;
  }

  public void setTypeAdapters(Map<Class<?>, TypeAdapter<?>> typeAdapters) {
    this.typeAdapters = typeAdapters;
  }

  @Override
  public void pushStudieInfo(FsStudieinfo info, int year, FsSemester semester, String language) throws Exception {
    
    catalogYear.set(year);
    catalogSemester.set(semester);
    
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
      catalogYear.remove();
      catalogSemester.remove();
    }
  }

  private void pushPrograms(List<Studieprogram> programs, String language) throws Exception {
    for (Studieprogram program : programs) {
      pushProgramToSolr(program);
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
    Map<String, Object> beanmap = getDescriptor(prog, "/studieprogram");
    updateDocuments(StudinfoType.STUDIEPROGRAM, prog.getSprak(), beanmap, null, "/studieprogram");
  }

  private void updateDocuments(StudinfoType infoType, String infoLanguage, Map<String, ?> beanProps, String parentDocId, String path) throws Exception {
    SolrInputDocument doc = new SolrInputDocument();
    String docId = createId(infoType, beanProps, path);
    doc.addField("id", docId);
    addCategories(doc, infoType);
    for (Entry<String, ?> entry : beanProps.entrySet()) {
      String propName = getSolrFieldName(path, entry.getKey());
      addFieldToDoc(path, doc, entry.getValue(), propName);
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
    return solrFieldName;
  }

  private Map<String, Object> getDescriptor(Object fsType, String path) {
    Class<?> klass = fsType.getClass();
    PropertyDescriptor[] pdArray = BeanUtils.getPropertyDescriptors(klass);
    Map<String, Object> map = new HashMap<String, Object>();
    for (PropertyDescriptor pd : pdArray) {
      String name = pd.getName();
      if (!name.equals("class") && !isIsSetProperty(pd)) {
        Object value = getValue(fsType, pd);
        if (value != null) {
          String newPath = path + "/" + name;
          addValue(map, name, value, newPath);
        }
      }
    }
    return map;
  }

  private boolean isIsSetProperty(PropertyDescriptor pd) {
    if (pd.getReadMethod().getName().startsWith("isSet")) {
      return true;
    }
    return false;
  }

  private String createId(StudinfoType infoType, Map<String, ?> beanProps, String path) {
    switch(infoType) {
      case STUDIEPROGRAM:
        if (path.equals("/studieprogram")) {
          return formatTokens(infoType, beanProps.get("studieprogramkode"));
        }
      case EMNE:
        if (path.equals("/emne")) {
          return formatTokens(infoType, beanProps.get("emneid"));
        }
        
    }
    return UUID.randomUUID().toString();
  }

  private void pushKursToSolr(Kurs kurs) throws Exception {
    SolrInputDocument doc = new SolrInputDocument();

    String courseId = formatTokens(kurs.getKursid().getKurskode(), kurs.getKursid().getTidkode());
    doc.addField("id", formatTokens(StudinfoType.KURS, courseId));

    addCategories(doc, StudinfoType.KURS);

    doc.addField("name", kurs.getKursnavn());
    for (Kurskategori kursKat : kurs.getKurskategoriListe()) {
      doc.addField("course_category_code", kursKat.getKurskategorikode());
    }

    doc.addField("course_code_s", kurs.getKursid().getKurskode());
    doc.addField("course_time_code_s", kurs.getKursid().getTidkode());
    String date = CalendarSerializer.convertToSolrString(kurs.getDatoFristSoknad());
    if (date != null) {
      doc.addField("application_deadline_dt", date);
    }
    date = CalendarSerializer.convertToSolrString(kurs.getDatoPubliseresFra());
    if (date != null) {
      doc.addField("publish_from_dt", date);
    }
    date = CalendarSerializer.convertToSolrString(kurs.getDatoPubliseresTil());
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

    updateDocument(StudinfoType.KURS, kurs.getSprak(), doc);
  }

  private void pushEmneToSolr(Emne emne) throws Exception {
    
    Map<String, Object> beanmap = getDescriptor(emne, "/emne");
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
    //solrServer.commit(true, true);
  }

  private SolrServer getSolrServer(String sprak, StudinfoType studinfoType) {

    return solrServers.get(formatTokens(studinfoType.name(), sprak));
  }

  private static void addCategories(SolrInputDocument doc, StudinfoType infoType) {
    doc.addField("cat", CATEGORY_STUDINFO);
    doc.addField("cat", infoType.toString());
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

  private ThreadLocal<Integer> catalogYear = new ThreadLocal<Integer>();
  private ThreadLocal<FsSemester> catalogSemester = new ThreadLocal<FsSemester>();

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

  public void setPurgeIndexBeforeUpdate(boolean purgeIndexBeforeUpdate) {
    this.purgeIndexBeforeUpdate = purgeIndexBeforeUpdate;
  }

  /**
   * Semesters prior to the one queried for ({@link #catalogSemester} and {@link #catalogYear}) are considered "old".
   */
  private boolean isOldKravSammen(KravSammensetting ks) {
    int validFromYear = ks.getArstallGjelderFra().getYear();
    
    FsSemester validFromSemester = ks.getTerminkodeGjelderFra();
    
    int _catalogYear = catalogYear .get();
    
    if (validFromYear < _catalogYear) {
      return true;
    }
    
    if (validFromYear == _catalogYear) {
      FsSemester _catalogSemester = catalogSemester.get();
      if (_catalogSemester.equals(FsSemester.HOST) && validFromSemester.equals(FsSemester.VAR)) {
        return true;
      }
    }
    
    return false;
  }
  
  private void addValue(Map<String, Object> map, String propName, Object value, String path) {
    if (value instanceof String) {
      map.put(propName, value);
    
    } else if (value.getClass().isPrimitive()) {
      map.put(propName, value);
    
    } else if (value instanceof Number) {
      map.put(propName, value);
    
    } else if (value instanceof YESNOType) {
      map.put(propName, value.equals(YESNOType.J) ? Boolean.TRUE : Boolean.FALSE);
    
    } else if(value instanceof Utdanningsplan) {
      Utdanningsplan uplan = (Utdanningsplan)value;
      if (uplan.isSetKravSammensetting()) {
        Iterator<KravSammensetting> ksIter = uplan.getKravSammensetting().iterator();
        while (ksIter.hasNext()) {
          KravSammensetting ks = ksIter.next();
          if (isOldKravSammen(ks)) {
            ksIter.remove();
          }
        }
      }
      if (uplan.isSetPlaninformasjon()) {
        uplan.unsetPlaninformasjon();
      }
      map.put(propName, gson.toJson(value));
      
    } else if (value instanceof Emneid) {
      Emneid emneid = (Emneid)value;
      map.put(propName, formatTokens(emneid.getInstitusjonsnr(), emneid.getEmnekode(), emneid.getVersjonskode()));
      if (path.equals("/emne/emneid")) {
        map.put("emnekode", emneid.getEmnekode());
      }
      
    } else if (isCollectionOfType(value, Fagperson.class)) {
      map.put(propName, createJsonArray(value));
      
    } else if(propName.equals("inngarIStudieprogram")) {
      map.put(propName, createStringArray(value));
      
    } else if(propName.equals("inngarIFag")) {
      map.put(propName, createStringArray(value));
      
    } else { 
      map.put(propName, gson.toJson(value));
    }
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

  private static boolean isCollectionOfType(Object value, Class<?> type) {
    Class<?> componentType = value.getClass().getComponentType();
    if (componentType != null && type.isAssignableFrom(componentType)) {
      return true;
    }

    if (value instanceof Collection) {
      Collection<?> coll = (Collection<?>)value;
      for (Object obj : coll) {
        if (!type.isAssignableFrom(obj.getClass())) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
  
  private static Object getValue(Object fsType, PropertyDescriptor pd) {
    try {
      if (isPropertySet(fsType, pd)) {
        Method rm = pd.getReadMethod();
        if (rm != null) {
          return rm.invoke(fsType, NULL_OBJECTS);
        }
      }
    } catch(Exception ex) {
      log.error("get " + pd.getName(), ex);
    }
    return null;
  }

  private static boolean isPropertySet(Object fsType, PropertyDescriptor pd) throws Exception {
    boolean useValue = true;
    
    String methodName = "isSet"+StringUtils.capitalize(pd.getName());
    try {
      Method isSetMethod = fsType.getClass().getMethod(methodName);
      if (isSetMethod != null) {
        Boolean isSet = (Boolean)isSetMethod.invoke(fsType, NULL_OBJECTS);
        if (!isSet.booleanValue()) {
          useValue = false;
        }
      }
    } catch(NoSuchMethodException ex) {
      log.info(methodName, ex);
    }
    return useValue;
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
  
}
