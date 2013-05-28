package no.uis.service.ws.studinfosolr;

import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(targetNamespace="http://studinfosolr.ws.service.uis.no/")
public interface StudinfoSolrService {
  
  void updateSolrKurs(
      @WebParam(name="year") int year, 
      @WebParam(name="semester") String semester, 
      @WebParam(name="language") String language,
      @WebParam(name="solrType") SolrType solrType) throws SolrUpdateException;

  void updateSolrEmne(
      @WebParam(name="faculties") int[] faculties,
      @WebParam(name="year") int year, 
      @WebParam(name="semester") String semester, 
      @WebParam(name="language") String language,
      @WebParam(name="solrType") SolrType solrType) throws SolrUpdateException;

  void updateSolrStudieprogram(
      @WebParam(name="faculties") int[] faculties,
      @WebParam(name="year") int year, 
      @WebParam(name="semester") String semester, 
      @WebParam(name="language") String language,
      @WebParam(name="solrType") SolrType solrType) throws SolrUpdateException;
}
