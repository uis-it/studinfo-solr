package no.uis.service.ws.studinfosolr.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;

import no.uis.service.fsimport.StudInfoImport;
import no.uis.service.studinfo.data.FsSemester;
import no.uis.service.studinfo.data.FsStudieinfo;
import no.uis.service.ws.studinfosolr.SolrUpdateException;
import no.uis.service.ws.studinfosolr.SolrUpdater;
import no.uis.service.ws.studinfosolr.StudinfoSolrService;

import org.apache.log4j.Logger;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;

@ManagedResource(
  objectName="uis:service=ws-studinfo-solr,component=webservice",
  description="StudinfoSolrService",
  log=false
)
public class StudinfoSolrServiceImpl implements StudinfoSolrService, NotificationPublisherAware {

  private static Logger log = Logger.getLogger(StudinfoSolrServiceImpl.class);
  private StudInfoImport studinfoImport;
  private SolrUpdater solrUpdater;
  private NotificationPublisher jmxPublisher;
  private int[] faculties = {-1};

  private static AtomicLong sequence = new AtomicLong();

  public void setStudinfoImport(StudInfoImport studinfoImport) {
    this.studinfoImport = studinfoImport;
  }

  public void setSolrUpdater(SolrUpdater solrUpdater) {
    this.solrUpdater = solrUpdater;
  }

  public void setFaculties(String facultiesString) {
    int[] newFaculties = null;
    if (faculties != null && !facultiesString.trim().isEmpty()) {
      String[] tokens = facultiesString.split("\\s*,\\s*");
      newFaculties = new int[tokens.length];
      for (int i = 0; i < tokens.length; i++) {
        newFaculties[i] = Integer.parseInt(tokens[i]);
      }
    }
    if (newFaculties == null) {
      faculties = new int[] {-1};
    } else {
      faculties = newFaculties;
    }
  }
  
  @Override
  public void updateSolrKurs(int year, String semester, String language) throws SolrUpdateException {
    long seq = sequence.incrementAndGet();
    sendNotification(seq, null, year, semester, language, "KURS-start");
    try {
      FsSemester fsSemester = FsSemester.stringToUisSemester(semester);
      FsStudieinfo fsinfo = studinfoImport.fetchCourses(217, year, semester.toString(), language);
      solrUpdater.pushCourses(fsinfo.getKurs(), year, fsSemester, language);
      sendNotification(seq, null, year, semester, language, "KURS-end");
    } catch(Exception e) {
      sendNotification(seq, e, year, semester, language, "KURS-error");
      log.error(String.format("updateSolrKurs: %d, %s, %s", year, semester, language), e);
      throw new SolrUpdateException(e);
    }
  }

  @Override
  public void updateSolrEmne(int year, String semester, String language) throws SolrUpdateException {
    for (int faculty : faculties) {
      updateSolrEmne(faculty, year, semester, language);
    }
  }
  
  private void updateSolrEmne(int faculty, int year, String semester, String language) throws SolrUpdateException {

    long seq = sequence.incrementAndGet();
    sendNotification(seq, null, faculty, year, semester, language, "EMNE-start");
    try {
      FsSemester fsSemester = FsSemester.stringToUisSemester(semester);
      FsStudieinfo fsinfo = studinfoImport.fetchSubjects(217, faculty, year, fsSemester.toString(), language);
      solrUpdater.pushSubjects(fsinfo.getEmne(), year, fsSemester, language);
      sendNotification(seq, null, faculty, year, semester, language, "EMNE-end");
    } catch(Exception e) {
      sendNotification(seq, e, faculty, year, semester, language, "EMNE-error");
      log.error(String.format("updateSolrEmne(%d): %d, %s, %s", faculty, year, semester, language), e);
      throw new SolrUpdateException(e);
    }
  }

  @Override
  public void updateSolrStudieprogram(int year, String semester, String language) throws SolrUpdateException {
    for (int faculty : faculties) {
      updateSolrStudieprogram(faculty, year, semester, language);
    }
  }
  
  private void updateSolrStudieprogram(int faculty, int year, String semester, String language) throws SolrUpdateException {

    long seq = sequence.incrementAndGet();
    sendNotification(seq, null, faculty, year, semester, language, "PROGRAM-start");
    try {
      FsSemester fsSemester = FsSemester.stringToUisSemester(semester);

      FsStudieinfo fsinfo = studinfoImport.fetchStudyPrograms(217, faculty, year, fsSemester.toString(), true, language);
      solrUpdater.pushPrograms(fsinfo.getStudieprogram(), year, fsSemester, language);
      sendNotification(seq, null, faculty, year, semester, language, "PROGRAM-end");
    } catch(Exception e) {
      sendNotification(seq, e, faculty, year, semester, language, "PROGRAM-error");
      log.error(String.format("updateSolrStudieprogram (%d): %d, %s, %s", faculty, year, semester, language), e);
      throw new SolrUpdateException(e);
    }
  }

  @Override
  public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
    this.jmxPublisher = notificationPublisher;
  }
  
  private void sendNotification(long seqNo, Throwable error, Object... args) {
    if (this.jmxPublisher == null) {
      return;
    }

    StringBuilder sb = new StringBuilder();
    for (Object arg : args) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(arg);
    }
    Notification notification = new Notification("PDFUpdate", this, seqNo, sb.toString());
    if (error != null) {
      notification.setUserData(errorToString(error));
    }
    this.jmxPublisher.sendNotification(notification);
  }

  private String errorToString(Throwable tr) {
    StringWriter sw = new StringWriter();
    tr.printStackTrace(new PrintWriter(sw));

    return sw.toString();
  }
}
