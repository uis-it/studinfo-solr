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
