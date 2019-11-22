package mx.vazquez.service;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class HelloService {

	public String getGreetings(String name) {
		//return "Hello World from service: ".concat(name);
		System.out.println("Inside service getGreetings");
		try {
			TimeUnit.SECONDS.sleep(35);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new RuntimeException();
	}
	
}
