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
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.ItSpec
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps.FakeRequestOps
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.{PayApiStub, PaymentsSurveyStub}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.{TestJourneys, TestPaymentsSurveyData}

class PaymentsSurveyControllerSpec extends ItSpec {

  val systemUnderTest: PaymentsSurveyController = app.injector.instanceOf[PaymentsSurveyController]

  "PaymentsSurveyController" - {
    "startSurvey" - {
      "redirect to payment survey redirect url when journey is in terminal state" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        PaymentsSurveyStub.stubForStartJourney2xx(TestPaymentsSurveyData.ssJResponse)
        val fakeRequest = FakeRequest().withSessionId()
        val result = systemUnderTest.startSurvey()(fakeRequest)
        redirectLocation(result) shouldBe Some("http://survey-redirect-url.com")
      }

      "should return 410 (Gone) when journey is not in terminal state" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val fakeRequest = FakeRequest().withSessionId()
        val result = systemUnderTest.startSurvey()(fakeRequest)
        status(result) shouldBe 410
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").html() shouldBe "This page can’t be found"
      }

      "should return 401 when journey cannot be found" in {
        val fakeRequest = FakeRequest().withSessionId()
        val result = systemUnderTest.startSurvey()(fakeRequest)
        status(result) shouldBe 401
      }

    }
  }

}

