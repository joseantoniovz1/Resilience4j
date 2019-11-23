package mx.vazquez.service;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class HelloService {

	public String getGreetings(String type) {
		System.out.println("Inside service getGreetings with type: ".concat(type));
		if("time".equals(type)) {
			try {
				TimeUnit.SECONDS.sleep(35);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else if("retry".equals(type)) {
			throw new NullPointerException();
		}
		throw new RuntimeException();
	}
	
}
