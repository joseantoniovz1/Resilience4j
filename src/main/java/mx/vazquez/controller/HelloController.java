package mx.vazquez.controller;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	
	@Autowired
	private HelloService helloService;

	private CircuitBreakerConfig circuitBreakerConfig;
	private TimeLimiterConfig limiterConfig;
	private RetryConfig retryConfig;
	private Retry retry;

	public HelloController() {
		circuitBreakerConfig = CircuitBreakerConfig.custom().failureRateThreshold(50)
				.recordExceptions(IOException.class, RuntimeException.class).build();

		limiterConfig = TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(10000)).cancelRunningFuture(true)
				.build();

		retryConfig = RetryConfig.custom().maxAttempts(3).retryOnException(e -> e instanceof NullPointerException)
				.retryExceptions(ExecutionException.class, NullPointerException.class).build();

		retry = Retry.of("my", retryConfig);
	}
	
	
	@GetMapping(path = "/resilience")
	public String getResilience(@RequestParam(name = "type", defaultValue = "default") String type) {

		CircuitBreaker circuitBreaker = CircuitBreaker.of("helloController", circuitBreakerConfig);
		TimeLimiter timeLimiter = TimeLimiter.of(limiterConfig);

		Supplier<CompletableFuture<String>> futureSupplier = () -> CompletableFuture.supplyAsync(() -> helloService.getGreetings(type));
		
		futureSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, futureSupplier);
		
		Callable<String> callable = TimeLimiter.decorateFutureSupplier(timeLimiter, futureSupplier);
		callable = Retry.decorateCallable(retry, callable::call);

		return Try.ofCallable(callable).recover((throwable) -> getFallback()).get();
	}

	private String getFallback() {
		return "Fallback";
	}

}
