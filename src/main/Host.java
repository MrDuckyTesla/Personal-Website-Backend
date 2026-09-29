package main;

import java.io.IOException;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Host implements AutoCloseable {
	
	private final ChunkedDB<User> users;
	private final ChunkedDB<Post> posts;
	private final ChunkedDB<User> verify;
	private final ChunkedDB<State> states;
	private final HttpServer server;

	public Host(int port) throws IOException {
		User proto = new User("", "");
		this.users = new ChunkedDB<>(128, "data/users/", proto);
		this.posts = new ChunkedDB<>(512, "data/posts/", new Post(proto, "", ""));
		this.verify = new ChunkedDB<>(64, "data/verify/users/", proto);
		this.states = new ChunkedDB<>(64, "data/verify/states/", new State(proto), proto);
		this.server = HttpServer.create(new InetSocketAddress(port), 0);
		
		this.server.createContext("/api/test", exchange -> {
			String response = "hello world";
			byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
			
			exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
			exchange.sendResponseHeaders(200, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		
		this.server.createContext("/api/users", exchange -> {
			
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); 
			if (body.length() == 0) {body = exchange.getRequestURI().getQuery();}
			Map<String, String> params = this.getParams(body);
			String response = ""; int code = 409; User user;
			
			if (this.verifyReal(params.get("real"))) {
				if (!this.userExists(params) && params.containsKey("username") && params.containsKey("password")) {
					user = new User(params.get("username"), params.get("password"));
					this.verify.put(params.get("username"), user);
					State state = new State(user); this.states.put(state.getID(), state);
					response = state.getID(); code = 200;
				}
			}
			
			byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
			exchange.sendResponseHeaders(code, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
			
		});
		
		this.server.createContext("/api/github", exchange -> {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); 
			if (body.length() == 0) {body = exchange.getRequestURI().getQuery();}
			Map<String, String> params = this.getParams(body); 
			User user; int code = 500;
			
			try {
				String githubCode = params.get("code");
				State state = this.states.get(params.get("state"));
				
				user = state.getUser();
				
				String clientID = "Ov23liQOvNxULB7rtaVH";
				String clientSecret = new String(Files.readAllBytes(Paths.get("gitsecret.txt")), StandardCharsets.UTF_8);
				
				String bodyGit =
					"client_id=" + URLEncoder.encode(clientID, StandardCharsets.UTF_8) +
					"&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
					"&code=" + URLEncoder.encode(githubCode, StandardCharsets.UTF_8);
				
				HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://github.com/login/oauth/access_token"))
					.header("Accept", "application/json")
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(bodyGit))
					.build();
				
				HttpClient client = HttpClient.newHttpClient();
				
				try {
					HttpResponse<String> responseGit = client.send(request, HttpResponse.BodyHandlers.ofString());
					if (responseGit.body().contains("\"access_token\"")) {
						String token = responseGit.body().split("\"access_token\":\"")[1].split("\"")[0];
						
						HttpRequest userRequest = HttpRequest.newBuilder()
							.uri(URI.create("https://api.github.com/user"))
							.header("Authorization", "Bearer " + token)
							.header("Accept", "application/vnd.github+json")
							.build();
						
						System.out.println("GITHUB CALLBACK");
	
						responseGit = client.send(userRequest, HttpResponse.BodyHandlers.ofString());
						
						System.out.println("GITHUB TOKEN RESPONSE");
						System.out.println(responseGit.statusCode());
						System.out.println(responseGit.body());
						
						user.setGithubID(responseGit.body().split("\"id\":")[1].split(",")[0]);
						
						 userRequest = HttpRequest.newBuilder()
							.uri(URI.create("https://api.github.com/user/emails"))
							.header("Authorization", "Bearer " + token)
							.header("Accept", "application/vnd.github+json")
							.build();
						 
						 System.out.println("GITHUB CALLBACK");
						 
						 responseGit = client.send(userRequest, HttpResponse.BodyHandlers.ofString());
						 String[] emails = responseGit.body().substring(1, responseGit.body().length() - 1).split("\\},\\{");
						 for (String i : emails) {
							 if (i.contains("\"primary\":true") && i.contains("\"verified\":true")) {
								 user.setEmail(i.split("\"email\":\"")[1].split("\"")[0]);
							 }
						 }
						 System.out.println("GITHUB TOKEN RESPONSE");
						 System.out.println(responseGit.statusCode());
						 System.out.println(responseGit.body());
						 
	//					 this.verify.remove(user.getUsername());
	//					 this.states.remove(state.getID());
	//					 this.users.put(user.getUsername(), user);
						 System.out.println("GitHub ID: " + user.getGithubID());
						 System.out.println("Email: " + user.getEmail());
	
						 this.verify.remove(user.getUsername());
						 System.out.println("Removed from verify");
	
						 this.states.remove(state.getID());
						 System.out.println("Removed state");
	
						 this.users.put(user.getUsername(), user);
						 System.out.println("Added to users");
						 this.users.close();
						 
						 code = 302;
					}
				} 
				catch (InterruptedException e) {e.printStackTrace();}
			} catch(Exception e) {e.printStackTrace();}
			exchange.getResponseHeaders().set("Location", "https://mrduckytesla.com/");
			exchange.sendResponseHeaders(code, -1);
			exchange.close();
		});
		
		this.server.createContext("/api/login", exchange -> {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); 
			if (body.length() == 0) {body = exchange.getRequestURI().getQuery();}
			Map<String, String> params = this.getParams(body); 
			String response = ""; int code = 401; User user;
			
			if (this.verifyReal(params.get("real"))) {
				if (this.isAuthenticated(params) == null) {
					user = this.canLogIn(params);
					if (user != null) {
						user.createAuthKey();
						response = user.getUserAuth();
						users.put(user.getUsername(), user);
						code = 200;
					}
				} 
			}
			
			byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
			exchange.sendResponseHeaders(code, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		
		this.server.createContext("/api/auth", exchange -> {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); 
			if (body.length() == 0) {body = exchange.getRequestURI().getQuery();}
			Map<String, String> params = this.getParams(body);
			User user = this.isAuthenticated(params);
			int code = user == null? 401 : 200;
			
			exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
			exchange.sendResponseHeaders(code, -1);
			exchange.close();
		});
		
		this.server.createContext("/api/posts", exchange -> {
			
		});
	}
	
	private User isAuthenticated(Map<String, String> params) {
		if (params.containsKey("auth") && params.containsKey("username")) {
			User user;
			try {
				user = users.get(params.get("username"));
				if (user != null && user.authKeyMatch(params.get("auth"))) {return user;}
			} catch (IOException e) {return null;}
		} return null;
	}
	
	private User canLogIn(Map<String, String> params) {
		if (params.containsKey("password") && params.containsKey("username")) {
			User user;
			try {
				user = users.get(params.get("username"));
				if (user != null && user.passwordsMatch(params.get("password"))) {return user;}
			} catch (IOException e) {return null;}
		} return null;
	}
	
	private boolean userExists(Map<String, String> params) {
		try {return params.containsKey("username") && (users.get(params.get("username")) != null || verify.get(params.get("username")) != null);} 
		catch (IOException e) {return false;}
	}
	
	private boolean verifyReal(String token) {
		try {
			String secret = new String(Files.readAllBytes(Paths.get("cldsecret.txt")),StandardCharsets.UTF_8);
			String body = "secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8) + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
	
		    HttpRequest request = HttpRequest.newBuilder()
		    	.uri(URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
		    	.header("Content-Type", "application/x-www-form-urlencoded")
		    	.POST(HttpRequest.BodyPublishers.ofString(body))
		    	.build();
	
			HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
			return response.body().contains("\"success\":true");
		} catch(Exception e) {return false;}
	}
	
	private Map<String, String> getParams(String body)  {
		try {
			String[] temp, split = body.split("&");
			Map<String, String> params = new HashMap<>();
			for (int i = 0; i < split.length; i++) {
				temp = split[i].split("=", 2);
				params.put(temp[0], URLDecoder.decode(temp[1], StandardCharsets.UTF_8));
			} return params;
		} catch(Exception e) {return new HashMap<>();}
	}
	
	public void start() {
		this.server.start();
	}
	
	@Override
	public void close() throws IOException {
		this.server.stop(0); this.users.close(); 
		this.verify.close(); this.posts.close();
		this.states.close();
		try {this.verify.emptyFolder();}
		catch(IOException e) {e.printStackTrace();}
		try {this.states.emptyFolder();}
		catch(IOException e) {e.printStackTrace();}
	}

	public static void main(String[] args) {
		try {
			Host host = new Host(6767);
			
			host.start();
			
			Scanner scn = new Scanner(System.in);
			
			while (!scn.nextLine().equalsIgnoreCase("stop")) {}
			
			scn.close(); host.close();
		} 
		catch (IOException e) {e.printStackTrace();}
	}

}
