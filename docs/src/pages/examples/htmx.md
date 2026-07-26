# Htmx

## Create models

```scala 3
package krop.examples.htmx.models

import io.circe.*

final case class LoginRequest(
    username: String,
    password: String
) derives Decoder,
      Encoder
```

## Create routes

```scala 3
package krop.examples.htmx.routes

import krop.all.*
import krop.examples.htmx.models.LoginRequest
import org.http4s.Status as HttpStatus
import org.http4s.headers.*

object Routes:
  val index =
    Route(
      Request.get(Path.root).extractHeader[`Cookie`],
      Response.ok(Entity.html)
    )

  val home =
    Route(
      Request.get(Path.root / "home").extractHeader[`Cookie`],
      Response.ok(Entity.html)
    )

  val register =
    Route(
      Request.get(Path.root / "register"),
      Response.ok(Entity.html)
    )

  val login = Route(
    Request
      .post(Path.root / "auth" / "login")
      .withEntity(Entity.jsonOf[LoginRequest]),
    Response
      .ok(Entity.html)
      .orElse(Response.status(HttpStatus.Forbidden, Entity.html))
      .orNotFound
  )

  val newUser = Route(
    Request
      .post(Path.root / "new_user")
      .withEntity(Entity.jsonOf[LoginRequest]),
    Response
      .status(HttpStatus.Created, Entity.html)
      .orElse(Response.status(HttpStatus.Conflict, Entity.html))
      .orNotFound
  )

  val logout = Route(
    Request
      .post(Path.root / "auth" / "logout")
      .extractHeader[Authorization],
    Response
      .ok(Entity.html)
      .orElse(Response.status(HttpStatus.Forbidden, Entity.html))
      .orNotFound
  )

  val assetRoute =
    Route(
      Request.get(Path.root / "asset" / Params.separatedString("/")),
      Response.staticResource("/asset/")
    )
end Routes
```

## Create views

### base.scala.html

```html
@(title: String, content: Html)
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>@title</title>

    <link rel="stylesheet" href="/asset/htmx-example.css" />
    <script src="/asset/htmx-example.js"></script>
    <script src="@{"https://cdn.jsdelivr.net/npm/htmx.org@2.0.10/dist/htmx.js"}" integrity="sha384-Q+Dky3iHVJOr6wUjQ4ulh6uQ76an/t+ak1+PjMVaxRjbZamFLAG+u9InkfjbsEQf" crossorigin="anonymous"></script>
</head>
<body>
<header class="app-header">
    <div class="header-container">
        <a
                hx-get="/home"
                hx-target="#app"
                hx-swap="outerHTML"
        >@title</a>
    </div>
    <div class="header-divider"></div>
</header>

<main class="main-content">
    @content
</main>
</body>
</html>
```

### login.scala.html

```html
@(errorMessage: Option[String])
<div id="app" class="app-container-narrow">
<h2>Login</h2>

<form id="loginForm">
  <div class="form-group">
    <label for="loginUsername">Username:</label>
    <input id="loginUsername" name="username" type="text" required />
  </div>
  <div class="form-group">
    <label for="loginPassword">Password:</label>
    <input id="loginPassword" name="password" type="password" required />
  </div>
  <button type="button" onclick="loginUser()">Login</button>
</form>

<div id="messageBlock">
  @errorMessage.map { msg =>
    <div class="error">@msg</div>
  }
</div>

<p>
  Don't have an account?
  <a
          onclick="htmx.process(this); this.click();"
          hx-get="/register"
          hx-target="#app"
          hx-swap="outerHTML"
  >Register</a>
</p>
</div>
```

### register.scala.html

```html
@(errorMessage: Option[String])
<div id="app" class="app-container-narrow">
<h2>Registration</h2>

<form id="registerForm">
    <div class="form-group">
        <label for="username">Username:</label>
        <input id="username" name="username" type="text" required />
    </div>
    <div class="form-group">
        <label for="password">Password:</label>
        <input id="password" name="password" type="password" required />
    </div>
    <button type="button" onclick="registerUser()">Register</button>
</form>

<div id="messageBlock">
  @errorMessage.map { msg =>
    <div class="error">@msg</div>
  }
</div>

<p>
    Have an account?
    <a
      onclick="htmx.process(this); this.click();"
      hx-get="/home"
      hx-target="#app"
      hx-swap="outerHTML"
    >Login</a>
</p>
</div>
```

### welcome.scala.html

