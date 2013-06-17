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

package no.uis.service.ws.studinfosolr;

import java.util.List;
import java.util.Map;

import no.uis.studinfo.commons.StudinfoContext;

public interface SolrUpdateListener<T> {

  void fireBeforeSolrUpdate(ThreadLocal<StudinfoContext> context, SolrType solrType, T studinfoElement, Map<String, Object> beanmap);

  void fireBeforePushElements(ThreadLocal<StudinfoContext> context, SolrType solrType, List<T> elements);

  void fireAfterPushElements(ThreadLocal<StudinfoContext> context, SolrType solrType);

  void cleanup();
}
