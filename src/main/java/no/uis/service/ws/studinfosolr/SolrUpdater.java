package no.uis.service.ws.studinfosolr;

import no.uis.service.studinfo.data.FsSemester;
import no.uis.service.studinfo.data.FsStudieinfo;

public interface SolrUpdater {

  void pushStudieInfo(FsStudieinfo fsinfo, int year, FsSemester semester, String language) throws Exception;
}
