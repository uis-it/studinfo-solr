package no.uis.service.ws.studinfosolr.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import no.uis.fsws.studinfo.data.Emne;
import no.uis.fsws.studinfo.data.Fagperson;
import no.uis.service.ws.studinfosolr.EmployeeNumberResolver;
import no.uis.service.ws.studinfosolr.SolrFieldnameResolver;
import no.uis.service.ws.studinfosolr.SolrProxy;
import no.uis.service.ws.studinfosolr.SolrType;
import no.uis.service.ws.studinfosolr.SolrUpdateListener;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

public class FagpersonPushListener implements SolrUpdateListener<Emne> {

  private static final Logger LOG = Logger.getLogger(FagpersonPushListener.class);

  private Map<Integer, Fagperson> fagpersons = new HashMap<Integer, Fagperson>();

  private SolrFieldnameResolver solrFieldnameResolver;
  
  private Map<SolrType, SolrProxy> solrProxies;

  private String fPersonId;

  private String fFornavn;

  private String fEtternavn;

  private String fAnsattnummer;
  
  private EmployeeNumberResolver employeeNumberResolver;
  
  public void setSolrFieldnameResolver(SolrFieldnameResolver resolver) {
    this.solrFieldnameResolver = resolver;
  }
  
  public void setSolrProxies(Map<SolrType, SolrProxy> proxies) {
    solrProxies = proxies;
  }
  
  public void setEmployeeNumberResolver(EmployeeNumberResolver resolver) {
    this.employeeNumberResolver = resolver;
  }
  
  @Override
  public void fireBeforeSolrUpdate(SolrType solrType, Emne emne, Map<String, Object> beanmap) {
    for (Fagperson fagPerson : emne.getFagpersonListe()) {
      fagpersons.put(fagPerson.getPersonid().intValue(), fagPerson);
    }
  }

  @Override
  public void fireBeforePushElements(SolrType solrType, List<Emne> elements) {
    fagpersons.clear();
    fPersonId = solrFieldnameResolver.getSolrFieldName("/fagperson", "personid");
    fFornavn = solrFieldnameResolver.getSolrFieldName("/fagperson", "fornavn");
    fEtternavn = solrFieldnameResolver.getSolrFieldName("/fagperson", "etternavn");
    fAnsattnummer = solrFieldnameResolver.getSolrFieldName("/fagperson", "ansattnummer");
  }

  @Override
  public void fireAfterPushElements(SolrType solrType) {
    try {
      pushFagpersonsToSolr(solrType, SolrUpdaterImpl.getContext().getLanguage(), fagpersons);
    } catch(Exception e) {
      LOG.warn(null, e);
    }
  }

  @Override
  public void cleanup() {
    fagpersons.clear();
  }

  private void pushFagpersonsToSolr(SolrType solrType, String language, Map<Integer, Fagperson> fagpersons) throws Exception {
    for (Entry<Integer, Fagperson> entry : fagpersons.entrySet()) {
      SolrInputDocument doc = new SolrInputDocument();
      doc.addField("id", "fagperson_"+entry.getKey());
      doc.addField("cat", "fagperson");
      SolrUpdaterImpl.addFieldToDoc(doc, entry.getKey(), fPersonId);
      SolrUpdaterImpl.addFieldToDoc(doc, entry.getValue().getPersonnavn().getFornavn(), fFornavn);
      SolrUpdaterImpl.addFieldToDoc(doc, entry.getValue().getPersonnavn().getEtternavn(), fEtternavn);
      SolrUpdaterImpl.addFieldToDoc(doc, getAnsattnummer(entry.getValue().getFnr()), fAnsattnummer);
      updateDocument(solrType, language, doc);
    }
  }

  private String getAnsattnummer(String fnr) {
    return employeeNumberResolver.findEmployeeNumber(fnr);
  }

  private void updateDocument(SolrType solrType, String language, SolrInputDocument doc) throws SolrServerException, IOException {
    getProxy(solrType).updateDocument(language, doc);
  }
  
  private SolrProxy getProxy(SolrType type) {
    if (solrProxies != null) {
      SolrProxy proxy = solrProxies.get(type);
      if (proxy != null) {
        return proxy;
      }
    }
    return SolrUpdaterImpl.DUMMY_PROXY;
  }
}
