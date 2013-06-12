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

import java.net.URL;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

public class SolrServerFactory implements FactoryBean<SolrServer> {

  private static final int QUEUE_SIZE = 3;
  private static final int THREAD_COUNT = 2;
  private URL url;
  private String username;
  private String password;
  private int timeout = 1000;
  
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
  
  @Override
  public SolrServer getObject() {
    return createServer();
  }

  private SolrServer createServer() {
    HttpClient httpClient = username == null ? null : new PreemptBasicAuthHttpClient(url, username, password);
    MySolrServer server = new MySolrServer(url.toExternalForm(), httpClient, QUEUE_SIZE, THREAD_COUNT);
    
    server.setSoTimeout(timeout);
    
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
  
  /**
   * Interface to get the server's base URL, which is not exposed by {@link ConcurrentUpdateSolrServer}.
   */
  public interface SolrServerInfo {
    String getBaseURL();
  }

  /**
   * Previously we used HttpSolrServer which exposes the baseUrl.
   * ConcurrentUpdateSolrServer does not. So we use a common interface to get this information.
   * HttpSolrServer exposes the baseURL directly.
   */
  private static class MySolrServer extends ConcurrentUpdateSolrServer implements SolrServerInfo {

    private static final long serialVersionUID = 1L;
    private final String baseUrl;
    
    public MySolrServer(String baseUrl, HttpClient client, int queueSize, int threadCount) {
      super(baseUrl, client, queueSize, threadCount);
      this.baseUrl = baseUrl;
    }
    
    @Override
    public String getBaseURL() {
      return this.baseUrl;
    }
  }
}
