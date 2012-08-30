package no.uis.service.component.studinfosolr.impl;

import java.net.URL;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

class PreemptBasicAuthHttpClient extends DefaultHttpClient {
  
  private URL url;
  
  public PreemptBasicAuthHttpClient(URL url, String username, String password) {
    this.url = url;
    
    BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(new AuthScope(url.getHost(), AuthScope.ANY_PORT), new UsernamePasswordCredentials(username, password));
    setCredentialsProvider(credsProvider);
  }
  
  @Override
  protected HttpContext createHttpContext() {
    HttpContext context = super.createHttpContext();
    
    AuthCache authCache = new BasicAuthCache();
  
    BasicScheme basicAuth = new BasicScheme();
    HttpHost targetHost = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
    authCache.put(targetHost, basicAuth);
  
    context.setAttribute(ClientContext.AUTH_CACHE, authCache);
  
    return context;
  }
}