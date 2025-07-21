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
import org.jsoup.nodes.Element
import org.scalatest.Assertion
import payapi.cardpaymentjourney.model.barclays.BarclaysOrder
import payapi.cardpaymentjourney.model.journey.{Journey, JourneySpecificData, Url}
import payapi.corcommon.model.barclays.TransactionReference
import payapi.corcommon.model.{AmountInPence, JourneyId, Origin, Origins}
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.cardpaymentfrontend.models.EmailAddress
import uk.gov.hmrc.cardpaymentfrontend.models.cardpayment.{BarclaycardAddress, CardPaymentInitiatePaymentRequest, CardPaymentInitiatePaymentResponse}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps.FakeRequestOps
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.{CardPaymentStub, PayApiStub}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys
import uk.gov.hmrc.cardpaymentfrontend.testsupport.{ItSpec, TestHelpers}

import scala.jdk.CollectionConverters.ListHasAsScala

class CheckYourAnswersControllerSpec extends ItSpec {

  val systemUnderTest: CheckYourAnswersController = app.injector.instanceOf[CheckYourAnswersController]

  def fakeRequest(journeyId: JourneyId = TestJourneys.PfSa.journeyBeforeBeginWebPayment._id): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest()
      .withSessionId()
      .withEmailAndAddressInSession(journeyId)

  def fakeRequestWelsh(journeyId: JourneyId = TestJourneys.PfSa.journeyBeforeBeginWebPayment._id): FakeRequest[AnyContentAsEmpty.type] = fakeRequest(journeyId).withLangWelsh()

  "GET /check-your-details" - {

    "should return 200 OK" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      status(result) shouldBe Status.OK
    }

