package no.uis.service.ws.studinfosolr;

import java.util.List;

import no.uis.fsws.studinfo.data.Emne;
import no.uis.fsws.studinfo.data.FsSemester;
import no.uis.fsws.studinfo.data.Kurs;
import no.uis.fsws.studinfo.data.Studieprogram;

public interface SolrUpdater {

  void pushCourses(List<Kurs> kurs, int year, FsSemester fsSemester, String language, SolrType solrType) throws Exception;

  void pushSubjects(List<Emne> emne, int year, FsSemester fsSemester, String language, SolrType solrType) throws Exception;

  void pushPrograms(List<Studieprogram> studieprogram, int year, FsSemester fsSemester, String language, SolrType solrType) throws Exception;

  void deleteByQuery(String language, String query, SolrType solrType) throws Exception;
}
