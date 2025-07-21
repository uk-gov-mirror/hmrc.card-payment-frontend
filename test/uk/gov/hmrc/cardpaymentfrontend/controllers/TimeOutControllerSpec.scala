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
import play.api.http.Status.OK
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, session, status}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.ItSpec
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps.FakeRequestOps
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.PayApiStub
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys

class TimeOutControllerSpec extends ItSpec {

  "TimeOutController should" - {

    val systemUnderTest: TimeOutController = app.injector.instanceOf[TimeOutController]
    val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/timed-out").withSessionId()

    "showDeleteAnswersPage should return OK with DeleteAnswersPage and clear session" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.showDeleteAnswersPage(fakeGetRequest)
      val document = Jsoup.parse(contentAsString(result))
      println(document)
      document.select(".govuk-button").text() shouldBe "Start again"
      document.title() shouldBe "You deleted your answers - Pay your Self Assessment - GOV.UK"
      status(result) shouldBe OK
      session(result).data shouldBe empty
    }

    "showForceDeleteAnswersPage should return OK with ForceDeleteAnswersPage and clear session" in {
      val result = systemUnderTest.showForceDeleteAnswersLoggedOutPage(fakeGetRequest)
      val document = Jsoup.parse(contentAsString(result))
      println(document)
      document.select(".govuk-button").text() shouldBe "Start again"
      document.title() shouldBe "For your security, we deleted your answers - Pay your tax - GOV.UK"
      status(result) shouldBe OK
      session(result).data shouldBe empty
    }

  }

}

