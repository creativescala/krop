# Controller

Controllers are responsible for extracting information from a request, making appropriate calls into the model, and constructing a view to return as the response. Of all the parts in a Krop application, controllers are the most concerned with the details of the HTTP protocol.

Controllers consist of two parts:

* [Routes](route/README.md), which deal with parsing HTTP requests and assembling HTTP responses; and
* [Handlers](handler.md), which work with the values parsed from an HTTP request to produce the values required to assemble the response.
