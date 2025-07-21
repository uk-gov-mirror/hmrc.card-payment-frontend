/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.cardpaymentfrontend.controllers

import org.jsoup.Jsoup
import play.api.http.Status._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, session, status}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.ItSpec
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps.FakeRequestOps
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.PayApiStub
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys

class SignOutControllerSpec extends ItSpec {

  "SignOutController should" - {

    val systemUnderTest: SignOutController = app.injector.instanceOf[SignOutController]
    val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/sign-out").withSessionId()

    "signOutFromTimeout should redirect to signOutUrl with continue URL" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val expectedContinueUrl = "http://localhost:10155/timed-out"
      val result = systemUnderTest.signOutFromTimeout(fakeGetRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"http://localhost:9553/bas-gateway/sign-out-without-state?continue=${expectedContinueUrl}")
    }

    "signOut should redirect to signOutUrl with feedback continue URL" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val expectedContinueUrl = "%2Ffeedback%2Fpay-online"
      val result = systemUnderTest.signOut(fakeGetRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"http://localhost:9553/bas-gateway/sign-out-without-state?continue=${expectedContinueUrl}")
    }

    "timedOut should return OK with TimedOutPage and clear session" in {
      val result = systemUnderTest.timedOut(fakeGetRequest)
      val document = Jsoup.parse(contentAsString(result))

      document.select(".govuk-button").text() shouldBe "Sign in"
      println(document)
      status(result) shouldBe OK
      session(result).data shouldBe empty
    }

    // TODO: Placeholder content. We don't know what this is doing.
    "keepAlive should return OK with 'Okay' response" in {
      val result = systemUnderTest.keepAlive(fakeGetRequest)
      status(result) shouldBe OK
    }

  }

}
