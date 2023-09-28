# Routes

Routes take care of the HTTP specific details of incoming requests and outgoing responses. Routes can:

1. match HTTP requests and extract Scala values;
2. convert Scala values into an HTTP response; and in the future
3. construct clients that call routes.

Routes are constructed from three components:

1. a @:api(krop.route.Request), which describes a HTTP request;
2. a @:api(krop.route.Response), which describes a HTTP response; and
3. a handler, which processes the values extracted from the request and produces the value needed by the response.
