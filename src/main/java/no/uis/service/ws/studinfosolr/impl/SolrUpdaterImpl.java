package no.uis.service.ws.studinfosolr.impl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
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

import no.uis.service.component.fsimport.StudInfoImport.StudinfoType;
import no.uis.service.component.fsimport.util.PropertyInfo;
import no.uis.service.component.fsimport.util.PropertyInfoUtils;
import no.uis.service.studinfo.commons.AcceptAllEmne;
import no.uis.service.studinfo.commons.AcceptAllStudieprogram;
import no.uis.service.studinfo.commons.StudinfoContext;
import no.uis.service.studinfo.commons.StudinfoFilter;
import no.uis.service.studinfo.commons.Studinfos;
import no.uis.service.studinfo.commons.ToString;
import no.uis.service.studinfo.commons.Utils;
import no.uis.service.studinfo.convert.CollectionConverter;
import no.uis.service.studinfo.convert.InngarIStudieprogramConverter;
import no.uis.service.studinfo.convert.ObligoppgaveConverter;
import no.uis.service.studinfo.convert.StringConverter;
import no.uis.service.studinfo.convert.StringConverterUtil;
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
import no.uis.service.studinfo.data.Sprak;
import no.uis.service.studinfo.data.Sted;
import no.uis.service.studinfo.data.Studieprogram;
import no.uis.service.studinfo.data.Utdanningsplan;
import no.uis.service.ws.studinfosolr.SolrFieldnameResolver;
import no.uis.service.ws.studinfosolr.SolrProxy;
import no.uis.service.ws.studinfosolr.SolrUpdateListener;
import no.uis.service.ws.studinfosolr.SolrUpdater;
import no.uis.service.ws.studinfosolr.util.SolrUtil;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;

@ManagedResource(
  objectName="uis:service=ws-studinfo-solr,component=updater",
  description="SolrUpdater",
  log=false
)
public class SolrUpdaterImpl implements SolrUpdater {

  private static final String CATEGORY_STUDINFO = "STUDINFO";

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

  private SolrFieldnameResolver solrFieldnameResolver;

  private Gson gson;
  private Map<Class<?>, TypeAdapter<?>> typeAdapters;
  
  private StudinfoFilter<Emne> emneFilter = new AcceptAllEmne();
  private StudinfoFilter<Studieprogram> studieprogramFilter = new AcceptAllStudieprogram();

  private ListenerSupport<Kurs> courseListenerSupport = new ListenerSupport<Kurs>();

  private ListenerSupport<Emne> emneListenerSupport = new ListenerSupport<Emne>();
  
  private ListenerSupport<Studieprogram> programListenerSupport = new ListenerSupport<Studieprogram>();
  
  private Object courseSync = new Object();

  private Object programSync = new Object();

  private Object subjectSync = new Object();

  private SolrProxy solrServerProxy;

  private static ThreadLocal<StudinfoContext> context = new ThreadLocal<StudinfoContext>();
  
  public static StudinfoContext getContext() {
    return context.get();
  }
  
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

  public void setSolrFieldnameResolver(SolrFieldnameResolver resolver) {
    this.solrFieldnameResolver = resolver;
  }

