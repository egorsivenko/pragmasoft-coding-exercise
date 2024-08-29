# Rate Limiter Using Token Bucket Algorithm
This document provides usage instructions for the Rate Limiter implemented using the Token Bucket algorithm. The rate limiter restricts the number of incoming HTTP requests to a configurable rate, ensuring fair usage and preventing abuse of resources.

## Features
- **Token Bucket Algorithm**: Limits the rate of incoming requests per client.
- **Configurable**: Allows customization of rate limits, including capacity, refill rate, and expiration.
- **Client Key Abstraction**: Supports client key retrieval from IP address, and might be easily replaced with a different strategy, e.g. Authorization header.
- **Thread-Safe**: Handles concurrent requests efficiently.
- **Stale Bucket Cleanup**: Periodically removes inactive client buckets to conserve memory.

## Setup and Configuration
### Step 1: Clone the Project
First, clone the project from the GitHub repository:
```
$ git clone https://github.com/egorsivenko/pragmasoft-coding-exercise.git
```

### Step 2: Configure Application Properties
Customize the rate limiter by setting the following properties in `application.properties`:
```
rate.limit.capacity=10           # Maximum number of tokens in each bucket
rate.limit.refillPeriod=1000     # Refill period in milliseconds
```

### Step 3: Running the Application
Run the Spring Boot application:
```
$ ./mvnw spring-boot:run
```
The application will start and be ready to handle incoming HTTP requests to the `/hello` endpoint with rate limiting applied:
```
curl http://localhost:8080/hello
```
- If the rate limit is not exceeded, the server responds with `Hello World`.
- If the rate limit is exceeded, the server responds with a `429 Too Many Requests` status code and a JSON error message:
```
{
    "title": "Rate Limit Exceeded",
    "status": 429,
    "detail": "Exceeded the maximum number of requests. Try again later.",
    "requests_number": 10,
    "wait_seconds": 1
}
```

## Testing
The project includes unit tests for the `RateLimitService`. You can run the tests using:
```
$ ./mvnw test
```

## Acknowledgements
This project was developed as part of a test assignment for [Pragmasoft](https://pragmasoft.com.ua/en/).
