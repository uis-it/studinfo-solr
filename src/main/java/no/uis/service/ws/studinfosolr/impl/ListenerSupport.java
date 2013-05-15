package no.uis.service.ws.studinfosolr.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import no.uis.service.ws.studinfosolr.SolrType;
import no.uis.service.ws.studinfosolr.SolrUpdateListener;

public class ListenerSupport<T> {
  
  private List<SolrUpdateListener<T>> listeners;

  public ListenerSupport() {
  }

  public void fireBeforeSolrUpdate(SolrType solrType, T studinfoElement, Map<String, Object> beanmap) {
    for (SolrUpdateListener<T> listener : getListeners()) {
      listener.fireBeforeSolrUpdate(solrType, studinfoElement, beanmap);
    }
  }

  public void fireBeforePushElements(SolrType solrType, List<T> elements) {
    for (SolrUpdateListener<T> listener : getListeners()) {
      listener.fireBeforePushElements(solrType, elements);
    }
  }

  public void fireAfterPushElements(SolrType solrType) {
    for (SolrUpdateListener<T> listener : getListeners()) {
      listener.fireAfterPushElements(solrType);
    }
  }

  public void cleanup() {
    for (SolrUpdateListener<T> listener : getListeners()) {
      listener.cleanup();
    }
  }

  private List<SolrUpdateListener<T>> getListeners() {
    synchronized(this) {
      if (listeners != null) {
        return listeners;
      } else {
        return Collections.emptyList();
      }
    }
  }
  
  public void setListeners(List<SolrUpdateListener<T>> l) {
    synchronized(this) {
      this.listeners = l; 
    }
  }
}
