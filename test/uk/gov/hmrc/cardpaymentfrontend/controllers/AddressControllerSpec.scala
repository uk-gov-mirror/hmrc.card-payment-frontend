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
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps.FakeRequestOps
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.PayApiStub
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys

import scala.jdk.CollectionConverters.ListHasAsScala

class AddressControllerSpec extends ItSpec {

  val systemUnderTest: AddressController = app.injector.instanceOf[AddressController]

  override def beforeEach(): Unit = {
    PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
    ()
  }

  "Address Controller" - {

    "GET /address" - {
      val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/address").withSessionId()
      val fakeGetRequestInWelsh = fakeGetRequest.withLangWelsh()

      "should return 200 OK" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        status(result) shouldBe Status.OK
      }

      "include the hmrc layout" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("html").hasClass("govuk-template") shouldBe true withClue "no govuk template"
      }

      "show the language toggle" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val langToggleText: List[String] = document.select(".hmrc-language-select__list-item").eachText().asScala.toList
        langToggleText should contain theSameElementsAs List("English", "Newid yr iaith ir Gymraeg Cymraeg") //checking the visually hidden text, it's simpler
      }

      "show the Title tab correctly in English" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Card billing address - Pay your Self Assessment - GOV.UK"
      }

      "show the Title tab correctly in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Cyfeiriad bilio’r cerdyn - Talu eich Hunanasesiad - GOV.UK"
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

      "show the heading correctly in English" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").html shouldBe "Card billing address" withClue "Page heading wrong in English"
      }

      "show the heading correctly in Welsh" in {
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").html shouldBe "Cyfeiriad bilio’r cerdyn" withClue "Page heading wrong in welsh"
      }

      "show the hint correctly in English" in {
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.selectXpath("//*[@id=\"main-content\"]/div/div/p[1]") contains
          "This billing address must match the address your card is registered with." withClue "Page hint 1 wrong in English"
        document.selectXpath("//*[@id=\"main-content\"]/div/div/p[2]") contains
          "If it does not, the payment will fail." withClue "Page hint 2 wrong in English"
      }

      "show the hint correctly in Welsh" in {
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.selectXpath("//*[@id=\"main-content\"]/div/div/p[1]") contains "Eich cyfeiriad bilio yw’r cyfeiriad y gwnaethoch gofrestru eich cerdyn ag ef" withClue "Page hint wrong in welsh"
      }

      "be prepopulated if there is an address in the session" in {
          def fakeRequestWithAddressInSession(journeyId: JourneyId = TestJourneys.PfSa.journeyBeforeBeginWebPayment._id): FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest()
              .withSessionId()
              .withAddressInSession(journeyId)
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)

        val result = systemUnderTest.renderPage(fakeRequestWithAddressInSession())
        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=line1]").attr("value") shouldBe "line1"
        document.select("input[name=line2]").attr("value") shouldBe "line2"
        document.select("input[name=city]").attr("value") shouldBe "city"
        document.select("input[name=postcode]").attr("value") shouldBe "AA0AA0"
        document.select("input[name=county]").attr("value") shouldBe "county"
        document.select("select[name=country] option[selected]").attr("value") shouldBe "GBR"

      }

    }

    "POST /address" - {
        def fakePostRequest(formData: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest("POST", "/address").withFormUrlEncodedBody(formData: _*).withSessionId()

        def fakePostRequestInWelsh(formData: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] =
          fakePostRequest(formData: _*).withLangWelsh()

      "should return 303 SEE_OTHER and redirect to /check-your-details when a valid address is submitted" in {
        val address = List(
          ("line1", "20 Fake Cottage"),
          ("line2", "Fake Street"),
          ("city", "Imaginaryshire"),
          ("county", "East Imaginationland"),
          ("postcode", "IM2 4HJ"),
          ("country", "GBR")
        )
        val result = systemUnderTest.submit(fakePostRequest(address: _*))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some("/check-your-details")
      }

      "Should show the correct error Title content in English" in {
        val firstLineMissing = List(
          ("line1", ""),
          ("line2", "Fake Street"),
          ("city", "Imaginaryshire"),
          ("county", "East Imaginationland"),
          ("postcode", "IM2 4HJ"),
          ("country", "GBR")
        )
        val result = systemUnderTest.submit(fakePostRequest(firstLineMissing: _*))
        val document = Jsoup.parse(contentAsString(result))
        document.title() shouldBe "Error: Card billing address - Pay your Self Assessment - GOV.UK"
      }

      "Should show the correct error Title content in Welsh" in {
        val firstLineMissing = List(
          ("line1", ""),
          ("line2", "Fake Street"),
          ("city", "Imaginaryshire"),
          ("county", "East Imaginationland"),
          ("postcode", "IM2 4HJ"),
          ("country", "GBR")
        )
        val result = systemUnderTest.submit(fakePostRequestInWelsh(firstLineMissing: _*))
        val document = Jsoup.parse(contentAsString(result))
        document.title() shouldBe "Gwall: Cyfeiriad bilio’r cerdyn - Talu eich Hunanasesiad - GOV.UK"
      }

      "should return html containing the correct error messages when first line of address is missing" in {
        val firstLineMissing = List(
          ("line1", ""),
          ("line2", "Fake Street"),
          ("city", "Imaginaryshire"),
          ("county", "East Imaginationland"),
          ("postcode", "IM2 4HJ"),
          ("country", "GBR")
        )
        val result = systemUnderTest.submit(fakePostRequest(firstLineMissing: _*))
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
        document.select(".govuk-error-summary__list").text() shouldBe "Enter the first line of the billing address"
        document.select(".govuk-error-summary__list").select("a").attr("href") shouldBe "#line1"
      }

      "should return html containing the correct error messages when postcode is missing" in {
        val postcodeMissing = List(
          ("line1", "20 Fake Cottage"),
          ("line2", "Fake Street"),
          ("city", "Imaginaryshire"),
          ("county", "East Imaginationland"),
          ("postcode", ""),
          ("country", "GBR")
        )
        val result = systemUnderTest.submit(fakePostRequest(postcodeMissing: _*))
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
        document.select(".govuk-error-summary__list").text() shouldBe "Enter your postcode"
        document.select(".govuk-error-summary__list").select("a").attr("href") shouldBe "#postcode"
      }

      "should return html containing the correct error messages when no country selected" in {
        val noCountrySelected = List(
          ("line1", "20 Fake Cottage"),
          ("line2", "Fake Street"),
          ("city", "Imaginaryshire"),
          ("county", "East Imaginationland"),
          ("postcode", "IM2 4HJ"),
          ("country", "")
        )
        val result = systemUnderTest.submit(fakePostRequest(noCountrySelected: _*))
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
        document.select(".govuk-error-summary__list").text() shouldBe "Select a country"
        document.select(".govuk-error-summary__list").select("a").attr("href") shouldBe "#country"
      }

      "should return a 400 BAD_REQUEST when an invalid address is submitted" in {
        val postcodeMissing = List(
          ("line1", "20 Fake Cottage"),
          ("line2", "Fake Street"),
          ("city", "Imaginaryshire"),
          ("county", "East Imaginationland"),
          ("postcode", "IM2 4HJ"),
          ("country", "")
        )
        val result = systemUnderTest.submit(fakePostRequest(postcodeMissing: _*))
        status(result) shouldBe Status.BAD_REQUEST
      }

      "should return html containing the correct error messages IN WELSH when first line of address is missing" in {
        val firstLineMissing = List(
          ("line1", ""),
          ("line2", "Fake Street"),
          ("city", "Imaginaryshire"),
          ("county", "East Imaginationland"),
          ("postcode", "IM2 4HJ"),
          ("country", "GBR")
        )
        val result = systemUnderTest.submit(fakePostRequestInWelsh(firstLineMissing: _*))
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-error-summary__title").text() shouldBe "Mae problem wedi codi"
        document.select(".govuk-error-summary__list").text() shouldBe "Nodwch linell gyntaf y cyfeiriad bilio"
        document.select(".govuk-error-summary__list").select("a").attr("href") shouldBe "#line1"
      }

      "should return html containing the correct error messages IN WELSH when postcode is missing" in {
        val postcodeMissing = List(
          ("line1", "20 Fake Cottage"),
          ("line2", "Fake Street"),
          ("city", "Imaginaryshire"),
          ("county", "East Imaginationland"),
          ("postcode", ""),
          ("country", "GBR")
        )
        val result = systemUnderTest.submit(fakePostRequestInWelsh(postcodeMissing: _*))
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-error-summary__title").text() shouldBe "Mae problem wedi codi"
        document.select(".govuk-error-summary__list").text() shouldBe "Nodwch eich cod post"
        document.select(".govuk-error-summary__list").select("a").attr("href") shouldBe "#postcode"
      }

      "should return html containing the correct error messages IN WELSH when no country selected" in {
        val noCountrySelected = List(
          ("line1", "20 Fake Cottage"),
          ("line2", "Fake Street"),
          ("city", "Imaginaryshire"),
          ("county", "East Imaginationland"),
          ("postcode", "IM2 4HJ"),
          ("country", "")
        )
        val result = systemUnderTest.submit(fakePostRequestInWelsh(noCountrySelected: _*))
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-error-summary__title").text() shouldBe "Mae problem wedi codi"
        document.select(".govuk-error-summary__list").text() shouldBe "Dewiswch eich gwlad"
        document.select(".govuk-error-summary__list").select("a").attr("href") shouldBe "#country"
      }

    }

  }

}