    "should render the page with the language toggle" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val langToggleText: List[String] = document.select(".hmrc-language-select__list-item").eachText().asScala.toList
      langToggleText should contain theSameElementsAs List("English", "Newid yr iaith ir Gymraeg Cymraeg") //checking the visually hidden text, it's simpler
    }

    "show the Title tab correctly in English" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      document.title shouldBe "Check your details - Pay your Self Assessment - GOV.UK"
    }

    "show the Title tab correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      document.title shouldBe "Gwiriwch eich manylion - Talu eich Hunanasesiad - GOV.UK"
    }

    "show the Service Name banner title correctly in English" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      document.select(".govuk-header__service-name").html shouldBe "Pay your Self Assessment"
    }

    "show the Service Name banner title correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      document.select(".govuk-header__service-name").html shouldBe "Talu eich Hunanasesiad"
    }

    "should render the h1 correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      document.select("h1").text() shouldBe "Check your details"
    }

    "should render the h1 correctly in welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      document.select("h1").text() shouldBe "Gwiriwch eich manylion"
    }

    "should render the continue button" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      document.select("#submit").text() shouldBe "Continue"
    }

    "should render the continue button in welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      document.select("#submit").text() shouldBe "Yn eich blaen"
    }

      // derives correct row in summary list due to Origins that may include FDP.
      // The rows show as (with index value):
      // Payment date (optional) 0 or 1
      // Payment reference 1 or 2
      // Email address (optional) 2 or 3
      // Card billing address 3 or 4 (or 2 if there is no email address)
      def deriveReferenceRowIndex(origin: Origin): Int = {
        origin match {
          case Origins.BtaSa            => 1
          case Origins.PtaSa            => 1
          case Origins.ItSa             => 1
          case Origins.BtaCt            => 1
          case Origins.BtaVat           => 1
          case Origins.VcVatReturn      => 1
          case Origins.VcVatOther       => 1
          case Origins.Ppt              => 1
          case Origins.BtaEpayeBill     => 1
          case Origins.BtaEpayePenalty  => 1
          case Origins.BtaEpayeInterest => 1
          case Origins.BtaEpayeGeneral  => 1
          case Origins.BtaClass1aNi     => 1
          case _                        => 0
        }
      }

      def deriveAmountRowIndex(origin: Origin): Int = {
        origin match {
          case Origins.BtaSa            => 2
          case Origins.PtaSa            => 2
          case Origins.ItSa             => 2
          case Origins.AlcoholDuty      => 2
          case Origins.BtaCt            => 2
          case Origins.PfCt             => 2
          case Origins.PfEpayeNi        => 2
          case Origins.PfEpayeP11d      => 2
          case Origins.BtaVat           => 2
          case Origins.VcVatReturn      => 2
          case Origins.VcVatOther       => 3
          case Origins.Ppt              => 2
          case Origins.BtaEpayeBill     => 2
          case Origins.BtaEpayePenalty  => 2
          case Origins.BtaEpayeInterest => 2
          case Origins.BtaEpayeGeneral  => 2
          case Origins.BtaClass1aNi     => 3
          case _                        => 1
        }
      }

      def deriveEmailRowIndex(origin: Origin): Int = {
        origin match {
          case Origins.BtaSa            => 3
          case Origins.PtaSa            => 3
          case Origins.ItSa             => 3
          case Origins.AlcoholDuty      => 3
          case Origins.BtaCt            => 3
          case Origins.PfCt             => 3
          case Origins.PfEpayeNi        => 3
          case Origins.PfEpayeP11d      => 3
          case Origins.BtaVat           => 3
          case Origins.VcVatReturn      => 3
          case Origins.VcVatOther       => 4
          case Origins.Ppt              => 3
          case Origins.BtaEpayeBill     => 3
          case Origins.BtaEpayePenalty  => 3
          case Origins.BtaEpayeInterest => 3
          case Origins.BtaEpayeGeneral  => 3
          case Origins.BtaClass1aNi     => 4
          case _                        => 2
        }
      }

      def deriveCardBillingAddressRowIndex(origin: Origin): Int = {
        origin match {
          case Origins.BtaSa            => 4
          case Origins.PtaSa            => 4
          case Origins.ItSa             => 4
          case Origins.AlcoholDuty      => 4
          case Origins.BtaCt            => 4
          case Origins.PfCt             => 4
          case Origins.PfEpayeNi        => 4
          case Origins.PfEpayeP11d      => 4
          case Origins.BtaVat           => 4
          case Origins.VcVatReturn      => 4
          case Origins.VcVatOther       => 5
          case Origins.Ppt              => 4
          case Origins.BtaEpayeBill     => 4
          case Origins.BtaEpayePenalty  => 4
          case Origins.BtaEpayeInterest => 4
          case Origins.BtaEpayeGeneral  => 4
          case Origins.BtaClass1aNi     => 5
          case _                        => 3
        }
      }

    TestHelpers.implementedOrigins.foreach { origin: Origin =>

      val tdJourney: Journey[JourneySpecificData] = TestHelpers.deriveTestDataFromOrigin(origin).journeyBeforeBeginWebPayment

      s"[${origin.entryName}] should render the amount row correctly" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(fakeRequest(tdJourney._id))
        val document = Jsoup.parse(contentAsString(result))
        val amountRowIndex = deriveAmountRowIndex(origin)
        val amountRow = document.select(".govuk-summary-list__row").asScala.toList(amountRowIndex)
        assertRow(amountRow, "Total to pay", "£12.34", Some("Change"), Some("http://localhost:9056/pay/change-amount?showSummary=false&stayOnPayFrontend=false"))
      }

      s"[${origin.entryName}] should render the amount row correctly in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(fakeRequestWelsh(tdJourney._id))
        val document = Jsoup.parse(contentAsString(result))
        val amountRowIndex = deriveAmountRowIndex(origin)
        val amountRow = document.select(".govuk-summary-list__row").asScala.toList(amountRowIndex)
        assertRow(amountRow, "Cyfanswm i’w dalu", "£12.34", Some("Newid"), Some("http://localhost:9056/pay/change-amount?showSummary=false&stayOnPayFrontend=false"))
      }

      //hint, this is so test without email address row does not become obsolete if we changed the value. Stops anyone "forgetting" to update the test.
      val emailAddressKeyText: String = "Email address"
      val emailAddressKeyTextWelsh: String = "Cyfeiriad e-bost"

      s"[${origin.entryName}] render the email address row correctly when there is an email in session" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(fakeRequest())
        val document = Jsoup.parse(contentAsString(result))
        val emailRow: Element = document.select(".govuk-summary-list__row").asScala.toList(deriveEmailRowIndex(origin))
        assertRow(emailRow, emailAddressKeyText, "blah@blah.com", Some("Change"), Some("/email-address"))
      }

      s"[${origin.entryName}] render the email address row correctly when there is an email in session in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(fakeRequestWelsh())
        val document = Jsoup.parse(contentAsString(result))
        val emailRow: Element = document.select(".govuk-summary-list__row").asScala.toList(deriveEmailRowIndex(origin))
        assertRow(emailRow, emailAddressKeyTextWelsh, "blah@blah.com", Some("Newid"), Some("/email-address"))
      }

      s"[${origin.entryName}] not render the email address row when there is not an email in session" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(FakeRequest().withSessionId().withAddressInSession(tdJourney._id))
        contentAsString(result) shouldNot include(emailAddressKeyText)
      }

      s"[${origin.entryName}] render the card billing address row correctly" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(fakeRequest())
        val document = Jsoup.parse(contentAsString(result))
        val cardBillingAddressRow: Element = document.select(".govuk-summary-list__row").asScala.toList(deriveCardBillingAddressRowIndex(origin))
        assertRow(cardBillingAddressRow, "Card billing address", "line1 AA0AA0", Some("Change"), Some("/address"))
      }

      s"[${origin.entryName}] render the card billing address row correctly in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(fakeRequestWelsh())
        val document = Jsoup.parse(contentAsString(result))
        val cardBillingAddressRow: Element = document.select(".govuk-summary-list__row").asScala.toList(deriveCardBillingAddressRowIndex(origin))
        assertRow(cardBillingAddressRow, "Cyfeiriad bilio", "line1 AA0AA0", Some("Newid"), Some("/address"))
      }

      s"[${origin.entryName}] should redirect to the Address page if no Address in session" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(FakeRequest().withSessionId())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/address")
      }
    }

    "[PfSa] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfSa))
      assertRow(referenceRow, "Unique Taxpayer Reference (UTR)", "1234567895K", Some("Change"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfSa] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfSa))
      assertRow(referenceRow, "Cyfeirnod Unigryw y Trethdalwr (UTR)", "1234567895K", Some("Newid"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[BtaSa] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaSa))
      assertRow(referenceRow, "Unique Taxpayer Reference (UTR)", "1234567895K", None, None)
    }

    "[BtaSa] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaSa))
      assertRow(referenceRow, "Cyfeirnod Unigryw y Trethdalwr (UTR)", "1234567895K", None, None)
    }

    "[PtaSa] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PtaSa))
      assertRow(referenceRow, "Unique Taxpayer Reference (UTR)", "1234567895K", None, None)
    }

    "[PtaSa] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PtaSa))
      assertRow(referenceRow, "Cyfeirnod Unigryw y Trethdalwr (UTR)", "1234567895K", None, None)
    }

    "[PfAlcoholDuty] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfAlcoholDuty))
      assertRow(referenceRow, "Payment reference", "XMADP0123456789", Some("Change"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfAlcoholDuty] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfAlcoholDuty))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XMADP0123456789", Some("Newid"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[AlcoholDuty] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.AlcoholDuty.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.AlcoholDuty))
      assertRow(referenceRow, "Payment reference", "XMADP0123456789", None, None)
    }

    "[AlcoholDuty] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.AlcoholDuty.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.AlcoholDuty))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XMADP0123456789", None, None)
    }

    "[AlcoholDuty] should render the charge reference row correctly when it's available" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.AlcoholDuty.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(referenceRow, "Charge reference", "XE1234567890123", None, None)
    }

    "[AlcoholDuty] should render the charge reference row correctly in welsh when it's available" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.AlcoholDuty.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(referenceRow, "Cyfeirnod y tâl", "XE1234567890123", None, None)
    }

    "[PfVat] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVat.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfVat))
      assertRow(referenceRow, "VAT registration number", "999964805", None, None)
    }

    "[PfVat] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVat.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfVat))
      assertRow(referenceRow, "Rhif cofrestru TAW", "999964805", None, None)
    }

    "[PfVat] should render the charge reference row correctly when it's available" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatWithChargeReference.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(referenceRow, "Charge reference", "XE123456789012", None, None)
    }

    "[PfVat] should render the charge reference row correctly in welsh when it's available" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatWithChargeReference.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(referenceRow, "Cyfeirnod y tâl", "XE123456789012", None, None)
    }

    "[BtaVat] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaVat))
      assertRow(referenceRow, "VAT registration number", "999964805", None, None)
    }

    "[BtaVat] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaVat))
      assertRow(referenceRow, "Rhif cofrestru TAW", "999964805", None, None)
    }

    "[VcVatReturn] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.VcVatReturn))
      assertRow(referenceRow, "VAT registration number", "999964805", None, None)
    }

    "[VcVatReturn] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.VcVatReturn))
      assertRow(referenceRow, "Rhif cofrestru TAW", "999964805", None, None)
    }

    "[VcVatOther] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.VcVatOther))
      assertRow(referenceRow, "VAT registration number", "999964805", None, None)
    }

    "[VcVatOther] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.VcVatOther))
      assertRow(referenceRow, "Rhif cofrestru TAW", "999964805", None, None)
    }

    "[VcVatOther] should render the charge reference row correctly when it's available" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(2)
      assertRow(referenceRow, "Charge reference", "999964805", None, None)
    }

    "[VcVatOther] should render the charge reference row correctly in welsh when it's available" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(2)
      assertRow(referenceRow, "Cyfeirnod y tâl", "999964805", None, None)
    }

    "[ItSa] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.ItSa))
      assertRow(referenceRow, "Unique Taxpayer Reference (UTR)", "1234567895K", None, None)
    }

    "[ItSa] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.ItSa))
      assertRow(referenceRow, "Cyfeirnod Unigryw y Trethdalwr (UTR)", "1234567895K", None, None)
    }

    "[BtaCt] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaCt))
      assertRow(referenceRow, "Payment reference", "1097172564A00101A", None, None)
    }

    "[BtaCt] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaCt))
      assertRow(referenceRow, "Cyfeirnod y taliad", "1097172564A00101A", None, None)
    }

    "[PfCt] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfCt))
      assertRow(referenceRow, "Unique Taxpayer Reference (UTR)", "1097172564", None, None)
    }

    "[PfCt] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfCt))
      assertRow(referenceRow, "Cyfeirnod Unigryw y Trethdalwr (UTR)", "1097172564", None, None)
    }

    "[PfCt] should render the Payslip reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(referenceRow, "Payslip reference", "1097172564A00101A", Some("Change"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfCt] should render the Payslip reference row correctly in welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(referenceRow, "Cyfeirnod slip talu", "1097172564A00101A", Some("Newid"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[Ppt] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.Ppt))
      assertRow(referenceRow, "Reference number", "XAPPT0000012345", None, None)
    }

    "[Ppt] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.Ppt))
      assertRow(referenceRow, "Cyfeirnod", "XAPPT0000012345", None, None)
    }

    "[PfPpt] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      println(document.toString)
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfPpt))
      assertRow(referenceRow, "Reference number", "XAPPT0000012345", Some("Change"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfPpt] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfPpt))
      assertRow(referenceRow, "Cyfeirnod", "XAPPT0000012345", Some("Newid"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[BtaEpayeBill] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaEpayeBill))
      assertRow(referenceRow, "Payment reference", "123PH456789002702", None, None)
    }

    "[BtaEpayeBill] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaEpayeBill))
      assertRow(referenceRow, "Cyfeirnod y taliad", "123PH456789002702", None, None)
    }

    "[BtaEpayePenalty] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaEpayePenalty))
      assertRow(referenceRow, "Payment reference", "123PH45678900", None, None)
    }

    "[BtaEpayePenalty] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaEpayePenalty))
      assertRow(referenceRow, "Cyfeirnod y taliad", "123PH45678900", None, None)
    }

    "[BtaEpayeInterest] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaEpayeInterest))
      assertRow(referenceRow, "Payment reference", "XE123456789012", None, None)
    }

    "[BtaEpayeInterest] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaEpayeInterest))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE123456789012", None, None)
    }

    "[BtaEpayeGeneral] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaEpayeGeneral))
      assertRow(referenceRow, "Payment reference", "123PH456789002702", None, None)
    }

    "[BtaEpayeGeneral] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaEpayeGeneral))
      assertRow(referenceRow, "Cyfeirnod y taliad", "123PH456789002702", None, None)
    }

    "[BtaClass1aNi] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaClass1aNi))
      assertRow(referenceRow, "Payment reference", "123PH456789002713", None, None)
    }

    "[BtaClass1aNi] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaClass1aNi))
      assertRow(referenceRow, "Cyfeirnod y taliad", "123PH456789002713", None, None)
    }

    "[PfAmls] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAmls.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfAmls))
      assertRow(referenceRow, "Payment reference", "XE123456789012", Some("Change"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfAmls] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAmls.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfAmls))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE123456789012", Some("Newid"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[Amls] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.Amls.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.Amls))
      assertRow(referenceRow, "Payment reference", "XE123456789012", None, None)
    }

    "[Amls] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.Amls.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.Amls))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE123456789012", None, None)
    }

    "[BtaSa] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[BtaSa] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[PtaSa] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[PtaSa] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[BtaCt] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[BtaCt] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[Ppt] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[Ppt] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[PfEpayeNi] should render the Tax period row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfEpayeNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val taxPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(taxPeriodRow, "Tax period", "6 April 2024 to 5 July 2024 (first quarter)", Some("Change"), Some("http://localhost:9056/pay/change-employers-paye-period?fromCardPayment=true"))
    }

    "[PfEpayeNi] should render the Tax period row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfEpayeNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val taxPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(taxPeriodRow, "Cyfnod talu", "6 Ebrill 2024 i 5 Gorffennaf 2024 (chwarter cyntaf)", Some("Newid"), Some("http://localhost:9056/pay/change-employers-paye-period?fromCardPayment=true"))
    }

    "[PfEpayeP11d] should render the Tax year correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfEpayeP11d.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val taxPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(taxPeriodRow, "Tax year", "2024 to 2025", Some("Change"), Some("http://localhost:9056/pay/change-tax-year?fromCardPayment=true"))
    }

    "[PfEpayeP11d] should render the Tax year correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfEpayeP11d.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val taxPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(taxPeriodRow, "Blwyddyn dreth", "2024 i 2025", Some("Newid"), Some("http://localhost:9056/pay/change-tax-year?fromCardPayment=true"))
    }

    "[BtaClass1aNi] should render the Tax period correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val taxPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(2)
      assertRow(taxPeriodRow, "Tax period", "2026 to 2027", None, None)
    }

    "[BtaClass1aNi] should render the Tax period correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val taxPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(2)
      assertRow(taxPeriodRow, "Cyfnod talu", "2026 i 2027", None, None)
    }

    "[BtaVat] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[BtaVat] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[VcVatReturn] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[VcVatReturn] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[VcVatOther] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[VcVatOther] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[BtaClass1aNi] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[BtaClass1aNi] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "sanity check for implemented origins" in {
      // remember to add the singular tests for reference rows as well as fdp if applicable, they are not covered in the implementedOrigins forall tests
      TestHelpers.implementedOrigins.size shouldBe 26 withClue "** This dummy test is here to remind you to update the tests above. Bump up the expected number when an origin is added to implemented origins **"
    }

  }

  private def assertRow(element: Element, keyText: String, valueText: String, actionText: Option[String], actionHref: Option[String]): Assertion = {
    element.select(".govuk-summary-list__key").text() shouldBe keyText
    element.select(".govuk-summary-list__value").text() shouldBe valueText

    actionText.fold {
      element.toString should not contain "Change"
      element.select(".govuk-summary-list__actions").asScala.size shouldBe 0 withClue "Expected No change links but there was one"
    }(content => element.select(".govuk-summary-list__actions").text() shouldBe content)

    actionHref.fold(element.select(".govuk-summary-list__actions").select("a").text() shouldBe "")(href => element.select(".govuk-summary-list__actions").select("a").attr("href") shouldBe href)
  }

  "POST /check-your-details" - {

    "should redirect to the iframe page when there is an address in session" in {
      val cardPaymentInitiatePaymentRequest = CardPaymentInitiatePaymentRequest(
        redirectUrl         = "http://localhost:10155/return-to-hmrc",
        clientId            = "SAEE",
        purchaseDescription = "1234567895K",
        purchaseAmount      = AmountInPence(1234),
        billingAddress      = BarclaycardAddress(
          line1       = "line1",
          postCode    = "AA0AA0",
          countryCode = "GBR"
        ),
        emailAddress        = Some(EmailAddress("blah@blah.com")),
        transactionNumber   = "00001999999999"
      )
      val expectedCardPaymentInitiatePaymentResponse = CardPaymentInitiatePaymentResponse("http://localhost:10155/this-would-be-iframe", "sometransactionref")
      CardPaymentStub.InitiatePayment.stubForInitiatePayment2xx(cardPaymentInitiatePaymentRequest, expectedCardPaymentInitiatePaymentResponse)
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)

      val result = systemUnderTest.submit(fakeRequest(TestJourneys.PfSa.journeyBeforeBeginWebPayment._id))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/show-iframe?iframeUrl=http%3A%2F%2Flocalhost%3A10155%2Fthis-would-be-iframe")
    }

    "should redirect to iFrameUrl if PaymentStatus is Sent and there is an order present" in {
        def fakeRequestWithSentPaymentStatus(journeyId: JourneyId = TestJourneys.PfSa.journeyAfterBeginWebPayment._id): FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withSessionId().withEmailAndAddressInSession(journeyId)
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterBeginWebPayment.copy(order =
        Some(BarclaysOrder(
          transactionReference = TransactionReference("Some-transaction-ref"),
          iFrameUrl            = Url("http://localhost:9975/barclays/pages/paypage.jsf/600e1342-0714-4989-ac6c-c11c745f1ce6"),
          cardCategory         = None,
          commissionInPence    = None,
          paidOn               = None
        ))))
      val result = systemUnderTest.submit(fakeRequestWithSentPaymentStatus())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/show-iframe?iframeUrl=http%3A%2F%2Flocalhost%3A9975%2Fbarclays%2Fpages%2Fpaypage.jsf%2F600e1342-0714-4989-ac6c-c11c745f1ce6")
    }

    "should redirect to the Address page if there is no Address in session" in {
        def fakeRequestWithoutAddressInSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId()
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.submit(fakeRequestWithoutAddressInSession)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/address")
    }
  }

}
