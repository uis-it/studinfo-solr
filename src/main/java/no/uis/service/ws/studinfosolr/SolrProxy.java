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

package no.uis.service.ws.studinfosolr;

import java.io.IOException;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

public interface SolrProxy {

  void deleteByQuery(String language, String query) throws SolrServerException, IOException;

  void updateDocument(String lang, SolrInputDocument doc) throws SolrServerException, IOException;

  Map<String, SolrServer> getSolrServers();

  void removeServer(String lang);

  void setServer(String lang, SolrServer object);

}
