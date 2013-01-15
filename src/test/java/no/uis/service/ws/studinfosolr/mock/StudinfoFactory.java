package no.uis.service.ws.studinfosolr.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import no.usit.fsws.wsdl.studinfo.StudInfoService;

import org.apache.cxf.helpers.IOUtils;
import org.junit.Assume;

public class StudinfoFactory implements StudInfoService {

  private File programSource;
  private File emneSource;
  private File kursSource;

  public void setProgramSource(File programSource) {
    this.programSource = programSource;
  }

  public void setEmneSource(File emneSource) {
    this.emneSource = emneSource;
  }
  
  public void setKursSource(File kursSource) {
    this.kursSource = kursSource;
  }

  @Override
  public String getEksamenSI(Integer institusjonsnr, Integer faknr,
      Integer instituttnr,
      Integer gruppenr,
      Integer arstall,
      String terminkode,
      String sprak)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getStudieprogramSI(Integer arstall,
      String terminkode,
      Integer medUPinfo,
      String studieprogramkode,
      Integer institusjonsnr,
      Integer faknr,
      Integer instituttnr,
      Integer gruppenr,
      String sprak)
  {
    try {
      return IOUtils.toString(new FileInputStream(programSource), "UTF-8");
    } catch(IOException e) {
      Assume.assumeNoException(e);
    }
    return null;
  }

  @Override
  public String getKodeSI(Integer arstall, String terminkode, String sprak)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getEmneSI(Integer institusjonsnr,
      String emnekode,
      String versjonskode,
      Integer faknr,
      Integer instituttnr,
      Integer gruppenr,
      Integer arstall,
      String terminkode,
      String sprak)
  {
    try {
      return IOUtils.toString(new FileInputStream(emneSource), "UTF-8");
    } catch(IOException e) {
    	Assume.assumeNoException(e);
    }
    return null;
  }

  @Override
  public String getFagpersonSI(Integer arstall,
      String terminkode,
      Integer fodselsdato,
      Integer personnr,
      Integer institusjonsnr,
      Integer faknr,
      Integer instituttnr,
      Integer gruppenr,
      String sprak)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getStudieretningSI(Integer arstall,
      String terminkode,
      String studieretningkode,
      Integer institusjonsnr,
      Integer faknr,
      Integer instituttnr,
      Integer gruppenr,
      String sprak)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getStedSI(Integer institusjonsnr,
      Integer faknr,
      Integer instituttnr,
      Integer gruppenr,
      Integer arstall,
      String terminkode,
      String sprak)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getKursSI(Integer institusjonsnr,
      Integer faknr,
      Integer instituttnr,
      Integer gruppenr,
      String sprak)
  {
    try {
      return IOUtils.toString(new FileInputStream(kursSource), "UTF-8");
    } catch(IOException e) {
      Assume.assumeNoException(e);
    }
    return null;
  }

  @Override
  public String getUndervisningSI(Integer institusjonsnr,
      Integer faknr,
      Integer instituttnr,
      Integer gruppenr,
      Integer arstall,
      String terminkode,
      String emnekode,
      String versjonskode,
      Integer fraStudieNiva,
      Integer tilStudieNiva,
      String sprak)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
