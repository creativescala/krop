# Views

Views are responsible for generating the user interface of your application. 
A user interface could be HTML displayed in a web browser, JSON returned from an API endpoint, or code that runs client-side.
As there are several different kinds of user interfaces there are several different systems for creating views.


## Static Files

## Templates

Krop uses [Twirl][twirl] for templates, which are views that are mostly text with a few pieces of programmatic content. Templates are ideal for generating HTML.

By default templates are found in `backend/src/main/twirl/`.


[twirl]: https://www.playframework.com/documentation/3.0.x/ScalaTemplates
