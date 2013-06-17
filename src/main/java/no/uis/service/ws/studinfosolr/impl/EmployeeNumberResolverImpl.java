/*
 Copyright 2012-2013 University of Stavanger, Norway

 Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package no.uis.service.ws.studinfosolr.impl;

import no.uis.idm.Affiliation;
import no.uis.idm.IdmWebService;
import no.uis.idm.Person;
import no.uis.service.ws.studinfosolr.EmployeeNumberResolver;

/**
 * Resolves the employee's employee number by using the supplied {@link IdmWebService}. 
 */
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
