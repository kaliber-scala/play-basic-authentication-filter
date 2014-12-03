package nl.rhinofly.basicAuthentication

import scala.concurrent.Future

import org.specs2.mutable.Specification

import play.api.GlobalSettings

import play.api.mvc.Action
import play.api.mvc.Cookie
import play.api.mvc.Handler
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.mvc.Results.Unauthorized
import play.api.mvc.WithFilters

import play.api.test.FakeApplication
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test.WithApplication
import play.api.test.Helpers

object BasicAuthenticationFilterSpec extends Specification {

  """|The BasicAuthenticationFilter is a filter that can be added to
     |the Global object""".stripMargin in {

    object Global extends WithFilters(BasicAuthenticationFilter()) with GlobalSettings
    success
  }

  """|The filter should respond with a correct unauthorized result if the
     |request does not contain the correct headers""".stripMargin >> {

    "If username and password are not present" in
      new FakeApplication {
        requestResult isEqualTo unAuthorizedResult("Application: The username or password could not be found in the configuration.")
      }

    "If the header is not present" in
      new FakeApplicationWithUnknownCredentials {
        requestResult isEqualTo defaultUnAuthorizedResult
      }

    "If a different realm is set" in
      new FakeApplicationWithRealm("test") {
        requestResult isEqualTo unAuthorizedResult(realm = "test")
      }

    "If the header is in a incorrect format" in
      new FakeApplicationWithUnknownCredentials {
        requestWithAuthorization("") isEqualTo defaultUnAuthorizedResult
        requestWithAuthorization("something") isEqualTo defaultUnAuthorizedResult
        requestWithAuthorization("Basic something else") isEqualTo defaultUnAuthorizedResult
      }
  }

  "The filter should allow all requests when it is disabled" in
    new FakeApplicationWithFilterDisabled {
      requestResult isEqualTo Ok
    }

  "The filter should allow a correctly authenticated request" in
    new FakeApplicationWithCredentials("Aladdin", "open sesame") {
      requestWithAuthorization("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==") isStatus OK
    }

  "Example1" in
    new FakeApplicationWithCredentials("fred", "fred") {
      requestWithAuthorization("Basic ZnJlZDpmcmVk") isStatus OK
    }

  "Example2" in
    new FakeApplicationWithCredentials("user1", "user1Pass") {
      requestWithAuthorization("Basic dXNlcjE6dXNlcjFQYXNz") isStatus OK
    }

  "The filter should authenticate based on a cookie the second time" in
    new FakeApplicationWithTestUser {
      val result = request()

      val cookie = cookieFromResult(result)

      requestWithCookie(cookie) isStatus OK
    }

  "The filter should not authenticate any cookie" in
    new FakeApplicationWithCredentials("test", "test") {
      val cookie1 = Cookie("play-basic-authentication-filter", "")
      requestWithCookie(cookie1) isEqualTo defaultUnAuthorizedResult

      val cookie2 = Cookie("play-basic-authentication-filter", "test")
      requestWithCookie(cookie2) isEqualTo defaultUnAuthorizedResult
    }

  "The filter should return the correct cookie" in
    new FakeApplicationWithTestUser {
      cookieFromResult(request()).value === "9298e9b9fec2749a9d1c9ffca186647f28daa9ba"
    }

  "The filter should allow other authorization headers when a cookie is available" in
    new FakeApplicationWithTestUser {
      val cookie = cookieFromResult(request())
      val result = route(FakeRequest()
        .withCookies(cookie)
        .withHeaders(AUTHORIZATION -> "test")).get

      result isStatus OK
    }

  private def configuration(
    realm: Option[String] = None,
    enabled: Option[Boolean] = None,
    username: Option[String] = Some("unknown username"),
    password: Option[String] = Some("unknown password")) =
    (realm.map("basicAuthentication.realm" -> _).toSeq ++
      enabled.map("basicAuthentication.enabled" -> _).toSeq ++
      username.map("basicAuthentication.username" -> _).toSeq ++
      password.map("basicAuthentication.password" -> _).toSeq :+
      ("application.secret" -> "test-secret")
    ).toMap

  private def requestResult = route(FakeRequest()).get

  private def requestWithAuthorization(value: String) =
    route(FakeRequest().withHeaders(AUTHORIZATION -> value)).get

  private def requestWithCookie(cookie: Cookie) =
    route(FakeRequest().withCookies(cookie)).get

  private class GlobalWithBasicAuth extends WithFilters(BasicAuthenticationFilter()) with GlobalSettings

  private class FakeApplication extends WithApplication(fakeApplication())

  private class FakeApplicationWithRealm(realm: String) extends WithApplication(
    fakeApplication(configuration(realm = Some(realm)))
  )

  private class FakeApplicationWithFilterDisabled extends WithApplication(
    fakeApplication(configuration(enabled = Some(false)))
  )

  private class FakeApplicationWithUnknownCredentials extends WithApplication(
    fakeApplication(configuration())
  )

  private class FakeApplicationWithCredentials(username: String, password: String) extends WithApplication(
    fakeApplication(configuration(username = Some(username), password = Some(password)))
  )

  private class FakeApplicationWithTestUser extends FakeApplicationWithCredentials("test-user", "test-password") {
    def request() = requestWithAuthorization("Basic dGVzdC11c2VyOnRlc3QtcGFzc3dvcmQ=")
  }

  private def fakeApplication(configuration: Map[String, Any] = Map.empty) =
    FakeApplication(
      withRoutes = defaultRoutes,
      additionalConfiguration = configuration,
      withGlobal = Some(new GlobalWithBasicAuth)
    )

  private def defaultRoutes: PartialFunction[(String, String), Handler] = {
    case (_, _) => Action { Ok }
  }

  private implicit class TestEnhancement(a: Future[Result]) {
    def isEqualTo[B](b: B) = a must beEqualTo(b).await
    def isStatus(b: Int) = Helpers.status(a) === b
  }

  private def defaultUnAuthorizedResult = unAuthorizedResult(realm = "Application")

  private def unAuthorizedResult(realm: String) =
    Unauthorized.withHeaders(WWW_AUTHENTICATE -> s"""Basic realm="$realm"""")

  private def cookieFromResult(result: Future[Result]) =
    Helpers.cookies(result).get("play-basic-authentication-filter").get
}