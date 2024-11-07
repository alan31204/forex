# A Local proxy for Forex rates

# Preview

We provide you with an initial scaffold for the application with some dummy interpretations/implementations. For starters we would like you to try and understand the structure of the application, so you can use this as the base to address the following use case:

* The service returns an exchange rate when provided with 2 supported currencies 
* The rate should not be older than 5 minutes
* The service should support at least 10,000 successful requests per day with 1 API token

Please note the following drawback of the [One-Frame service](https://hub.docker.com/r/paidyinc/one-frame): 

> The One-Frame service supports a maximum of 1000 requests per day for any given authentication token. 

# Start
Before starting, be sure to initiate the OneFrame Docker image from running. 

To run the project, use below command line

```
sbt compile

sbt run
```
This will start the entire program with compiling + running. 

Then, one can make curl command to the service by calling sample command below
```
curl 'localhost:8081/rates?from=USD&to=JPY'
```

# Libraries used
Utilize the Java io json parsing, Java HttpClient and scala mutable map for serving as the usage of basic caching

# Approach
All of the changes are implemented within OneFrameDummy. In order to serve the requests from clients, proxy will need to judge whether the value has been stored in the cache or not. If it's still valid and can be reused from cache, serve the request directly to customer. Otherwise, makes a call to OneFrame API for querying the request. 

In addition to get function, I defined extractRate for parsing the response from json object, getCache for trying to see if the query is within valid timeframe(5 minutes) of cache, and cacheRate function for putting the data in cache. 

allParis parameter generates all 72 combinations. Then, I defined a newExtractRate function which makes a single call of json which applies bulk call to OneFrame with all 72 combinations and then stores every response in cache. In addition to that, I take a pair as parameter input, which at the end of newExtractRate function will make a call to the cache from response. Thus, newExtractRate parses entire json, do pattern matching to find each rate, stores them in cache, then finally returns the value of single cache back to the response. The reason for choosing to make single bulk call of all 72 combinations to OneFrame API is due to the limitations that OneFrame can only serve 1000 requests at most per day. Forex proxy is needed to support 10000 requests per day, so it won't work properly if we need to make OneFrame API call whenever we can't find the data in cache or it's not accurate anymore(over 5 minutes). 

Thus, within the get function, first need to see if it's a hit or miss of the cache by calling getCache. 
If hit, directly return back the rate found in cache. If miss, we initiated a query function to OneFrame API with all combinations of query parameters, URI, and token. Then use Java HttpClient to make a call to the OneFrame service. 
If the request is traversed and found, then use newExtractRate instead of single extractRate to extract the Rate info. During newExtractRate function, it does query for all 72 combinations, cache all in rate pair, then do a single getCache call with searched rate. 

This serves as a basic proxy protocol for OneFrame API service. 

# Limitations and Improvements
Within the given time, I wasn't able to learn all the needed concepts in Scala for helping me with what I want to achieve. Below are some potential improvements that I can think of. 

1. This proxy makes single request for all 72 combinations to OneFrame if it's not a hit of cache. With limitations of OneFrame API, this can currently work when we have small set of rate pair combinations since OneFrame API supports a maximum of 1000 requests per day. If there are more different combinations of currency combinations, a better approach of caching + OneFrame API calls need to be implemented instead of current approach. It would be per design based since there can be different limitations and tradeoffs on call frequencies, accuracies and loading.  
2. Currently, I haven't experimented the testing framework of Scala, so that I have tested it with End to End testing without having some unit tests and integration tests. With learning scala testing frameworks, I think the service can be designed more robust. 
3. Currently, I implemented the cache using standard Scala map function, but to scale it better, I think I don't need to implement my own caching system, but can utilize some existing libraries to support this. 
4. For the host endpoint, token, and port info, it can be improved by managing it within config instead of hardcoding in the file as it is now. 

# Final
This is an interesting opportunity for me to learn functional programming and do type matching + inferencing. There are a lot of concepts to learn, and I think it's a valuable experience for me. 