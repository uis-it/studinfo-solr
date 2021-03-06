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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import no.uis.service.ws.studinfosolr.SolrType;
import no.uis.service.ws.studinfosolr.SolrUpdateListener;
import no.uis.studinfo.commons.StudinfoContext;

/**
 * Support class for {@link SolrUpdateListener SolrUpdateListeners}.
 * 
 * @param <T>
 *        either {@link no.uis.fsws.studinfo.data.Studieprogram Studieprogram}, {@link no.uis.fsws.studinfo.data.Emne Emne} or
 *        {@link no.uis.fsws.studinfo.data.Kurs Kurs}
 */
public class ListenerSupport<T> {

  private List<SolrUpdateListener<T>> listeners;

  public ListenerSupport() {
  }

  public void fireBeforeSolrUpdate(ThreadLocal<StudinfoContext> context, SolrType solrType, T studinfoElement,
      Map<String, Object> beanmap)
  {
    for (SolrUpdateListener<T> listener : getListeners()) {
      listener.fireBeforeSolrUpdate(context, solrType, studinfoElement, beanmap);
    }
  }

  public void fireBeforePushElements(ThreadLocal<StudinfoContext> context, SolrType solrType, List<T> elements) {
    for (SolrUpdateListener<T> listener : getListeners()) {
      listener.fireBeforePushElements(context, solrType, elements);
    }
  }

  public void fireAfterPushElements(ThreadLocal<StudinfoContext> context, SolrType solrType) {
    for (SolrUpdateListener<T> listener : getListeners()) {
      listener.fireAfterPushElements(context, solrType);
    }
  }

  public void cleanup() {
    for (SolrUpdateListener<T> listener : getListeners()) {
      listener.cleanup();
    }
  }

  private List<SolrUpdateListener<T>> getListeners() {
    synchronized (this) {
      if (listeners != null) {
        return listeners;
      } else {
        return Collections.emptyList();
      }
    }
  }

  public void setListeners(List<SolrUpdateListener<T>> l) {
    synchronized (this) {
      this.listeners = l;
    }
  }
}
