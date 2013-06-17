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

import no.uis.fsws.studinfo.data.Emne;
import no.uis.fsws.studinfo.data.FsSemester;
import no.uis.fsws.studinfo.data.Kurs;
import no.uis.fsws.studinfo.data.Studieprogram;

/**
 * Interface for classes that talk to the Solr server. 
 */
public interface SolrUpdater {

  void pushCourses(List<Kurs> kurs, int year, FsSemester fsSemester, String language, SolrType solrType) throws Exception;

  void pushSubjects(List<Emne> emne, int year, FsSemester fsSemester, String language, SolrType solrType) throws Exception;

  void pushPrograms(List<Studieprogram> studieprogram, int year, FsSemester fsSemester, String language, SolrType solrType) throws Exception;

  void deleteByQuery(String language, String query, SolrType solrType) throws Exception;
}
