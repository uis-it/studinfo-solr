package no.uis.service.component.studinfosolr.impl;

import java.net.URL;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

public class SolrServerFactory implements FactoryBean<SolrServer> {

  private String url;
  private String username;
  private String password;
  private int timeout = 1000;
  private boolean compression = true;
  
  @Required
  public void setUrl(String url) {
    this.url = url;
  }

  @Required
  public void setUsername(String username) {
    this.username = username;
  }

  @Required
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
  public SolrServer getObject() throws Exception {
    
    return createServer();
  }

  private SolrServer createServer() throws Exception {
    HttpClient httpClient = new PreemptBasicAuthHttpClient(new URL(url), username, password);
    HttpSolrServer server = new HttpSolrServer(url, httpClient, new XMLResponseParser());
    
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
