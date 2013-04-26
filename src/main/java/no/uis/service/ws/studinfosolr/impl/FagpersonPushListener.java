package no.uis.service.ws.studinfosolr.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import no.uis.service.studinfo.data.Emne;
import no.uis.service.studinfo.data.Fagperson;
import no.uis.service.studinfo.data.Personnavn;
import no.uis.service.ws.studinfosolr.EmployeeNumberResolver;
import no.uis.service.ws.studinfosolr.SolrFieldnameResolver;
import no.uis.service.ws.studinfosolr.SolrProxy;
import no.uis.service.ws.studinfosolr.SolrUpdateListener;

public class FagpersonPushListener implements SolrUpdateListener<Emne> {

  private static final Logger LOG = Logger.getLogger(FagpersonPushListener.class);

  private Map<Integer, Fagperson> fagpersons = new HashMap<Integer, Fagperson>();

  private SolrFieldnameResolver solrFieldnameResolver;
  
  private SolrProxy solrProxy;

  private String fPersonId;

  private String fFornavn;

  private String fEtternavn;

  private String fAnsattnummer;
  
  private EmployeeNumberResolver employeeNumberResolver;
  
  public void setSolrFieldnameResolver(SolrFieldnameResolver resolver) {
    this.solrFieldnameResolver = resolver;
  }
  
  public void setSolrProxy(SolrProxy proxy) {
    solrProxy = proxy;
  }
  
  public void setEmployeeNumberResolver(EmployeeNumberResolver resolver) {
    this.employeeNumberResolver = resolver;
  }
  
  @Override
  public void fireBeforeSolrUpdate(Emne emne, Map<String, Object> beanmap) {
    for (Fagperson fagPerson : emne.getFagpersonListe()) {
      fagpersons.put(fagPerson.getPersonid().intValue(), fagPerson);
    }
  }

  @Override
  public void fireBeforePushElements(List<Emne> elements) {
    fagpersons.clear();
    fPersonId = solrFieldnameResolver.getSolrFieldName("/fagperson", "personid");
    fFornavn = solrFieldnameResolver.getSolrFieldName("/fagperson", "fornavn");
    fEtternavn = solrFieldnameResolver.getSolrFieldName("/fagperson", "etternavn");
    fAnsattnummer = solrFieldnameResolver.getSolrFieldName("/fagperson", "ansattnummer");
  }

  @Override
  public void fireAfterPushElements() {
    try {
      pushFagpersonsToSolr(SolrUpdaterImpl.getContext().getLanguage(), fagpersons);
    } catch(Exception e) {
      LOG.warn(null, e);
    }
  }

  @Override
  public void cleanup() {
    fagpersons.clear();
  }

  private void pushFagpersonsToSolr(String language, Map<Integer, Fagperson> fagpersons) throws Exception {
    for (Entry<Integer, Fagperson> entry : fagpersons.entrySet()) {
      SolrInputDocument doc = new SolrInputDocument();
      doc.addField("id", "fagperson_"+entry.getKey());
      doc.addField("cat", "fagperson");
      SolrUpdaterImpl.addFieldToDoc(doc, entry.getKey(), fPersonId);
      SolrUpdaterImpl.addFieldToDoc(doc, entry.getValue().getPersonnavn().getFornavn(), fFornavn);
      SolrUpdaterImpl.addFieldToDoc(doc, entry.getValue().getPersonnavn().getEtternavn(), fEtternavn);
      SolrUpdaterImpl.addFieldToDoc(doc, getAnsattnummer(entry.getValue().getFnr()), fAnsattnummer);
      updateDocument(language, doc);
    }
  }

  private String getAnsattnummer(String fnr) {
    return employeeNumberResolver.findEmployeeNumber(fnr);
  }

  private void updateDocument(String language, SolrInputDocument doc) throws SolrServerException, IOException {
    solrProxy.updateDocument(language, doc);
  }
}