```html
@(username: String, token: String)
<div id="app" class="app-container">
    <div id="welcomeBlock" class="welcome-container"
         data-token="@token">
        <div class="welcome-header">
            <div class="welcome-user">
                <h2>Welcome, @username!</h2>
            </div>
            <button
                    id="logoutBtn"
                    type="button"
                    onclick="clearUserCookies(); htmx.process(this); this.click();"
                    hx-post="/auth/logout"
                    hx-target="#app"
                    hx-swap="outerHTML"
                    hx-headers='{"Authorization": "Bearer @token"}'
                    class="logout-btn"
            >
                Выход
            </button>
        </div>

        <div class="tabs-container">
            <div class="tabs-header">
                <button class="tab-btn active" data-tab="in-progress" onclick="switchTab('in-progress')">
                    In progress
                </button>
                <button class="tab-btn" data-tab="planned" onclick="switchTab('planned')">
                    Planned
                </button>
                <button class="tab-btn" data-tab="knowledge" onclick="switchTab('knowledge')">
                    Knowledge base
                </button>
            </div>

            <div id="tab-in-progress" class="tab-content active">
                <div class="tab-actions">
                    <button
                            class="create-task-btn"
                            onclick="htmx.process(this); this.click();"
                            hx-get="/task/create?tab=in-progress"
                            hx-target="#app"
                            hx-swap="outerHTML"
                    >
                        + Create task
                    </button>
                </div>
                <div class="tasks-grid">
                    <div class="task-card">
                        <div class="task-icon">📚</div>
                        <div class="task-info">
                            <div class="task-title">Read "Creative Scala"</div>
                            <div class="task-meta">Noel Welsh • Progress: 60%</div>
                        </div>
                    </div>
                    <div class="task-card">
                        <div class="task-icon">📝</div>
                        <div class="task-info">
                            <div class="task-title">Write an article about Krop</div>
                            <div class="task-meta">Blog • Deadline: July 25, 2026</div>
                        </div>
                    </div>
                    <div class="task-card">
                        <div class="task-icon">💻</div>
                        <div class="task-info">
                            <div class="task-title">Implement Authentication</div>
                            <div class="task-meta">PDP Project • Priority: High</div>
                        </div>
                    </div>
                    <div class="task-card">
                        <div class="task-icon">📊</div>
                        <div class="task-info">
                            <div class="task-title">Prepare Monthly Report</div>
                            <div class="task-meta">Statistics • Due by August 1st</div>
                        </div>
                    </div>
                </div>
            </div>

            <div id="tab-planned" class="tab-content">
                <div class="tab-actions">
                    <button
                            class="create-task-btn"
                            onclick="htmx.process(this); this.click();"
                            hx-get="/task/create?tab=planned"
                            hx-target="#app"
                            hx-swap="outerHTML"
                    >
                        + Create task
                    </button>
                </div>
                <div class="tasks-grid">
                    <div class="task-card">
                        <div class="task-icon">📖</div>
                        <div class="task-info">
                            <div class="task-title">Learn Scala 3</div>
                            <div class="task-meta">Plan • Start in August</div>
                        </div>
                    </div>
                    <div class="task-card">
                        <div class="task-icon">🎯</div>
                        <div class="task-info">
                            <div class="task-title">Launch MVP project</div>
                            <div class="task-meta">Plan • Goal: Q4 2026</div>
                        </div>
                    </div>
                    <div class="task-card">
                        <div class="task-icon">📈</div>
                        <div class="task-info">
                            <div class="task-title">Course Typelevel Stack</div>
                            <div class="task-meta">Training • Planned for September</div>
                        </div>
                    </div>
                </div>
            </div>

            <div id="tab-knowledge" class="tab-content">
                <div class="tab-actions">
                    <button
                            class="create-task-btn"
                            onclick="htmx.process(this); this.click();"
                            hx-get="/task/create?tab=knowledge"
                            hx-target="#app"
                            hx-swap="outerHTML"
                    >
                        + Create task
                    </button>
                </div>
                <div class="tasks-grid">
                    <div class="task-card">
                        <div class="task-icon">📄</div>
                        <div class="task-info">
                            <div class="task-title">Krop Documentation</div>
                            <div class="task-meta">Knowledge Base • Read and Take Notes</div>
                        </div>
                    </div>
                    <div class="task-card">
                        <div class="task-icon">🎓</div>
                        <div class="task-info">
                            <div class="task-title">FP Lectures</div>
                            <div class="task-meta">Knowledge Base • View and Write Questions</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div id="messageBlock"></div>
    </div>
</div>
```

## Create assets

