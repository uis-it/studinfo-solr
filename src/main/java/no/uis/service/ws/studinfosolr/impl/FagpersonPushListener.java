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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import no.uis.service.ws.studinfosolr.EmployeeNumberResolver;
import no.uis.service.ws.studinfosolr.SolrFieldnameResolver;
import no.uis.service.ws.studinfosolr.SolrProxy;
import no.uis.service.ws.studinfosolr.SolrType;
import no.uis.service.ws.studinfosolr.SolrUpdateListener;
import no.uis.studinfo.commons.StudinfoContext;
import no.usit.fsws.schemas.studinfo.Emne;
import no.usit.fsws.schemas.studinfo.Fagperson;
import no.usit.fsws.schemas.studinfo.Personnavn;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.dao.DataAccessException;

/**
 * Collects a courses staff, supplementing it with the employee id and writes it as separate documents to Solr. 
 */
public class FagpersonPushListener implements SolrUpdateListener<Emne> {

  private static final String FAGPERSON_ROOT = "/fagperson";

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
  public void fireBeforeSolrUpdate(ThreadLocal<StudinfoContext> context, SolrType solrType, Emne emne, Map<String, Object> beanmap) {
    if (emne.isSetFagpersonListe() && emne.getFagpersonListe().isSetFagperson()) {
      for (Fagperson fagPerson : emne.getFagpersonListe().getFagperson()) {
        fagpersons.put(fagPerson.getPersonid().intValue(), fagPerson);
      }
    }
  }

  @Override
  public void fireBeforePushElements(ThreadLocal<StudinfoContext> context, SolrType solrType, List<Emne> elements) {
    fPersonId = solrFieldnameResolver.getSolrFieldName(FAGPERSON_ROOT, "personid");
    fFornavn = solrFieldnameResolver.getSolrFieldName(FAGPERSON_ROOT, "fornavn");
    fEtternavn = solrFieldnameResolver.getSolrFieldName(FAGPERSON_ROOT, "etternavn");
    fAnsattnummer = solrFieldnameResolver.getSolrFieldName(FAGPERSON_ROOT, "ansattnummer");
  }

  @Override
  public void fireAfterPushElements(ThreadLocal<StudinfoContext> context, SolrType solrType) {
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
      SolrUpdaterImpl.addFieldToDoc(doc, getAnsattnummer(entry.getValue()), fAnsattnummer);
      updateDocument(solrType, language, doc);
    }
  }

  private String getAnsattnummer(Fagperson person) {
    try {
      return employeeNumberResolver.findEmployeeNumber(person.getFnr());
    } catch(DataAccessException e) { // TODO dependency on Spring framework, is this good?
      StringBuilder sb = new StringBuilder();
      sb.append(e.getMessage());
      sb.append(": ");
      final Personnavn pn = person.getPersonnavn();
      sb.append(pn.getFornavn());
      sb.append(' ');
      sb.append(pn.getEtternavn());
      LOG.warn(sb.toString());
      return null;
    }
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
