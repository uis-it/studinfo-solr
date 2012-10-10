package no.uis.service.ws.studinfosolr;

import javax.jws.WebParam;
import javax.jws.WebService;

@WebService
public interface StudinfoSolrService {

  void updateSolrKurs(
      @WebParam(name="year") int year, 
      @WebParam(name="semester") String semester, 
      @WebParam(name="language") String language) throws SolrUpdateException;

  void updateSolrEmne(
      @WebParam(name="year") int year, 
      @WebParam(name="semester") String semester, 
      @WebParam(name="language") String language) throws SolrUpdateException;

  void updateSolrStudieprogram(
      @WebParam(name="year") int year, 
      @WebParam(name="semester") String semester, 
      @WebParam(name="language") String language) throws SolrUpdateException;
}
