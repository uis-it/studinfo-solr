package no.uis.service.component.studinfosolr;

import no.uis.service.studinfo.data.FsSemester;
import no.uis.service.studinfo.data.FsStudieinfo;

public interface SolrUpdater {

  void pushStudieInfo(FsStudieinfo info, int year, FsSemester semester, String language) throws Exception;
}
