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

import javax.jws.WebParam;
import javax.jws.WebService;

/**
 * Web service interface for Updating a Solr core with studinfo from FS.
 * @author 2904630
 *
 */
@WebService(targetNamespace = "http://studinfosolr.ws.service.uis.no/")
public interface StudinfoSolrService {
  
  void updateSolrKurs(
      @WebParam(name = "year") int year, 
      @WebParam(name = "semester") String semester, 
      @WebParam(name = "language") String language,
      @WebParam(name = "solrType") SolrType solrType) throws SolrUpdateException;

  void updateSolrEmne(
      @WebParam(name = "faculty") int[] faculties,
      @WebParam(name = "year") int year, 
      @WebParam(name = "semester") String semester, 
      @WebParam(name = "language") String language,
      @WebParam(name = "solrType") SolrType solrType) throws SolrUpdateException;

  void updateSolrStudieprogram(
      @WebParam(name = "faculty") int[] faculties,
      @WebParam(name = "year") int year, 
      @WebParam(name = "semester") String semester, 
      @WebParam(name = "language") String language,
      @WebParam(name = "solrType") SolrType solrType) throws SolrUpdateException;
}
