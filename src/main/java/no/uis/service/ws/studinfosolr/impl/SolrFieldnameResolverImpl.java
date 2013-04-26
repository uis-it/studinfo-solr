package no.uis.service.ws.studinfosolr.impl;

import java.util.Map;

import no.uis.service.ws.studinfosolr.SolrFieldnameResolver;

public class SolrFieldnameResolverImpl implements SolrFieldnameResolver {

  private Map<String, String> solrFieldMapping;

  public void setSolrFieldMapping(Map<String, String> mapping) {
    this.solrFieldMapping = mapping;
  }

  @Override
  public String getSolrFieldName(String path, String propName) {
    String solrFieldName = solrFieldMapping.get(path + '/' + propName);
    if (solrFieldName == null) {
      solrFieldName = solrFieldMapping.get(propName);
      if (solrFieldName == null) {
        solrFieldName = propName + "_s";
      }
    }
    if (solrFieldName.isEmpty()) {
      solrFieldName = null;
    }
    return solrFieldName;
  }

}
