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
import org.jsoup.select.Elements
import play.api.mvc.{AnyContentAsEmpty, Cookie}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.Status
import uk.gov.hmrc.cardpaymentfrontend.testsupport.ItSpec
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps.FakeRequestOps
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.PayApiStub
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys

import scala.jdk.CollectionConverters.ListHasAsScala

class PaymentCancelledControllerSpec extends ItSpec {

  "PaymentCancelledController" - {

    val systemUnderTest = app.injector.instanceOf[PaymentCancelledController]

    "GET /payment-cancelled" - {

      val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/payment-cancelled").withSessionId()
      val fakeGetRequestInWelsh: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/payment-cancelled").withCookies(Cookie("PLAY_LANG", "cy")).withSessionId()

      "should return 200 OK" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        status(result) shouldBe Status.OK
      }

      "render the page with the language toggle" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val langToggleText: List[String] = document.select(".hmrc-language-select__list-item").eachText().asScala.toList
        langToggleText should contain theSameElementsAs List("English", "Newid yr iaith ir Gymraeg Cymraeg") //checking the visually hidden text, it's simpler
      }

      "show the Title tab correctly in English" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Payment cancelled - Pay your Self Assessment - GOV.UK"
      }

      "show the Title tab correctly in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Taliad wedi’i ganslo - Talu eich Hunanasesiad - GOV.UK"
      }

      "render the page without a back link" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val backLink: Elements = document.select(".govuk-back-link")
        backLink.size() shouldBe 0
      }

      "should render the h1 correctly" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text() shouldBe "Payment cancelled"
      }

      "should render the h1 correctly in welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text() shouldBe "Taliad wedi’i ganslo"
      }

      "should render the two paragraphs correctly before the cta and links" in {
        val expectedParagraphs = List(
          "You have cancelled your payment.",
          "No payment has been taken from your account."
        )
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val paragraphs = document.select("#paragraph-wrapper").select("p")
        paragraphs.size() shouldBe 2
        paragraphs.asScala.toList.map(_.text()) should contain theSameElementsInOrderAs expectedParagraphs
      }

      "should render the two paragraphs correctly in welsh before the cta and links" in {
        val expectedParagraphs = List(
          "Rydych wedi canslo’ch taliad.",
          "Nid oes arian wedi’i gymryd allan o’ch cyfrif."
        )
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val paragraphs = document.select("#paragraph-wrapper").select("p")
        paragraphs.size() shouldBe 2
        paragraphs.asScala.toList.map(_.text()) should contain theSameElementsInOrderAs expectedParagraphs
      }

      "should render the page with a CTA button promoting them to enter their details again" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val button = document.select("#enter-details-again-button")
        button.hasClass("govuk-button") shouldBe true
        button.text() shouldBe "Enter your details again"
      }

      "should render the page with a CTA button promoting them to enter their details again in welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val button = document.select("#enter-details-again-button")
        button.hasClass("govuk-button") shouldBe true
        button.text() shouldBe "Nodwch eich manylion eto"
      }

      "the 'Enter details again' button should link to the /email-address page" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val button = document.select("#enter-details-again-button")
        button.attr("href") shouldBe "/email-address"
      }

      "should render the page with a link to pay another way" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val link = document.select("#pay-another-way-link")
        link.text() shouldBe "Pay another way"
      }

      "should render the page with a link to pay another way in welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val link = document.select("#pay-another-way-link")
        link.text() shouldBe "Talu drwy ddull arall"
      }

      "the pay another way link should link to X" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val button = document.select("#pay-another-way-link")
        button.attr("href") shouldBe "https://www.gov.uk/topic/dealing-with-hmrc/paying-hmrc"
      }

      "should render the page with a link to exit without paying" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val link = document.select("#exit-wthout-paying-link")
        link.text() shouldBe "Exit without paying"
      }

      "should render the page with a link to exit without paying in welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val link = document.select("#exit-wthout-paying-link")
        link.text() shouldBe "Gadewch heb dalu"
      }

      "the exit without paying link should link to survey if returnUrl is not set for journey" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val link = document.select("#exit-wthout-paying-link")
        link.attr("href") shouldBe "/start-payment-survey"
      }

      "the exit without paying link should link to returnUrl if returnUrl is set for journey" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyAfterCancelWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val link = document.select("#exit-wthout-paying-link")
        link.attr("href") shouldBe "https://www.return-url.com"
      }
    }
  }
}
