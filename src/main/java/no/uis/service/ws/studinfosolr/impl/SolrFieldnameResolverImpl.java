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
