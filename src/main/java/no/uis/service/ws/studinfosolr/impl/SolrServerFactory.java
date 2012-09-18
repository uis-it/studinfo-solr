package no.uis.service.ws.studinfosolr.impl;

import java.net.URL;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

public class SolrServerFactory implements FactoryBean<SolrServer> {

  private URL url;
  private String username;
  private String password;
  private int timeout = 1000;
  private boolean compression = true;
  
  @Required
  public void setUrl(URL url) {
    this.url = url;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setReadTimeout(int timeout) {
    this.timeout = timeout;
  }
  
  public void setEnableCompression(boolean compression) {
    this.compression = compression;
  }
  
  @Override
  public SolrServer getObject() {
    return createServer();
  }

  private SolrServer createServer() {
    HttpClient httpClient = (username == null ? null : new PreemptBasicAuthHttpClient(url, username, password));
    HttpSolrServer server = new HttpSolrServer(url.toExternalForm(), httpClient, new XMLResponseParser());
    
    server.setSoTimeout(timeout);
    server.setAllowCompression(compression);
    
    return server;
  }

  @Override
  public Class<?> getObjectType() {
    return SolrServer.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

}