```javascript
document.addEventListener('htmx:beforeRequest', function(event) {
    var token = getCookie('token');
    if (token) {
        event.detail.xhr.setRequestHeader('Authorization', 'Bearer ' + token);
    }
});

function setCookie(name, value, days) {
    days = days || 7;
    var expires = new Date();
    expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
    document.cookie = name + "=" + encodeURIComponent(value) +
        "; expires=" + expires.toUTCString() +
        "; path=/";
}

function getCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) === ' ') c = c.substring(1, c.length);
        if (c.indexOf(nameEQ) === 0) return decodeURIComponent(c.substring(nameEQ.length, c.length));
    }
    return null;
}

function deleteCookie(name) {
    document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
}

function saveUserCookies(token) {
    if (token) {
        setCookie('token', token);
    }
}

function clearUserCookies() {
    deleteCookie('token');
}

function handleAuthResponse(response) {
    if (response.ok) {
        return response.text().then(html => {
            var parser = new DOMParser();
            var doc = parser.parseFromString(html, 'text/html');
            var welcomeBlock = doc.querySelector('#welcomeBlock');

            if (welcomeBlock) {
                var token = welcomeBlock.getAttribute('data-token');
                if (token) {
                    saveUserCookies(token);
                }
            }

            document.getElementById('app').outerHTML = html;
        });
    } else {
        return response.text().then(html => {
            document.getElementById('app').outerHTML = html;
        });
    }
}

function registerUser() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    fetch('/new_user', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({username, password})
    })
        .then(response => handleAuthResponse(response));
}

function loginUser() {
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;

    fetch('/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({username, password})
    })
        .then(response => handleAuthResponse(response));
}

function switchTab(tabId) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

    document.querySelector(`.tab-btn[data-tab="${tabId}"]`).classList.add('active');
    document.getElementById(`tab-${tabId}`).classList.add('active');
}
```


## Create simple auth server only for demonstration

```scala 3

```


## Create Handlers

### Create Parser 

```scala 3
package krop.examples.htmx.handlers

import org.http4s.headers.Cookie

object Parser:
  extension (cookie: Cookie)
    def getToken: Option[String] =
      cookie.values.collectFirst:
        case rq if rq.name == "token" => rq.content
```

### InitialHandler

```scala 3
package krop.examples.htmx.handlers

import cats.effect.IO
import cats.syntax.all.*
import krop.all.*
import krop.examples.htmx.routes.Routes
import krop.examples.htmx.server.SimpleAuthServer
import org.http4s.headers.Cookie
import org.typelevel.log4cats.Logger
import krop.examples.htmx.handlers.Parser.*
import krop.examples.htmx.views.html

final case class InitialHandler(
    authServer: SimpleAuthServer[IO]
)(using Logger[IO]):
  private val name = "Personal Development Plan"

  private val defaultPage =
    html.base(name, html.login(None)).toString

  val handler: Handler =
    Routes.index.handleIO: (cookie: Cookie) =>
      cookie.getToken match
        case Some(token) =>
          authServer
            .findUser(token)
            .map:
              case Some(user) =>
                html
                  .base(name, html.welcome(user.username, token))
                  .toString
              case None =>
                defaultPage
            .recoverWith:
              case ex =>
                Logger[IO]
                  .error(ex)(s"Server error: ${ex.getMessage}")
                  .as(defaultPage)
        case None =>
          defaultPage.pure[IO]
end InitialHandler
```

### HomeHandler

```scala 3
package krop.examples.htmx.handlers

import cats.effect.IO
import cats.syntax.all.*
import krop.all.*
import krop.examples.htmx.routes.Routes
import krop.examples.htmx.server.SimpleAuthServer
import org.http4s.headers.Cookie
import org.typelevel.log4cats.Logger
import krop.examples.htmx.handlers.Parser.*
import krop.examples.htmx.views.html

final case class HomeHandler(
    authServer: SimpleAuthServer[IO]
)(using Logger[IO]):
  private val defaultPage = html.login(None).toString

  val handler: Handler =
    Routes.home.handleIO: (cookie: Cookie) =>
      cookie.getToken match
        case Some(token) =>
          authServer
            .findUser(token)
            .map:
              case Some(user) =>
                html.welcome(user.username, token).toString
              case None =>
                defaultPage
            .recoverWith:
              case ex =>
                Logger[IO]
                  .error(ex)(s"Ошибка: ${ex.getMessage}")
                  .as(defaultPage)
        case None =>
          defaultPage.pure[IO]
end HomeHandler
```

### RegisterHandler

```scala 3
package krop.examples.htmx.handlers

import krop.all.*
import krop.examples.htmx.routes.Routes
import krop.examples.htmx.views.html

object RegisterHandler:
  val handler: Handler =
    Routes.register.handle { () =>
      html.register(None).toString
    }
```

### LoginHandler

```scala 3

```

### LogoutHandler

```scala 3

```

### NewUserHandler

```scala 3

```



## Create Main

```scala 3

```


[htmx]: https://htmx.org/
[source]: https://github.com/creativescala/krop/tree/main/examples/src/main