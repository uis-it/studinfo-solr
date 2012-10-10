package no.uis.service.ws.studinfosolr.impl;

import org.apache.log4j.Logger;

import no.uis.service.fsimport.StudInfoImport;
import no.uis.service.studinfo.data.FsSemester;
import no.uis.service.studinfo.data.FsStudieinfo;
import no.uis.service.ws.studinfosolr.SolrUpdateException;
import no.uis.service.ws.studinfosolr.SolrUpdater;
import no.uis.service.ws.studinfosolr.StudinfoSolrService;

public class StudinfoSolrServiceImpl implements StudinfoSolrService {

  private static Logger log = Logger.getLogger(StudinfoSolrServiceImpl.class);
  private StudInfoImport studinfoImport;
  private SolrUpdater solrUpdater;
  
  public void setStudinfoImport(StudInfoImport studinfoImport) {
    this.studinfoImport = studinfoImport;
  }

  public void setSolrUpdater(SolrUpdater solrUpdater) {
    this.solrUpdater = solrUpdater;
  }

  @Override
  public void updateSolrKurs(int year, String semester, String language) throws SolrUpdateException {
    try {
      FsSemester fsSemester = FsSemester.valueOf(semester);
      FsStudieinfo fsinfo = studinfoImport.fetchCourses(217, year, semester.toString(), language);
      solrUpdater.pushStudieInfo(fsinfo, year, fsSemester, language);
    } catch(Exception e) {
      log.error(String.format("updateSolrKurs: %d, %s, %s", year, semester, language), e);
      throw new SolrUpdateException(e);
    }
  }

  @Override
  public void updateSolrEmne(int year, String semester, String language) throws SolrUpdateException {
    try {
      FsSemester fsSemester = FsSemester.valueOf(semester);
      FsStudieinfo fsinfo = studinfoImport.fetchSubjects(217, year, fsSemester.toString(), language);
      solrUpdater.pushStudieInfo(fsinfo, year, fsSemester, language);
    } catch(Exception e) {
      log.error(String.format("updateSolrKurs: %d, %s, %s", year, semester, language), e);
      throw new SolrUpdateException(e);
    }
  }

  @Override
  public void updateSolrStudieprogram(int year, String semester, String language) throws SolrUpdateException {
    try {
      FsSemester fsSemester = FsSemester.valueOf(semester);
      FsStudieinfo fsinfo = studinfoImport.fetchStudyPrograms(217, year, fsSemester.toString(), true, language);
      solrUpdater.pushStudieInfo(fsinfo, year, fsSemester, language);
    } catch(Exception e) {
      log.error(String.format("updateSolrKurs: %d, %s, %s", year, semester, language), e);
      throw new SolrUpdateException(e);
    }
  }
}
