package no.uis.service.ws.studinfosolr.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import no.uis.service.ws.studinfosolr.SolrProxy;
import no.uis.service.ws.studinfosolr.util.SolrUtil;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

public class SolrProxyImpl implements SolrProxy {

  private static final int COMMIT_WITHIN = 3000;

  private Map<String, SolrServer> solrServers;
  
  public void setSolrServers(Map<String, SolrServer> solrServers) {
    this.solrServers = solrServers;
  }

  @Override
  public void deleteByQuery(String language, String query) throws SolrServerException, IOException {
    SolrServer solrServer = getSolrServer(language);
    solrServer.deleteByQuery(query, COMMIT_WITHIN);
  }

  @Override
  public void updateDocument(String lang, SolrInputDocument doc) throws SolrServerException, IOException {
    SolrServer solrServer = getSolrServer(lang);
    doc.addField("timestamp", SolrUtil.solrDateNow());
    solrServer.add(doc, COMMIT_WITHIN);
  }

  @Override
  public Map<String, SolrServer> getSolrServers() {
    return Collections.unmodifiableMap(solrServers);
  }
  
  @Override
  public void removeServer(String lang) {
    solrServers.remove(lang);
  }

  @Override
  public void setServer(String lang, SolrServer server) {
    solrServers.put(lang, server);
  }

  private SolrServer getSolrServer(String sprak) {

    return solrServers.get(sprak.substring(0, 1));
  }
}
