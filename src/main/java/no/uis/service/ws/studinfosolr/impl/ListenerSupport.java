package no.uis.service.ws.studinfosolr.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import no.uis.service.ws.studinfosolr.SolrUpdateListener;

public class ListenerSupport<T> {
  
  private List<SolrUpdateListener<T>> listeners;

  public ListenerSupport() {
  }

  public void fireBeforeSolrUpdate(T studinfoElement, Map<String, Object> beanmap) {
    for (SolrUpdateListener<T> listener : getListeners()) {
      listener.fireBeforeSolrUpdate(studinfoElement, beanmap);
    }
  }

  public void fireBeforePushElements(List<T> elements) {
    for (SolrUpdateListener<T> listener : getListeners()) {
      listener.fireBeforePushElements(elements);
    }
  }

  public void fireAfterPushElements() {
    for (SolrUpdateListener<T> listener : getListeners()) {
      listener.fireAfterPushElements();
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
