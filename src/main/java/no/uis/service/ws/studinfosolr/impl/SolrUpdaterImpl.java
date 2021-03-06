/*
 Copyright 2012-2013 University of Stavanger, Norway

 Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package no.uis.service.ws.studinfosolr.impl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.datatype.XMLGregorianCalendar;

import no.uis.fsws.proxy.StudInfoImport.StudinfoType;
import no.uis.fsws.studinfo.data.FsSemester;
import no.uis.fsws.studinfo.data.FsYearSemester;
import no.uis.fsws.studinfo.util.PropertyInfo;
import no.uis.fsws.studinfo.util.PropertyInfoUtils;
import no.uis.service.ws.studinfosolr.SolrFieldnameResolver;
import no.uis.service.ws.studinfosolr.SolrProxy;
import no.uis.service.ws.studinfosolr.SolrType;
import no.uis.service.ws.studinfosolr.SolrUpdateListener;
import no.uis.service.ws.studinfosolr.SolrUpdater;
import no.uis.service.ws.studinfosolr.util.SolrUtil;
import no.uis.studinfo.commons.AcceptAllEmne;
import no.uis.studinfo.commons.AcceptAllStudieprogram;
import no.uis.studinfo.commons.StudinfoContext;
import no.uis.studinfo.commons.StudinfoFilter;
import no.uis.studinfo.commons.Studinfos;
import no.uis.studinfo.commons.Utils;
import no.uis.studinfo.convert.AbstractStringConverter;
import no.uis.studinfo.convert.CollectionConverter;
import no.uis.studinfo.convert.InngarIStudieprogramConverter;
import no.uis.studinfo.convert.ObligoppgaveConverter;
import no.uis.studinfo.convert.StringConverter;
import no.uis.studinfo.convert.StringConverterUtil;
import no.usit.fsws.schemas.studinfo.Emne;
import no.usit.fsws.schemas.studinfo.Emneid;
import no.usit.fsws.schemas.studinfo.Fagperson;
import no.usit.fsws.schemas.studinfo.FagpersonListe;
import no.usit.fsws.schemas.studinfo.InngarIFagListe;
import no.usit.fsws.schemas.studinfo.InngarIStudieprogram;
import no.usit.fsws.schemas.studinfo.Kurs;
import no.usit.fsws.schemas.studinfo.Kursid;
import no.usit.fsws.schemas.studinfo.Kurskategori;
import no.usit.fsws.schemas.studinfo.KurskategoriListe;
import no.usit.fsws.schemas.studinfo.Obligoppgave;
import no.usit.fsws.schemas.studinfo.Sprak;
import no.usit.fsws.schemas.studinfo.SprakListe;
import no.usit.fsws.schemas.studinfo.Sted;
import no.usit.fsws.schemas.studinfo.Studieprogram;
import no.usit.fsws.schemas.studinfo.Utdanningsplan;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.xerces.dom.ElementImpl;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;

/**
 * Default implementation of SolrUpdater.
 */
@ManagedResource(
  objectName = "uis:service=ws-studinfo-solr,component=updater",
  description = "SolrUpdater",
  log = false
)
public class SolrUpdaterImpl implements SolrUpdater {

  /**
   * Instance of of a SolrProxy that does nothing.  
   */
  public static final SolrProxy DUMMY_PROXY = new DefaultProxy();
  
  private static final String TIDKODE = "tidkode";

  private static final String KURSKODE = "kurskode";
  
  private static final String KURS_ROOT = "/kurs";

  private static final String EMNE_ROOT = "/emne";

  private static final String CATEGORY_STUDINFO = "STUDINFO";

  private static final String STUDIEPROGRAM_ROOT = "/studieprogram";
  
  private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(SolrUpdaterImpl.class);
  
  private static final StringConverter SPRAK_TO_STRING = new AbstractStringConverter<Sprak>() {

    @Override
    public String convert(Sprak o) {
      return o.getSprakkode();
    }
  };

  private static final StringConverter INNGAR_I_STUDIEPROGRAM_TO_STRING = new AbstractStringConverter<InngarIStudieprogram>() {

    @Override
    public String convert(InngarIStudieprogram o) {
      return o.getStudieprogramkode();
    }
  };

  private static final StringConverter FAGPERSON_TO_STRING = new AbstractStringConverter<Fagperson>() {

    @Override
    public String convert(Fagperson o) {
      return o.getPersonid().toString();
    }
  };

  
  private static ThreadLocal<StudinfoContext> context = new ThreadLocal<StudinfoContext>();

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

