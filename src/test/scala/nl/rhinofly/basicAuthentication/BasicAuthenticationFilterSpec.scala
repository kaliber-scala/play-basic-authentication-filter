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

  "The filter should allow multiple passwords to be used" in
    new FakeApplicationWithCredentials("user1", Seq("user1Pass", "user2Pass")) {
      requestWithAuthorization("Basic dXNlcjE6dXNlcjFQYXNz") isStatus OK
      requestWithAuthorization("Basic dXNlcjE6dXNlcjJQYXNz") isStatus OK
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
      cookieFromResult(request()).value === "624189caa1ac60e44842403820962958dfb720b9"
    }

  "The filter should allow other authorization headers when a cookie is available" in
    new FakeApplicationWithTestUser {
      val cookie = cookieFromResult(request())
      val result = route(FakeRequest()
        .withCookies(cookie)
        .withHeaders(AUTHORIZATION -> "test")).get

      result isStatus OK
    }

  "The filter should allow specific requests when their paths are excluded" in
  new WithApplication(
    fakeApplication(configuration(excluded = Some(Seq("/test", "/test3/[a-z/]*"))))
  ) {
    val result1 = route(FakeRequest("GET", "/test")).get
    val result2 = route(FakeRequest("GET", "/test?some=none")).get
    val result3 = route(FakeRequest("POST", "/test")).get
    val result4 = route(FakeRequest("GET", "/test2")).get
    val result5 = route(FakeRequest("POST", "/test3/")).get
    val result6 = route(FakeRequest("GET", "/test3/some")).get
    val result7 = route(FakeRequest("POST", "/test3/some/none")).get
    val result8 = route(FakeRequest("POST", "/test3/123/none")).get

    result1 isStatus OK
    result2 isStatus OK
    result3 isStatus OK
    result4 isEqualTo defaultUnAuthorizedResult
    result5 isStatus OK
    result6 isStatus OK
    result7 isStatus OK
    result8 isEqualTo defaultUnAuthorizedResult
  }

  private def configuration(
    realm: Option[String] = None,
    enabled: Option[Boolean] = None,
    username: Option[String] = Some("unknown username"),
    password: Option[AnyRef] = Some("unknown password"),
    excluded: Option[Seq[String]] = Some(Seq.empty)) =
    (realm.map("basicAuthentication.realm" -> _).toSeq ++
      enabled.map("basicAuthentication.enabled" -> _).toSeq ++
      username.map("basicAuthentication.username" -> _).toSeq ++
      password.map("basicAuthentication.password" -> _).toSeq ++
      excluded.map("basicAuthentication.excluded" -> _).toSeq :+
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

  private class FakeApplicationWithCredentials(username: String, password: AnyRef) extends WithApplication(
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