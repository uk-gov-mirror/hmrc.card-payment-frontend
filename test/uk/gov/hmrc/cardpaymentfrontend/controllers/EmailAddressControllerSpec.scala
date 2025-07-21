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
import payapi.corcommon.model.JourneyId
import play.api.http.Status
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.cardpaymentfrontend.testsupport.ItSpec
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps._
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.PayApiStub
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys

import scala.jdk.CollectionConverters.ListHasAsScala

class EmailAddressControllerSpec extends ItSpec {

  override def beforeEach(): Unit = {
    PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
    ()
  }

  private val systemUnderTest: EmailAddressController = app.injector.instanceOf[EmailAddressController]

  "EmailController" - {

    "GET /email-address" - {

      val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", "/email-address").withSessionId()

      val fakeGetRequestInWelsh: FakeRequest[AnyContentAsEmpty.type] =
        fakeGetRequest.withLangWelsh()
      "should return 200 OK" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        status(result) shouldBe Status.OK
      }

      //TODO: check if this should redirect to /pay as well.
      "should return 401 unauthorised when there is no journey returned from pay-api via action refiner" in {
        PayApiStub.stubForFindBySessionId404
        val result = systemUnderTest.renderPage(fakeGetRequest)
        status(result) shouldBe Status.UNAUTHORIZED
      }

      "render the page with the hmrc layout" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("html").hasClass("govuk-template") shouldBe true withClue "no govuk template"
      }

      "show the Title tab correctly in English" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "What is your email address? (optional) - Pay your Self Assessment - GOV.UK"
      }

      "show the Title tab correctly in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Beth yw’ch cyfeiriad e-bost? (dewisol) - Talu eich Hunanasesiad - GOV.UK"
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

      "render the page with the h1 correctly in English" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text() shouldBe "What is your email address? (optional)" withClue "service name wrong"
      }

      "render the page with the h1 correctly in Welsh" in {
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text() shouldBe "Beth yw’ch cyfeiriad e-bost? (dewisol)" withClue "service name wrong in welsh"
      }

      "render the page with the language toggle" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val langToggleText: List[String] = document.select(".hmrc-language-select__list-item").eachText().asScala.toList
        langToggleText should contain theSameElementsAs List("English", "Newid yr iaith ir Gymraeg Cymraeg") //checking the visually hidden text, it's simpler
      }

      "render the page with a back link" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val backLink = document.select(".govuk-back-link")
        backLink.text() shouldBe "Back"
        backLink.attr("href") shouldBe "#"
      }

      "render the page with a back link in welsh" in {
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val backLink = document.select(".govuk-back-link")
        backLink.text() shouldBe "Yn ôl"
        backLink.attr("href") shouldBe "#"
      }

      "render the page with a text field" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("#email-address").size() shouldBe 1
      }

      "render page with the correct hint text above the text field" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("#email-address-hint").text() shouldBe "We’ll only use this to confirm you sent a payment"
      }

      "render page with the correct hint text above the text field in welsh" in {
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.select("#email-address-hint").text() shouldBe "Byddwn ond yn defnyddio hwn i gadarnhau’ch bod wedi anfon taliad"
      }

      "render page with a Continue button" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("#submit").html shouldBe "Continue"
      }

      "render page with a Continue button in welsh" in {
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.select("#submit").html shouldBe "Yn eich blaen"
      }

      "render page with the 'Is this page not working properly?' link" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val link = document.select(".hmrc-report-technical-issue")
        link.text() shouldBe "Is this page not working properly? (opens in new tab)"
        link.attr("target") shouldBe "_blank"
      }

      "render page with the 'Is this page not working properly?' link in welsh" in {
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val link = document.select(".hmrc-report-technical-issue")
        link.text() shouldBe "A yw’r dudalen hon yn gweithio’n iawn? (yn agor tab newydd)"
        link.attr("target") shouldBe "_blank"
      }

      "be prepopulated if there is an email address in the session" in {
          def fakeRequestWithAddressInSession(journeyId: JourneyId = TestJourneys.PfSa.journeyBeforeBeginWebPayment._id): FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest()
              .withSessionId()
              .withEmailInSession(journeyId)
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)

        val result = systemUnderTest.renderPage(fakeRequestWithAddressInSession())
        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=email-address]").attr("value") shouldBe "blah@blah.com"
      }

    }

    "POST /email-address" - {

        def fakePostRequest(formData: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest("POST", "/email-address")
            .withSessionId()
            .withFormUrlEncodedBody(formData: _*)

        def fakePostRequestInWelsh(formData: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] =
          fakePostRequest(formData: _*)
            .withLangWelsh()

      "should return 303 SEE_OTHER and redirect to /address when a valid email address is submitted and email is added to the session" in {
        val validFormData = ("email-address", "blag@blah.com")
        val result = systemUnderTest.submit(fakePostRequest(validFormData))
        status(result) shouldBe Status.SEE_OTHER
        session(result).data.get("TestJourneyId-44f9-ad7f-01e1d3d8f151").map(_.replaceAll("\\s", "")) shouldBe Some("""{"email":"blag@blah.com"}""")
        redirectLocation(result) shouldBe Some("/address")
      }

      "should return 303 SEE_OTHER and redirect to /address when no email address to be submitted" in {
        val validFormData = ("email-address", "")
        val result = systemUnderTest.submit(fakePostRequest(validFormData))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some("/address")
      }

      "should return a 400 BAD_REQUEST when an invalid email address is submitted" in {
        val validFormData = ("email-address", "notALegitEmail")
        val result = systemUnderTest.submit(fakePostRequest(validFormData))
        status(result) shouldBe Status.BAD_REQUEST
      }

      "should return the correct Title when Error invalid email address submitted" in {
        val validFormData = ("email-address", "notALegitEmail")
        val result = systemUnderTest.submit(fakePostRequest(validFormData))
        val document = Jsoup.parse(contentAsString(result))
        document.title() shouldBe "Error: What is your email address? (optional) - Pay your Self Assessment - GOV.UK"
      }

      "should return the correct Title when Error invalid email address submitted in Welsh" in {
        val validFormData = ("email-address", "notALegitEmail")
        val result = systemUnderTest.submit(fakePostRequestInWelsh(validFormData))
        val document = Jsoup.parse(contentAsString(result))
        document.title() shouldBe "Gwall: Beth yw’ch cyfeiriad e-bost? (dewisol) - Talu eich Hunanasesiad - GOV.UK"
      }

      "should return html containing the correct error messages when an invalid email address is submitted" in {
        val validFormData = ("email-address", "notALegitEmail")
        val result = systemUnderTest.submit(fakePostRequest(validFormData))
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
        document.select(".govuk-error-summary__list").text() shouldBe "Enter a valid email address or leave it blank"
        document.select(".govuk-error-summary__list").select("a").attr("href") shouldBe "#email-address"
      }

      "should return html containing the correct error messages in welsh when an invalid email address is submitted" in {
        val validFormData = ("email-address", "notALegitEmail")
        val result = systemUnderTest.submit(fakePostRequestInWelsh(validFormData))
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-error-summary__title").text() shouldBe "Mae problem wedi codi"
        document.select(".govuk-error-summary__list").text() shouldBe "Nodwch gyfeiriad e-bost dilys neu gadewch yn wag"
        document.select(".govuk-error-summary__list").select("a").attr("href") shouldBe "#email-address"
      }
    }
  }

}
