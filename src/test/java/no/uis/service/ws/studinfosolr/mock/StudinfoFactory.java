package no.uis.service.ws.studinfosolr.mock;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import javax.xml.stream.util.StreamReaderDelegate;

import no.uis.service.studinfo.data.FsStudieinfo;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

public class StudinfoFactory implements FactoryBean<FsStudieinfo> {

  private Resource sourceFile;

  @Override
  public FsStudieinfo getObject() throws Exception {
    FsStudieinfo info = unmarshalSource();
    
    return info;
  }

  private FsStudieinfo unmarshalSource() throws IOException, FactoryConfigurationError, XMLStreamException, JAXBException {
    XMLInputFactory xif = XMLInputFactory.newFactory();
    
    XMLStreamReader xsr = xif.createXMLStreamReader(new InputStreamReader(sourceFile.getInputStream()));

    JAXBContext jc = JAXBContext.newInstance(FsStudieinfo.class);
    Unmarshaller um = jc.createUnmarshaller();

    FsStudieinfo info = (FsStudieinfo)um.unmarshal(xsr);
    return info;
  }

  @Override
  public Class<?> getObjectType() {
    return FsStudieinfo.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
  
  public void setSource(Resource src) {
    this.sourceFile = src;
  }
}
