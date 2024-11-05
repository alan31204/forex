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

Thus, within the get function, first need to see if it's a hit or miss of the cache. If hit, directly give back the rate. If miss, we initiated a query function to OneFrame API with query parameters, URI, and token. Then use Java HttpClient to make a call to the OneFrame service. If the request is traversed and found, then use extractRate to extract the Rate object, and then applies to cacheRate function for storing the cache.  

This serves as a basic proxy protocol for OneFrame API service. 

# Limitations and Improvements
Within the given time, I wasn't able to learn all the needed concepts in Scala for helping me with what I want to achieve. Below are some potential improvements that I can think of. 

1. This proxy makes single request for a given pair to OneFrame if it's not a hit of cache. With limitations of OneFrame API, this will not work if the request are made frequently and fairly since OneFrame API supports a maximum of 1000 requests per day. One potential improvement is that whenever trying to make request to OneFrame API, make requests for all 72 combinations of pairs in our currency lists as there are only 9 currency types, which results into 72 combinations. By this, it will address the limitations of only 1000 request to OneFrame per day by calling OneFrame once and updated all cache values of 72 combinations. 
2. Currently, I haven't experimented the testing framework of Scala, so that I have tested it with End to End testing without having some unit tests and integration tests. With learning scala testing frameworks, I think the service can be designed more robust. 
3. Currently, I implemented the cache using standard Scala map function, but to scale it better, I think I don't need to implement my own caching system, but can utilize some existing libraries to support this. 
4. For the host endpoint, token, and port info, it can be improved by managing it within config instead of hardcoding in the file as it is now. 

# Final
This is an interesting opportunity for me to learn functional programming and do type matching + inferencing. There are a lot of concepts to learn, and I think it's a valuable experience for me. 