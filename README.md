# Basic Authentication Filter

A simple Play Framework filter that provides basic authentication

## Installing

```scala
resolvers += "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local"

libraryDependencies += "nl.rhinofly" %% "play-basic-authentication-filter" % "0.4"
```

*Global.scala*
```
import nl.rhinofly.basicAuthentication.BasicAuthenticationFilter

object Global extends WithFilters(BasicAuthenticationFilter()) with GlobalSettings
```

## Configuration

The filter can be configured in different ways.

**enabled**

If set to false, the filter does not do anything.

```scala
// default: true
basicAuthentication.enabled=false
```

**realm**

This is the realm to which the security applies. Most browsers display it
in the dialog.

```scala
// default: Application
basicAuthentication.realm="My Special Realm"
```

**username** and **password**

These are the credentials that are checked. If you do not specify them, the `realm`
is set to a value indicating that and it defaults to a random universal unique
identifier for both username and password.

```scala
// default: random uuid
basicAuthentication.username=username
basicAuthentication.password=password

// multiple passwords (used in combination with the same username)
basicAuthentication.password=[password1,password2]
```
Running the application in a browser should now display a basic authentication popup.

**excluded**

Allows you to add paths that the authentication filter ignores.

```scala
// default: empty list
basicAuthentication.excluded = ["/path1", "/path2"]
```

## Releasing the play-basic-authentication-filter plugin

Make sure you have the correct credentials present (in `~/.sbt/0.13/credentials.sbt`)

```scala
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
```

Then in the `sbt` console type `release`

