package no.uis.service.ws.studinfosolr;

import java.util.List;
import java.util.Map;

public interface SolrUpdateListener<T> {

  void fireBeforeSolrUpdate(T studinfoElement, Map<String, Object> beanmap);

  void fireBeforePushElements(List<T> elements);

  void fireAfterPushElements();

  void cleanup();

}
