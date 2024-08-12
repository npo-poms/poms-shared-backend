# VPRO Web API Standards

* [Guidelines](#guidelines)
* [Pragmatic REST](#pragmatic-rest)
* [HTTP Verbs](#http-verbs)
* [Responses](#responses)
* [Error handling](#error-handling)
* [Versions](#versions)
* [Record limits](#record-limits)
* [Request & Response Examples](#request-response-examples)
* [Mock Responses](#mock-responses)
* [JSONP](#jsonp)

## Guidelines

This document provides guidelines and examples for VPRO Web APIs, encouraging consistency, maintainability, and best practices across applications. VPRO APIs aim to balance a truly RESTful API interface with a positive developer experience (DX).

This is a based on the White House API standards found here:
[White House Web API Standards](https://github.com/WhiteHouse/api-standards)

## Pragmatic REST

These guidelines aim to support a RESTful API. Here are a few exceptions:
* Put the version number of the API in the URL (see examples below). Don’t accept any requests that do not specify a version number.
* Allow users to request formats like JSON or XML like this:
    * http://api.vpro.nl/v1/magazines.json (???)
    * http://api.vpro.nl/v1/magazines.xml (???)
* We support also Accept-headers so you can also do:
    * http://api.vpro.nl/v1/magazines

## URLs

### General guidelines for  URLs
* URLs should include nouns, not verbs.
* Use plural nouns only for consistency (no singular nouns).
* You shouldn’t need to go deeper than resource/identifier/resource.
* Put the version number at the base of your URL, for example http://example.com/v1/path/to/resource.
* URL v. header:
    * If it changes the logic you write to handle the response, put it in the URL.
    * If it doesn’t change the logic for each response, like OAuth info, put it in the header.
* Specify optional fields in a comma separated list.
* Formats should be in the form of v2/resource/{id}.json

### Good URL examples
* List of magazines:
    * http://www.api.vpro.nl/v1/magazines.json
* Filtering is a query:
    * http://www.api.vpro.nl/v1/magazines.json?year=2011&sort=desc
    * http://www.api.vpro.nl/v1/magazines.json?topic=economy&year=2011
* A single magazine in JSON format:
    * http://www.api.vpro.nl/v1/magazines/1234.json
* All articles in (or belonging to) this magazine:
    * http://www.api.vpro.nl/v1/magazines/1234/articles.json
* All articles in this magazine in XML format:
    * GET http://api.vpro.nl/v1/magazines/1234/articles.xml
* Specify optional fields in a comma separated list:
    * http://www.api.vpro.nl/v1/magazines/1234.json?fields=title,subtitle,date
* Add a new article to a particular magazine:
    * POST http://api.vpro.nl/v1/magazines/1234/articles

### Bad URL examples
* Non-plural noun:
    * http://www.api.vpro.nl/magazine
    * http://www.api.vpro.nl/magazine/1234
    * http://www.api.vpro.nl/publisher/magazine/1234
* Verb in URL:
    <dependency>
      <groupId>org.jboss.resteasy</groupId>
      <artifactId>resteasy-jaxrs</artifactId>
    </dependency>
    * http://www.api.vpro.nl/magazine/1234/create
* Filter outside of query string
    * http://www.api.vpro.nl/magazines/2011/desc

## HTTP Verbs

| HTTP METHOD | POST            | GET       | PUT         | DELETE |
| ----------- | --------------- | --------- | ----------- | ------ |
| CRUD OP     | CREATE          | READ      | UPDATE      | DELETE |
| /dogs       | Search dogs     | List dogs | not supported  | not supported   |
| /dogs/1234  | Error           | Show Bo   | not supported  | not supported   |

(Example from Web API Design, by Brian Mulloy, Apigee.)


## Responses

* No values in keys
* No internal-specific names (e.g. "node" and "taxonomy term")
* Metadata should only contain direct properties of the response set, not properties of the members of the response set

### Good examples

No values in keys:

    "tags": [
      {"id": "125", "name": "Environment"},
      {"id": "834", "name": "Water Quality"}
    ],


### Bad examples

Values in keys:

    "tags": [
      {"125": "Environment"},
      {"834": "Water Quality"}
    ],


## Error handling

Error responses should include a common HTTP status code, message for the developer, message for the end-user (when appropriate), internal error code (corresponding to some specific internally determined error number), links where developers can find more info. For example:

    {
      "status" : "400",
      "developerMessage" : "Verbose, plain language description of the problem. Provide developers
       suggestions about how to solve their problems here",
      "userMessage" : "This is a message that can be passed along to end-users, if needed.",
      "errorCode" : "444444",
      "more info" : "http://www.api.vpro.nl/developer/path/to/help/for/444444,
       http://drupal.org/node/444444",
    }

Use three simple, common response codes indicating (1) success, (2) failure due to client-side problem, (3) failure due to server-side problem:
* 200 - OK
* 400 - Bad Request
* 500 - Internal Server Error


## Versions

* Never release an API without a version number.
* Versions should be integers, not decimal numbers, prefixed with ‘v’. For example:
    * Good: v1, v2, v3
    * Bad: v-1.1, v1.2, 1.3
* Maintain APIs at least one version back.



## Record limits

* If no max is specified, return results with a default max.
* To get records 50 through 75 do this:
    * http://api.vpro.nl/magazines?max=25&offset=50
    * offset=50 means, ‘skip fifty records’
    * max=25 means, ‘return at most 25 records’

Information about record limits should also be included in the Example response. Example:

    {
      "count": 50,
      "offset": 25,
      "max": 25,
      "list": [
            { .. }
        ]
    }

## Request & Response Examples

### API Resources

  - [GET /magazines](#get-magazines)
  - [GET /magazines/[id]] (#get-magazinesid)
  - [POST /magazines/[id]/articles] (#post-magazinesidarticles)

### GET /magazines

Example: http://api.vpro.nl/v1/magazines.json

    {
        "count": 123,
        "offset": 0,
        "max": 10,
        "list": [
            {
                "id": "1234",
                "type": "magazine",
                "title": "Public Water Systems",
                "tags": [
                    {"id": "125", "name": "Environment"},
                    {"id": "834", "name": "Water Quality"}
                ],
                "created": "1231621302"
            },
            {
                "id": 2351,
                "type": "magazine",
                "title": "Public Schools",
                "tags": [
                    {"id": "125", "name": "Elementary"},
                    {"id": "834", "name": "Charter Schools"}
                ],
                "created": "126251302"
            }
            {
                "id": 2351,
                "type": "magazine",
                "title": "Public Schools",
                "tags": [
                    {"id": "125", "name": "Pre-school"},
                ],
                "created": "126251302"
            }
        ]
    }

### GET /magazines/[id]

Example: http://api.vpro.nl/v1/magazines/[id].json

    {
        "id": "1234",
        "type": "magazine",
        "title": "Public Water Systems",
        "tags": [
            {"id": "125", "name": "Environment"},
            {"id": "834", "name": "Water Quality"}
        ],
        "created": "1231621302"
    }

### POST /magazines

Posting to a list performs a search. You post a form object. Depending
on the content-type of the request you can post this in JSON or in
XML.

You can e.g. post something like this

{
    "highlight": false,
    "searches": {
        "broadcasters": [
            {
                "partial": false,
                "value": "VPRO"
            }
        ]
    }
}


So, we don't support the creation of new documents in a RESTfull
manner.




## Mock Responses

It is suggested that each resource accept a 'mock' parameter on the testing server. Passing this parameter should return a mock data response (bypassing the backend).

Implementing this feature early in development ensures that the API will exhibit consistent behavior, supporting a test driven development methodology.

Note: If the mock parameter is included in a request to the production environment, an error should be raised.


## JSONP

JSONP is easiest explained with an example. Here's a one from [StackOverflow](http://stackoverflow.com/questions/2067472/what-is-jsonp-all-about?answertab=votes#tab-top):

> Say you're on domain abc.com, and you want to make a request to domain xyz.com. To do so, you need to cross domain boundaries, a no-no in most of browserland.

> The one item that bypasses this limitation is `<script>` tags. When you use a script tag, the domain limitation is ignored, but under normal circumstances, you can't really DO anything with the results, the script just gets evaluated.

> Enter JSONP. When you make your request to a server that is JSONP enabled, you pass a special parameter that tells the server a little bit about your page. That way, the server is able to nicely wrap up its response in a way that your page can handle.

> For example, say the server expects a parameter called "callback" to enable its JSONP capabilities. Then your request would look like:

>         http://www.xyz.com/sample.aspx?callback=mycallback

> Without JSONP, this might return some basic javascript object, like so:

>         { foo: 'bar' }

> However, with JSONP, when the server receives the "callback" parameter, it wraps up the result a little differently, returning something like this:

>         mycallback({ foo: 'bar' });

> As you can see, it will now invoke the method you specified. So, in your page, you define the callback function:

>         mycallback = function(data){
>             alert(data.foo);
>         };

http://stackoverflow.com/questions/2067472/what-is-jsonp-all-about?answertab=votes#tab-top
