package mx.vazquez.controller;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.vavr.control.Try;
import mx.vazquez.service.HelloService;

@RestController
@RequestMapping("/resilience")
public class HelloController {

	private CircuitBreakerConfig circuitBreakerConfig;
	private TimeLimiterConfig limiterConfig;
	private RetryConfig retryConfig;
	private Retry retry;

	public HelloController() {
		circuitBreakerConfig = CircuitBreakerConfig.custom().failureRateThreshold(50)
				.recordExceptions(IOException.class, TimeoutException.class).build();

		limiterConfig = TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(10000)).cancelRunningFuture(false)
				.build();

		retryConfig = RetryConfig.custom().maxAttempts(3).retryOnException(e -> e instanceof RuntimeException)
				.retryExceptions(RuntimeException.class, TimeoutException.class, ExecutionException.class).build();

		retry = Retry.of("my", retryConfig);
	}

	@Autowired
	private HelloService helloService;

	@GetMapping(path = "/hello")
	public String helloWorld() {

		CircuitBreaker circuitBreaker = CircuitBreaker.of("helloController", circuitBreakerConfig);

		Supplier<String> result = () -> helloService.getGreetings("Example");

		result = Retry.decorateSupplier(retry, result);
		Supplier<String> getCircuitBreaker = CircuitBreaker.decorateSupplier(circuitBreaker, result);


		return Try.ofSupplier(getCircuitBreaker).recover((throwable) -> getFallback()).get();
	}
	
	
	
	@GetMapping(path = "/hola")
	public String holaMundo() {

		CircuitBreaker circuitBreaker = CircuitBreaker.of("helloController", circuitBreakerConfig);
		TimeLimiter timeLimiter = TimeLimiter.of(limiterConfig);

		Supplier<CompletableFuture<String>> futureSupplier = () -> CompletableFuture.supplyAsync(() -> helloService.getGreetings("Example"));
		
		futureSupplier = Retry.decorateSupplier(retry, futureSupplier);
		futureSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, futureSupplier);
		
		Callable<String> callable = TimeLimiter.decorateFutureSupplier(timeLimiter, futureSupplier);


		return Try.ofCallable(callable).recover((throwable) -> getFallback()).get();
	}

	private String getFallback() {
		return "Fallback";
	}

}
