/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.rest.api.task;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.rest.api.ActivitiUtil;
import org.activiti.rest.api.SecuredResource;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.restlet.representation.Representation;
import org.restlet.resource.Put;

/**
 * @author Tijs Rademakers
 */
public class TaskOperationResource extends SecuredResource {
  
  @Put
  public ObjectNode executeTaskOperation(Representation entity) {
    if(authenticate() == false) return null;
    
    String effectiveUser = (String) getRequest().getAttributes().get("effectiveUser");
    if(authenticate(SecuredResource.ADMIN) == true && effectiveUser != null){
    	Authentication.setAuthenticatedUserId(effectiveUser);
    	super.loggedInUser = effectiveUser;
    }
    
    String taskId = (String) getRequest().getAttributes().get("taskId");
    String operation = (String) getRequest().getAttributes().get("operation");
    try {
      String startParams = entity.getText();
      JsonNode startJSON = new ObjectMapper().readTree(startParams);
      Iterator<String> itName = startJSON.getFieldNames();
      Map<String, String> properties = new HashMap<String, String>();
      while(itName.hasNext()) {
        String name = itName.next();
        JsonNode valueNode = startJSON.path(name);
        properties.put(name, valueNode.getTextValue());
      }
      
      if ("claim".equals(operation)) {
        ActivitiUtil.getTaskService().claim(taskId, loggedInUser);
      } else if ("complete".equals(operation)) {
        properties.remove("taskId");
        ActivitiUtil.getFormService().submitTaskFormData(taskId, properties);
      }else if ("reassign".equals(operation)){
      	String userId = properties.get("userId");
      	ActivitiUtil.getTaskService().setAssignee(taskId, userId);
      }
      else {
        throw new ActivitiException("'" + operation + "' is not a valid operation");
      }
      
    } catch(Exception e) {
      throw new ActivitiException("Did not receive the operation parameters", e);
    }
    
    ObjectNode successNode = new ObjectMapper().createObjectNode();
    successNode.put("success", true);
    return successNode;
  }
}
