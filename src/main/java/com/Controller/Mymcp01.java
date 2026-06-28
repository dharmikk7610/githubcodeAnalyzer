package com.Controller;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Mymcp01 {

	private ChatClient chatclient ;
	
	public Mymcp01(ChatClient.Builder builder,
            ToolCallbackProvider toolcallbackprovider) {

 System.out.println("Tool count = "
         + toolcallbackprovider.getToolCallbacks().length);

 this.chatclient = builder
         .defaultToolCallbacks(toolcallbackprovider)
         .build();
}
	    
	   
		@GetMapping("/getaskme")
	    public String ask(@RequestParam String prompt) {
	        return chatclient.prompt()
	                .user(prompt)
	                .call()
	                .content();
	        
	    }
	

}
