package no.uis.service.ws.studinfosolr.impl;

import no.uis.service.idm.ws.IdmWebService;
import no.uis.service.model.Person;
import no.uis.service.ws.studinfosolr.EmployeeNumberResolver;

public class EmployeeNumberResolverImpl implements EmployeeNumberResolver {

  private IdmWebService idm;
  
  public void setIdmService(IdmWebService idmService) {
    idm = idmService;
  }
  
  @Override
  public String findEmployeeNumber(String fnr) {
    final Person person = idm.getPersonByNIN(fnr);
    return person.getUserId();
  }

}
