package com.Controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class Githubmcp02 {

	private ChatClient chatclient ; 
	
	public Githubmcp02(ChatClient.Builder builder , ToolCallbackProvider toolcallbackprovider) {
		
		this.chatclient = builder
				.defaultToolCallbacks(toolcallbackprovider)
							.build();
	}
	
	 @GetMapping("/analyze")
	    public String analyze(@RequestParam String repoUrl) {

	        return chatclient.prompt()
	                .user("""
	                    Analyze repository:

	                    %s
	                    """.formatted(repoUrl))
	                .call()
	                .content();
	    }

}
