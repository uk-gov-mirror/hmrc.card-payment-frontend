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
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.Status
import uk.gov.hmrc.cardpaymentfrontend.forms.ChooseAPaymentMethodFormValues
import uk.gov.hmrc.cardpaymentfrontend.testsupport.ItSpec
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps.FakeRequestOps
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.PayApiStub
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys

import scala.jdk.CollectionConverters.ListHasAsScala

class PaymentFailedControllerSpec extends ItSpec {

  private val systemUnderTest: PaymentFailedController = app.injector.instanceOf[PaymentFailedController]

  "PaymentFailedController" - {

    "GET /payment-failed" - {

      val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/payment-failed").withSessionId()
      val fakeGetRequestInWelsh: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/payment-failed").withSessionId().withLangWelsh()
      //todo we can rewrite all of these into a more cohesive test.
      "should return 200 OK" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage()(fakeGetRequest)
        status(result) shouldBe Status.OK
      }

      "include the hmrc layout" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("html").hasClass("govuk-template") shouldBe true withClue "no govuk template"
      }

      "render the page with the language toggle" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        status(result) shouldBe Status.OK
        val document = Jsoup.parse(contentAsString(result))
        val langToggleText: List[String] = document.select(".hmrc-language-select__list-item").eachText().asScala.toList
        langToggleText should contain theSameElementsAs List("English", "Newid yr iaith ir Gymraeg Cymraeg")
      }

      "show the Title tab correctly in English" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Payment failed - Pay your Self Assessment - GOV.UK"
      }

      "show the Title tab correctly in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Taliad wedi methu - Talu eich Hunanasesiad - GOV.UK"
      }

      "show the Service Name banner title correctly in English" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-header__service-name").html shouldBe "Pay your Self Assessment"
      }

      "show the Service Name banner title correctly in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-header__service-name").html shouldBe "Talu eich Hunanasesiad"
      }

      "render the correct content in English for origins with no OpenBanking" in {
        // TODO: May need changing if/when ItSa payment methods are changed
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val title = document.body().select(".govuk-heading-l")
        title.select("h1").text() shouldBe "Payment failed"
        document.getElementById("sub-heading").text() shouldBe "No payment has been taken from your card."
        document.getElementById("line1").text() shouldBe "The payment may have failed if:"
        document.getElementById("line2").text() shouldBe "there are not enough funds in your account"
        document.getElementById("line3").text() shouldBe "you entered invalid or expired card details"
        document.getElementById("line4").text() shouldBe "the address you gave does not match the one your card issuer has"
      }

      "render the correct content in Welsh for origins with no OpenBanking" in {
        // TODO: May need changing if/when ItSa payment methods are changed
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val title = document.body().select(".govuk-heading-l")
        title.select("h1").text() shouldBe "Taliad wedi methu"
        document.getElementById("sub-heading").text() shouldBe "Nid oes taliad wedi’i dynnu o’ch cerdyn."
        document.getElementById("line1").text() shouldBe "Mae’n bosibl bod y taliad wedi methi oherwydd:"
        document.getElementById("line2").text() shouldBe "nid oes yna ddigon o arian yn eich cyfrif"
        document.getElementById("line3").text() shouldBe "rydych wedi nodi manylion cerdyn sy’n annilys neu sydd wedi dod i ben"
        document.getElementById("line4").text() shouldBe "nid yw’r cyfeiriad a roesoch i ni’n cyd-fynd â’r un sydd gan ddosbarthwr eich cerdyn"
      }

    }

    "GET /payment-failed with Open Banking available as a Payment Method" - {

      val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/payment-failed").withSessionId()
      val fakeGetRequestInWelsh: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/payment-failed").withSessionId().withLangWelsh()

      "should return 200 OK" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage()(fakeGetRequest)
        status(result) shouldBe Status.OK
      }

      "render the page with the correct sub heading in English" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.selectXpath("//*[@id=\"main-content\"]/div/div/p[1]").text() shouldBe "No payment has been taken from your card."
      }

      "render the page with the correct Radio Heading content in English" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage()(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-fieldset__legend").text() shouldBe "What do you want to do?"
      }

      "render the page with the correct Radio contents in the correct order in English" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val expectedContent = List("Approve a payment to come straight from my bank account", "Try card payment again")

        val result = systemUnderTest.renderPage()(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val radios = document.select(".govuk-radios__item").asScala.toList

        radios.map(_.text()) should contain theSameElementsInOrderAs expectedContent

      }

      "render the page with the correct sub heading in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage()(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.selectXpath("//*[@id=\"main-content\"]/div/div/p[1]").text() shouldBe "Nid oes taliad wedi’i dynnu o’ch cerdyn."
      }

      "render the page with the correct Radio Heading content in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage()(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-fieldset__legend").text() shouldBe "Beth hoffech chi ei wneud?"
      }

      "render the page with the correct Radio contents in the correct order in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val expectedContent = List("Cymeradwyo taliad i fynd yn syth o’m cyfrif banc", "Rhowch gynnig arall ar dalu drwy gerdyn")

        val result = systemUnderTest.renderPage()(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val radios = document.select(".govuk-radios__item").asScala.toList

        radios.map(_.text()) should contain theSameElementsInOrderAs expectedContent

      }

    }

    "GET /payment-failed with No Open Banking as a Payment Method" - {

      val fakeGetRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/payment-failed").withSessionId().withFormUrlEncodedBody(("payment_method", ChooseAPaymentMethodFormValues.TryAgain.entryName))
      val fakeGetRequestInWelsh: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/payment-failed").withSessionId().withLangWelsh()

      "should return 200 OK" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage()(fakeGetRequest)
        status(result) shouldBe Status.OK
      }

      "render the page with the correct sub heading in English" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.selectXpath("//*[@id=\"main-content\"]/div/div/p[1]").text() shouldBe "No payment has been taken from your card."
      }

      "render the page with the correct sub heading in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage()(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.selectXpath("//*[@id=\"main-content\"]/div/div/p[1]").text() shouldBe "Nid oes taliad wedi’i dynnu o’ch cerdyn."
      }

      "render the page with the correct button content in English" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-button").first().text() shouldBe "Check details and try again"
      }

      "render the page with the correct button content in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-button").first().text() shouldBe "Gwiriwch y manylion a rhowch gynnig arall arni"
      }

      "Button should redirect to TryAgain - Enter Email Address page" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.submit(fakeGetRequest)
        redirectLocation(result) shouldBe Some("/email-address")
      }

    }

    "When Open Banking is selected" - {
      val fakeGetRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/payment-failed").withSessionId().withFormUrlEncodedBody(("payment_method", ChooseAPaymentMethodFormValues.OpenBanking.entryName))

      "Should redirect to start Open Banking Journey" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.submit(fakeGetRequest)
        redirectLocation(result) shouldBe Some("/start-open-banking")
      }

    }

    "When Try Again is selected" - {
      val fakeGetRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/payment-failed").withSessionId().withFormUrlEncodedBody(("payment_method", ChooseAPaymentMethodFormValues.TryAgain.entryName))

      "Should redirect to the Enter Email Address page" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.submit(fakeGetRequest)
        redirectLocation(result) shouldBe Some("/email-address")
      }
    }

    "When No radio option is selected" - {

      "Should show the correct error Title content in English" in {
        val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "/payment-failed").withSessionId()
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.submit(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.title() shouldBe "Error: Payment failed - Pay your Self Assessment - GOV.UK"
      }

      "Should show the correct error Title content in Welsh" in {
        val fakeGetRequestInWelsh: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/payment-failed").withSessionId().withLangWelsh()
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.submit(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.title() shouldBe "Gwall: Taliad wedi methu - Talu eich Hunanasesiad - GOV.UK"
      }

      "Should show the correct error content in English - BadRequest" in {
        val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "/payment-failed").withSessionId()
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.submit(fakeGetRequest)
        status(result) shouldBe 400
        val document = Jsoup.parse(contentAsString(result))
        val errorSummary = document.select(".govuk-error-summary")
        errorSummary.select("h2").text() shouldBe "There is a problem"
        val errorSummaryList = errorSummary.select(".govuk-error-summary__list").select("li").asScala.toList
        errorSummaryList.size shouldBe 1
        errorSummaryList.map(_.text()) shouldBe List("Select how you want to pay")
        document.select(".govuk-error-message").text() shouldBe "Error: Select how you want to pay"
      }

      "Should show the correct error content in Welsh - BadRequest" in {
        val fakeGetRequestInWelsh: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/payment-failed").withSessionId().withLangWelsh()
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.submit(fakeGetRequestInWelsh)
        status(result) shouldBe 400
        val document = Jsoup.parse(contentAsString(result))
        val errorSummary = document.select(".govuk-error-summary")
        errorSummary.select("h2").text() shouldBe "Mae problem wedi codi"
        val errorSummaryList = errorSummary.select(".govuk-error-summary__list").select("li").asScala.toList
        errorSummaryList.size shouldBe 1
        errorSummaryList.map(_.text()) shouldBe List("Dewiswch sut yr ydych am dalu")
        document.select(".govuk-error-message").text() shouldBe "Gwall: Dewiswch sut yr ydych am dalu"
      }

      "should return BadRequest when value not in ChooseAPaymentMethodFormValues is submitted" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterFailWebPayment)
        val result = systemUnderTest.submit(FakeRequest("POST", "/payment-failed").withSessionId().withFormUrlEncodedBody("payment-method" -> "IAmInValid"))
        status(result) shouldBe 400
      }

    }

  }

}
