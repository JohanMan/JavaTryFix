package com.johan.tryfix;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CMD {

	public static void execute(String command, String... args) throws Exception {
		StringBuilder builder = new StringBuilder();
		builder.append(command);
		for (String arg : args) {
			builder.append(" ").append(arg);
		}
		String cmdCommand = builder.toString();
		System.out.println("cmd : " + cmdCommand);
        Process process = Runtime.getRuntime().exec(cmdCommand);
        try {
        	BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        	String line = null;
        	StringBuffer buffer = new StringBuffer();
        	while ((line = reader.readLine()) != null) {
        		buffer.append(line + "\n");
        	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
        process.waitFor();
	}
	
}
