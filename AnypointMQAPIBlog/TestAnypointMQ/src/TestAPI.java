import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;




public class TestAPI {
	public static void main(String[] args){
	try {
		 
		String client_id="85xxxxxxxxxxxxxxxxxxxxxxxxxxx8fd";
		String client_secret="2B7xxxxxxxxxxxxxxxxxxxxxxxxxxx3b1";
		String queueName="myQueue";
		String queueMessage="This is my message 10";
		String authURI="https://mq-us-west-2.anypoint.mulesoft.com/api/v1/authorize";
		String brokerURI="https://mq-us-west-2.anypoint.mulesoft.com/api/v1/organizations/";
	
		// Get access token required for publish/subscribe API's
		
		Client client = Client.create();
		
		WebResource webResource = client.resource(authURI);
		String input = "client_id="+client_id+"&client_secret="+client_secret+"&grant_type=client_credentials";		
		ClientResponse response = webResource.header("Content-Type", "application/x-www-form-urlencoded")
											 .accept("application/json")
											 .post(ClientResponse.class, input);
		
		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		String authResponse = response.getEntity(String.class);
		System.out.println("\n============Platform Reponse============");
		System.out.println(authResponse);
		
		JSONParser jsonParser=new JSONParser();
		JSONObject jsonObject= (JSONObject) jsonParser.parse(authResponse);
		String access_token=(String)jsonObject.get("access_token");
		
		JSONObject structure = (JSONObject) jsonObject.get("simple_client");
		String orgId=(String)structure.get("orgId");
		String envId=(String)structure.get("envId");
		System.out.println("Access Token:"+access_token);
		System.out.println("Organization Id:"+orgId);
		System.out.println("EnvironmentId:"+envId);
		
		brokerURI = brokerURI + orgId +"/environments/"+ envId +"/destinations/"+ queueName+"/messages";
		
		// Post message to existing queue using API
		String messageId=UUID.randomUUID().toString();
		String postURI=brokerURI+"/"+messageId;
		System.out.println(postURI);
		
		WebResource webResourcePost = client.resource(postURI);

		String inputPost="{ \"body\": \""+queueMessage+"\" }";
		ClientResponse responsePost=webResourcePost.header("Authorization"," bearer" + access_token)
												   .type("application/json")
												   .accept("application/json")
												   .put(ClientResponse.class, inputPost);
		
		String outputPost = responsePost.getEntity(String.class);
		System.out.println("Post Successful: \n"+outputPost);

		// Get message from existing queue using API
		WebResource webResourceGet = client.resource(brokerURI);
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		queryParams.add("poolingTime","10000");
		queryParams.add("batchSize","1");
		queryParams.add("lockTtl","10000");
		
		ClientResponse responseGet=webResourceGet.queryParams(queryParams)
												.header("Authorization"," bearer" + access_token)
												.type("application/json").accept("application/json").get(ClientResponse.class);
		
		String outputGet = responseGet.getEntity(String.class);
		System.out.println("Message Retrieved: \n" + outputGet);
		
		// Message acknowledgement, effectively deleting message from the queue
		
		JSONArray jsonArrayAck= (JSONArray) jsonParser.parse(outputGet);
		JSONObject structureAck =(JSONObject) ((JSONObject)jsonArrayAck.get(0)).get("headers");
		String lockId= (String)structureAck.get("lockId");

		WebResource webResourceAck = client.resource(postURI);
		String inputAck="{ \"lockId\": \""+lockId+"\" }";
		
		ClientResponse responseAck=webResourceAck.header("Authorization"," bearer" + access_token)
												 .type("application/json")
												 .accept("application/json")
												 .delete(ClientResponse.class, inputAck);

		
		String outputAck = responseAck.getEntity(String.class);
		System.out.println("Ack Message: \n" + outputAck);

	} catch (Exception e) {
		e.printStackTrace();
	}
	}
}

