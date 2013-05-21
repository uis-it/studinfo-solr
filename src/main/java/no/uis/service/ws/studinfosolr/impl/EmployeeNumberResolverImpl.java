package no.uis.service.ws.studinfosolr.impl;

import no.uis.idm.Affiliation;
import no.uis.idm.IdmWebService;
import no.uis.idm.Person;
import no.uis.service.ws.studinfosolr.EmployeeNumberResolver;

public class EmployeeNumberResolverImpl implements EmployeeNumberResolver {

  private IdmWebService idm;
  
  public void setIdmService(IdmWebService idmService) {
    idm = idmService;
  }
  
  @Override
  public String findEmployeeNumber(String fnr) {
    final Person person = idm.getPersonByNIN(fnr, Affiliation.EMPLOYEE);
    return person.getUserId();
  }

}