  private Map<SolrType, SolrProxy> solrProxies;

  
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
      // for <any/> element
      builder.registerTypeAdapterFactory(TypeAdapters.newTypeHierarchyFactory(ElementImpl.class, new XmlElementAdapter()));
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

  public void setSolrProxies(Map<SolrType, SolrProxy> proxies) {
    this.solrProxies = proxies;
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
  public void pushCourses(List<Kurs> courses, int year, FsSemester semester, String language, SolrType solrType) throws Exception {
    synchronized(courseSync) {
      context.set(new StudinfoContext(new FsYearSemester(year, semester), language));
      try {
        courseListenerSupport.fireBeforePushElements(context, solrType, courses);
        for (Kurs kurs : courses) {
          pushKursToSolr(solrType, kurs);
        }
        courseListenerSupport.fireAfterPushElements(context, solrType);
      } finally {
        context.remove();
      }
    }
  }

  @Override
  public void pushSubjects(List<Emne> subjects, int year, FsSemester semester, String language, SolrType solrType) throws Exception {
    synchronized(subjectSync) {
      context.set(new StudinfoContext(new FsYearSemester(year, semester), language));
      try {
        emneListenerSupport.fireBeforePushElements(context, solrType, subjects);
        for (Emne emne : subjects) {
          if (emneFilter.accept(emne)) {
            pushEmneToSolr(solrType, emne);
          }
        }
        emneListenerSupport.fireAfterPushElements(context, solrType);
      } finally {
        context.remove();
      }
    }
  }

  @Override
  public void pushPrograms(List<Studieprogram> programs, int year, FsSemester semester, String language, SolrType solrType) throws Exception {
    synchronized(programSync) {
      context.set(new StudinfoContext(new FsYearSemester(year, semester), language));
      try {
        programListenerSupport.fireBeforePushElements(context, solrType, programs);
        for (Studieprogram program : programs) {
          if (studieprogramFilter.accept(program)) {
            pushProgramToSolr(solrType, program);
          }
        }
        programListenerSupport.fireAfterPushElements(context, solrType);
      } finally {
        context.remove();
      }
    }
  }

  @ManagedOperation(description = "Delete documents of solr index (given by language) by solr query")
  @ManagedOperationParameters({
    @ManagedOperationParameter(name = "language", description = "one-letter language code: (B)okmål, (E)ngelsk or (N)ynorsk"),
    @ManagedOperationParameter(name = "query", description = "Solr query for documents to delete"),
    @ManagedOperationParameter(name = "solrType", description = "WWW or STUDENT")
  })
  public void deleteByQuery(String language, String query, String solrType) throws Exception {
    this.deleteByQuery(language, query, SolrType.valueOf(solrType));
  }
  
  @Override
  public void deleteByQuery(String language, String query, SolrType solrType) throws Exception {
    getProxy(solrType).deleteByQuery(language, query);
  }
  
  private void pushProgramToSolr(SolrType solrType, Studieprogram prog) throws Exception {
    
    Map<String, Object> beanmap = getBeanMap(prog, STUDIEPROGRAM_ROOT);
    updateDocuments(solrType, StudinfoType.STUDIEPROGRAM, prog.getSprak(), beanmap, null, STUDIEPROGRAM_ROOT);
  }

  private void updateDocuments(SolrType solrType, StudinfoType infoType, String infoLanguage, Map<String, ?> beanProps,
      String parentDocId, String path) throws Exception
  {
    SolrInputDocument doc = new SolrInputDocument();
    String docId = createId(infoType, beanProps, path);
    doc.addField("id", docId);
    addCategories(doc, infoType);
    addFieldToDoc(doc, context.get().getCurrentYearSemester().getYear(), solrFieldnameResolver.getSolrFieldName(path, "year"));
    addFieldToDoc(doc, context.get().getCurrentYearSemester().getSemester().toString(),
      solrFieldnameResolver.getSolrFieldName(path, "semester"));
    for (Entry<String, ?> entry : beanProps.entrySet()) {
      String propName = solrFieldnameResolver.getSolrFieldName(path, entry.getKey());
      if (propName != null) {
        addFieldToDoc(doc, entry.getValue(), propName);
      }
    }
    updateDocument(solrType, infoLanguage, doc);
  }

  private void updateDocument(SolrType solrType, String lang, SolrInputDocument doc) throws SolrServerException, IOException {
    this.getProxy(solrType).updateDocument(lang, doc);
  }

  public static void addFieldToDoc(SolrInputDocument doc, Object value, String solrFieldName) {
    if (value != null) {
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
    String id = null;
    if (infoType.equals(StudinfoType.STUDIEPROGRAM) && STUDIEPROGRAM_ROOT.equals(path)) {
      id = Utils.formatTokens(infoType, beanmap.get("studieprogramkode"), context.get().getCurrentYearSemester().getYear(),
        context.get().getCurrentYearSemester().getSemester());

    } else if (infoType.equals(StudinfoType.EMNE) && EMNE_ROOT.equals(path)) {
      id = Utils.formatTokens(infoType, beanmap.get("emneid"), context.get().getCurrentYearSemester().getYear(), 
        context.get().getCurrentYearSemester().getSemester());

   } else if (infoType.equals(StudinfoType.KURS) && KURS_ROOT.equals(path)) {
      id = Utils.formatTokens(infoType, beanmap.get(KURSKODE), beanmap.get(TIDKODE));
    
    } else {
      id = UUID.randomUUID().toString();
    }
    
    return id;
  }

  private void pushKursToSolr(SolrType solrType, Kurs kurs) throws Exception {
    Map<String, Object> beanmap = getBeanMap(kurs, KURS_ROOT);
    String kurskode = Utils.formatTokens(beanmap.get(KURSKODE), beanmap.get(TIDKODE));
    beanmap.put("kursid", kurskode);
    courseListenerSupport.fireBeforeSolrUpdate(context, solrType, kurs, beanmap);
    updateDocuments(solrType, StudinfoType.KURS, kurs.getSprak(), beanmap, null, KURS_ROOT);
  }

  private void pushEmneToSolr(SolrType solrType, Emne emne) throws Exception {
    
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
    
    Map<String, Object> beanmap = getBeanMap(emne, EMNE_ROOT);
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
    
    emneListenerSupport.fireBeforeSolrUpdate(context, solrType, emne, beanmap);
    updateDocuments(solrType, StudinfoType.EMNE, emne.getSprak(), beanmap, null, EMNE_ROOT);
  }

  private static void addCategories(SolrInputDocument doc, StudinfoType infoType) {
    doc.addField("cat", CATEGORY_STUDINFO);
    doc.addField("cat", infoType.toString());
  }

  @ManagedOperation(description = "get Server Map as String")
  public String getSolrServersAsString() {
    StringBuilder sb = new StringBuilder();
    
    for (SolrType solrType : SolrType.values()) {
      for (Entry<String, SolrServer> entry : getProxy(solrType).getSolrServers().entrySet()) {
        sb.append(solrType).append(": ").append(entry.getKey());
        sb.append(" = ");
        SolrServer server = entry.getValue();
        if (server instanceof SolrServerFactory.SolrServerInfo) {
          sb.append(((SolrServerFactory.SolrServerInfo)server).getBaseURL());
        } else if (server instanceof ConcurrentUpdateSolrServer) {
          sb.append("Unable to get server name");
        } else {
          sb.append(server.toString());
        }
        sb.append("\n");
      }
    }
    return sb.toString();
  }
  
  @ManagedOperation(
    description = "set the Solr server for a specific language. If the URL is null or empty, the solrserver for the given language is removed"
    )
  @ManagedOperationParameters({
    @ManagedOperationParameter(name = "solrType", description = "set of solr cores to use"),
    @ManagedOperationParameter(name = "lang", description = "language code: B, E or N"),
    @ManagedOperationParameter(name = "url", description = "URL to the solr core"),
    @ManagedOperationParameter(name = "username", description = "optional username for the Solr core"),
    @ManagedOperationParameter(name = "password", description = "optional password for the Solr core")
  })
  public void assignSolrServer(String solrTypeString, String lang, String url, String username, String password) throws MalformedURLException {
    
    SolrType solrType = SolrType.valueOf(solrTypeString);
    if (url == null || url.trim().isEmpty()) {
      getProxy(solrType).removeServer(lang);
    } else {
      SolrServerFactory serverFactory = new SolrServerFactory(); 
      serverFactory.setUrl(new URL(url));
      if (username != null && !username.trim().isEmpty()) {
        serverFactory.setUsername(username);
      }
      if (password != null && !password.trim().isEmpty()) {
        serverFactory.setPassword(password);
      }
      getProxy(solrType).setServer(lang, serverFactory.getObject());
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

      if (uplan.isSetPlaninformasjonListe()) {
        uplan.setPlaninformasjonListe(null);
      }
      map.put(propName, gson.toJson(value));
      
    } else if (value instanceof Emneid) {
      Emneid emneid = (Emneid)value;
      map.put(propName, Utils.formatTokens(emneid.getInstitusjonsnr(), emneid.getEmnekode(), emneid.getVersjonskode()));
      if ("/emne/emneid".equals(path)) {
        map.put("emnekode", Utils.formatTokens(emneid.getEmnekode(), emneid.getVersjonskode()));
      }
      
    } else if (value instanceof Kursid) {
      Kursid kursid = (Kursid)value;
      map.put(KURSKODE, kursid.getKurskode());
      map.put(TIDKODE, kursid.getTidkode());
    
    } else if ("kurskategoriListe".equals(propName)) {
      KurskategoriListe kkList = (KurskategoriListe)value;
      if (kkList.isSetKurskategori()) {
        List<String> kkkList = new ArrayList<String>(kkList.getKurskategori().size());
        for (Kurskategori kk : kkList.getKurskategori()) {
          kkkList.add(kk.getKurskategorikode());
        }
        map.put(propName, kkkList);
      }
    } else if ("fagpersonListe".equals(propName)) {
      FagpersonListe fpListe = (FagpersonListe)value;
      if (fpListe.isSetFagperson()) {
        map.put(propName, createJsonArray(fpListe.getFagperson()));
        
        map.put("fagpersonidListe", Utils.createStringArray(fpListe.getFagperson(), new HashSet<String>(), FAGPERSON_TO_STRING));
      }      
    } else if (value instanceof Sted) { // fagansvarlig, adminansvarlig
      map.put(propName, gson.toJson(value));
      addStedValue(map, propName+'_', (Sted)value);
    
    } else if("inngarIStudieprogram".equals(propName)) {
      map.put(propName, gson.toJson(value));
      
      map.put(propName+'_', Utils.createStringArray(value, null, INNGAR_I_STUDIEPROGRAM_TO_STRING));
    
    } else if ("sprakListe".equals(propName)) {
      SprakListe spListe = (SprakListe)value;
      Collection<String> usprak;
      if (spListe.isSetSprak()) {
        map.put("sprakListe", gson.toJson(spListe.getSprak()));
        usprak = Utils.createStringArray(spListe.getSprak(), new HashSet<String>(), SPRAK_TO_STRING);
  
      } else {
        usprak = Arrays.asList("NO");
      }
      map.put(propName+'_', usprak);
      
    } else if("inngarIFag".equals(propName)) {
      InngarIFagListe ifListe = (InngarIFagListe)value;
      if (ifListe.isSetInngarIFag()) {
        map.put(propName, Utils.createStringArray(ifListe.getInngarIFag(), null, null));
      }
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
        if (!(boolean)mIsSet.invoke(fsType)) {
          return null;
        }
      }
      
      Method mGet = pi.getGet();
      if (mGet != null && mGet.getParameterTypes().length == 0) {
        return mGet.invoke(fsType);
      }
    } catch(Exception ex) {
      LOG.error("get " + pi.getPropName(), ex);
    }
    return null;
  }

  @PreDestroy
  public void destroy() throws Exception {
    courseListenerSupport.cleanup();
    emneListenerSupport.cleanup();
    programListenerSupport.cleanup();
  }
  
  private SolrProxy getProxy(SolrType type) {
    if (solrProxies != null) {
      SolrProxy proxy = solrProxies.get(type);
      if (proxy != null) {
        return proxy;
      }
    }
    return DUMMY_PROXY;
  }

  private static final class DefaultProxy implements SolrProxy {
    @Override
    public void deleteByQuery(String language, String query) throws SolrServerException, IOException {
    }

    @Override
    public void updateDocument(String lang, SolrInputDocument doc) throws SolrServerException, IOException {
    }

    @Override
    public Map<String, SolrServer> getSolrServers() {
      return null;
    }

    @Override
    public void removeServer(String lang) {
    }

    @Override
    public void setServer(String lang, SolrServer object) {
    }
  }
}
