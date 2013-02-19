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

  private static AtomicLong sequence = new AtomicLong();

  public void setStudinfoImport(StudInfoImport studinfoImport) {
    this.studinfoImport = studinfoImport;
  }

  public void setSolrUpdater(SolrUpdater solrUpdater) {
    this.solrUpdater = solrUpdater;
  }

  @Override
  public void updateSolrKurs(int year, String semester, String language) throws SolrUpdateException {
    long seq = sequence.incrementAndGet();
    sendNotification(seq, year, semester, language, "KURS-start", null);
    try {
      FsSemester fsSemester = FsSemester.stringToUisSemester(semester);
      FsStudieinfo fsinfo = studinfoImport.fetchCourses(217, year, semester.toString(), language);
      solrUpdater.pushStudieInfo(fsinfo, year, fsSemester, language);
      sendNotification(seq, year, semester, language, "KURS-end", null);
    } catch(Exception e) {
      sendNotification(seq, year, semester, language, "KURS-error", errorToString(e));
      log.error(String.format("updateSolrKurs: %d, %s, %s", year, semester, language), e);
      throw new SolrUpdateException(e);
    }
  }

  @Override
  public void updateSolrEmne(int year, String semester, String language) throws SolrUpdateException {
    long seq = sequence.incrementAndGet();
    sendNotification(seq, year, semester, language, "EMNE-start", null);
    try {
      FsSemester fsSemester = FsSemester.stringToUisSemester(semester);
      FsStudieinfo fsinfo = studinfoImport.fetchSubjects(217, year, fsSemester.toString(), language);
      solrUpdater.pushStudieInfo(fsinfo, year, fsSemester, language);
      sendNotification(seq, year, semester, language, "EMNE-end", null);
    } catch(Exception e) {
      sendNotification(seq, year, semester, language, "EMNE-error", errorToString(e));
      log.error(String.format("updateSolrEmne: %d, %s, %s", year, semester, language), e);
      throw new SolrUpdateException(e);
    }
  }

  @Override
  public void updateSolrStudieprogram(int year, String semester, String language) throws SolrUpdateException {
    long seq = sequence.incrementAndGet();
    sendNotification(seq, year, semester, language, "PROGRAM-start", null);
    try {
      FsSemester fsSemester = FsSemester.stringToUisSemester(semester);

      FsStudieinfo fsinfo = studinfoImport.fetchStudyPrograms(217, year, fsSemester.toString(), true, language);
      solrUpdater.pushStudieInfo(fsinfo, year, fsSemester, language);
      sendNotification(seq, year, semester, language, "PROGRAM-end", null);
    } catch(Exception e) {
      sendNotification(seq, year, semester, language, "PROGRAM-error", errorToString(e));
      log.error(String.format("updateSolrStudieprogram: %d, %s, %s", year, semester, language), e);
      throw new SolrUpdateException(e);
    }
  }

  @Override
  public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
    this.jmxPublisher = notificationPublisher;
  }
  
  private void sendNotification(long seqNo, int year, String semester, String language, String type, Object error) {
    if (this.jmxPublisher == null) {
      return;
    }
    
    String jmxMessage = String.format("%s %d %s %s", type, year, semester, language);
    Notification notification = new Notification("SolrUpdate", this, seqNo, jmxMessage);
    if (error != null) {
      notification.setUserData(error);
    }
    this.jmxPublisher.sendNotification(notification);
  }
  
  private String errorToString(Throwable tr) {
    StringWriter sw = new StringWriter();
    tr.printStackTrace(new PrintWriter(sw));
    
    return sw.toString();
  }
}
