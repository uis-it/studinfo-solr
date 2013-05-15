package no.uis.service.ws.studinfosolr;

import java.util.List;
import java.util.Map;

public interface SolrUpdateListener<T> {

  void fireBeforeSolrUpdate(SolrType solrType, T studinfoElement, Map<String, Object> beanmap);

  void fireBeforePushElements(SolrType solrType, List<T> elements);

  void fireAfterPushElements(SolrType solrType);

  void cleanup();
}