  public void setSolrProxy(SolrProxy proxy) {
    this.solrServerProxy = proxy;
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

  public void setCourseListeners(List<SolrUpdateListener<Kurs>> listeners) {
    courseListenerSupport.setListeners(listeners);
  }
  
  public void setSubjectListeners(List<SolrUpdateListener<Emne>> listeners) {
    this.emneListenerSupport.setListeners(listeners);
  }
  
  public void setProgramListeners(List<SolrUpdateListener<Studieprogram>> listeners) {
    this.programListenerSupport.setListeners(listeners);
  }
  
  @Override
  public void pushCourses(List<Kurs> courses, int year, FsSemester semester, String language) throws Exception {
    synchronized(courseSync) {
      context.set(new StudinfoContext(new FsYearSemester(year, semester), language));
      try {
        courseListenerSupport.fireBeforePushElements(courses);
        for (Kurs kurs : courses) {
          pushKursToSolr(kurs);
        }
        courseListenerSupport.fireAfterPushElements();
      } finally {
        context.remove();
      }
    }
  }

  @Override
  public void pushSubjects(List<Emne> subjects, int year, FsSemester semester, String language) throws Exception {
    synchronized(subjectSync ) {
      context.set(new StudinfoContext(new FsYearSemester(year, semester), language));
      try {
        emneListenerSupport.fireBeforePushElements(subjects);
        for (Emne emne : subjects) {
          if (emneFilter.accept(emne)) {
            pushEmneToSolr(emne);
          }
        }
        emneListenerSupport.fireAfterPushElements();
      } finally {
        context.remove();
      }
    }
  }

  @Override
  public void pushPrograms(List<Studieprogram> programs, int year, FsSemester semester, String language) throws Exception {
    synchronized(programSync) {
      context.set(new StudinfoContext(new FsYearSemester(year, semester), language));
      try {
        programListenerSupport.fireBeforePushElements(programs);
        for (Studieprogram program : programs) {
          if (studieprogramFilter.accept(program)) {
            pushProgramToSolr(program);
          }
        }
        programListenerSupport.fireAfterPushElements();
      } finally {
        context.remove();
      }
    }
  }

  @Override
  @ManagedOperation(description="Delete documents of solr index (given by language) by solr query")
  @ManagedOperationParameters({
    @ManagedOperationParameter(name="language", description="one-letter language code: (B)okmål, (E)ngelsk or (N)ynorsk"),
    @ManagedOperationParameter(name="query", description="Solr query for documents to delete")
  })
  public void deleteByQuery(String language, String query) throws Exception {
    solrServerProxy.deleteByQuery(language, query);
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
    addCategories(doc, infoType);
    addFieldToDoc(doc, context.get().getCurrentYearSemester().getYear(), solrFieldnameResolver.getSolrFieldName(path, "year"));
    addFieldToDoc(doc, context.get().getCurrentYearSemester().getSemester().toString(), solrFieldnameResolver.getSolrFieldName(path, "semester"));
    for (Entry<String, ?> entry : beanProps.entrySet()) {
      String propName = solrFieldnameResolver.getSolrFieldName(path, entry.getKey());
      if (propName != null) {
        addFieldToDoc(doc, entry.getValue(), propName);
      }
    }
    updateDocument(infoLanguage, doc);
  }

  private void updateDocument(String lang, SolrInputDocument doc) throws SolrServerException, IOException {
    this.solrServerProxy.updateDocument(lang, doc);
  }

  public static void addFieldToDoc(SolrInputDocument doc, Object value, String solrFieldName) {
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
          return Utils.formatTokens(infoType, beanmap.get("studieprogramkode"), context.get().getCurrentYearSemester().getYear(), context.get().getCurrentYearSemester().getSemester());
        }
        break;
      case EMNE:
        if (path.equals("/emne")) {
          return Utils.formatTokens(infoType, beanmap.get("emneid"), context.get().getCurrentYearSemester().getYear(), context.get().getCurrentYearSemester().getSemester());
        }
        break;
      case KURS:
        if (path.equals("/kurs")) {
          return Utils.formatTokens(infoType, beanmap.get("kurskode"), beanmap.get("tidkode"));
        }
        break;
    }
    return UUID.randomUUID().toString();
  }

  private void pushKursToSolr(Kurs kurs) throws Exception {
    Map<String, Object> beanmap = getBeanMap(kurs, "/kurs");
    String kurskode = Utils.formatTokens(beanmap.get("kurskode"), beanmap.get("tidkode"));
    beanmap.put("kursid", kurskode);
    courseListenerSupport.fireBeforeSolrUpdate(kurs, beanmap);
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
    
    emneListenerSupport.fireBeforeSolrUpdate(emne, beanmap);
    updateDocuments(StudinfoType.EMNE, emne.getSprak(), beanmap, null, "/emne");
  }

  private static void addCategories(SolrInputDocument doc, StudinfoType infoType) {
    doc.addField("cat", CATEGORY_STUDINFO);
    doc.addField("cat", infoType.toString());
  }

  @ManagedOperation(description="get Server Map as String")
  public String getSolrServersAsString() {
    StringBuilder sb = new StringBuilder();
    
    for (Entry<String, SolrServer> entry : solrServerProxy.getSolrServers().entrySet()) {
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
      solrServerProxy.removeServer(lang);
    } else {
      SolrServerFactory serverFactory = new SolrServerFactory(); 
      serverFactory.setUrl(new URL(url));
      if (username != null && !username.trim().isEmpty()) {
        serverFactory.setUsername(username);
      }
      if (password != null && !password.trim().isEmpty()) {
        serverFactory.setPassword(password);
      }
      solrServerProxy.setServer(lang, serverFactory.getObject());
    }
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
      map.put(propName, Utils.formatTokens(emneid.getInstitusjonsnr(), emneid.getEmnekode(), emneid.getVersjonskode()));
      if (path.equals("/emne/emneid")) {
        map.put("emnekode", Utils.formatTokens(emneid.getEmnekode(), emneid.getVersjonskode()));
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
      
      map.put(propName+"_", Utils.createStringArray(value, new HashSet<String>(), FAGPERSON_TO_STRING));
      
    } else if (value instanceof Sted) { // fagansvarlig, adminansvarlig
      map.put(propName, gson.toJson(value));
      addStedValue(map, propName+"_", (Sted)value);
    
    } else if(propName.equals("inngarIStudieprogram")) {
      map.put(propName, gson.toJson(value));
      
      map.put(propName+"_", Utils.createStringArray(value, null, INNGAR_I_STUDIEPROGRAM_TO_STRING));
    
    } else if (propName.equals("sprakListe")) {
      map.put("sprakListe", gson.toJson(value));
      Collection<String> usprak = Utils.createStringArray(value, new HashSet<String>(), SPRAK_TO_STRING);

      if (usprak.isEmpty()) {
        usprak.add("NO");
      }
      map.put(propName+"_", usprak);
      
    } else if(propName.equals("inngarIFag")) {
      map.put(propName, Utils.createStringArray(value, null, null));
    
    } else if (path.startsWith("/kurs/dato")) {
      map.put(propName, SolrUtil.xmlCalToSolrDateString((XMLGregorianCalendar)value));
      
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

  @PreDestroy
  public void destroy() throws Exception {
    courseListenerSupport.cleanup();
    emneListenerSupport.cleanup();
    programListenerSupport.cleanup();
  }
}
