package com.Controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Testcontroller {

	private ChatClient chatclient ;
	public Testcontroller(ChatClient.Builder builder) {
		this.chatclient = builder.build();
	}
	
	 @GetMapping("/ask")
	    public String ask(@RequestParam String message) {
	        return chatclient.prompt(message)
	                .call()
	                .content();
	    }

}
