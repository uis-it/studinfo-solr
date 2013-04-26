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
