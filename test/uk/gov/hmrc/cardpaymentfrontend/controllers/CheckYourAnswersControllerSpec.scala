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
import payapi.corcommon.model.{JourneyId, Origin, Origins}
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.cardpaymentfrontend.models.cardpayment.CardPaymentInitiatePaymentResponse
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
      langToggleText should contain theSameElementsAs List("English", "Newid yr iaith i’r Gymraeg Cymraeg") //checking the visually hidden text, it's simpler
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
          case Origins.BtaSa               => 1
          case Origins.PtaSa               => 1
          case Origins.ItSa                => 1
          case Origins.BtaCt               => 1
          case Origins.BtaVat              => 1
          case Origins.VcVatReturn         => 1
          case Origins.VcVatOther          => 1
          case Origins.Ppt                 => 1
          case Origins.BtaEpayeBill        => 1
          case Origins.BtaEpayePenalty     => 1
          case Origins.BtaEpayeInterest    => 1
          case Origins.BtaEpayeGeneral     => 1
          case Origins.BtaClass1aNi        => 1
          case Origins.EconomicCrimeLevy   => 1
          case Origins.BtaSdil             => 1
          case Origins.PtaSimpleAssessment => 1
          case Origins.NiEuVatOss          => 1
          case Origins.NiEuVatIoss         => 1
          case _                           => 0
        }
      }

      def deriveAmountRowIndex(origin: Origin): Int = {
        origin match {
          case Origins.BtaSa               => 2
          case Origins.PtaSa               => 2
          case Origins.ItSa                => 2
          case Origins.AlcoholDuty         => 2
          case Origins.BtaCt               => 2
          case Origins.PfCt                => 2
          case Origins.PfEpayeNi           => 2
          case Origins.PfEpayeP11d         => 2
          case Origins.BtaVat              => 2
          case Origins.VcVatReturn         => 2
          case Origins.VcVatOther          => 3
          case Origins.Ppt                 => 2
          case Origins.BtaEpayeBill        => 2
          case Origins.BtaEpayePenalty     => 2
          case Origins.BtaEpayeInterest    => 2
          case Origins.BtaEpayeGeneral     => 2
          case Origins.BtaClass1aNi        => 3
          case Origins.EconomicCrimeLevy   => 2
          case Origins.BtaSdil             => 2
          case Origins.PtaSimpleAssessment => 4
          case Origins.NiEuVatOss          => 4
          case Origins.PfNiEuVatOss        => 3
          case Origins.NiEuVatIoss         => 4
          case Origins.PfNiEuVatIoss       => 3
          case Origins.AppSimpleAssessment => 2
          case _                           => 1
        }
      }

      def deriveEmailRowIndex(origin: Origin): Int = {
        origin match {
          case Origins.BtaSa               => 3
          case Origins.PtaSa               => 3
          case Origins.ItSa                => 3
          case Origins.AlcoholDuty         => 3
          case Origins.BtaCt               => 3
          case Origins.PfCt                => 3
          case Origins.PfEpayeNi           => 3
          case Origins.PfEpayeP11d         => 3
          case Origins.BtaVat              => 3
          case Origins.VcVatReturn         => 3
          case Origins.VcVatOther          => 4
          case Origins.Ppt                 => 3
          case Origins.BtaEpayeBill        => 3
          case Origins.BtaEpayePenalty     => 3
          case Origins.BtaEpayeInterest    => 3
          case Origins.BtaEpayeGeneral     => 3
          case Origins.BtaClass1aNi        => 4
          case Origins.EconomicCrimeLevy   => 3
          case Origins.BtaSdil             => 3
          case Origins.PtaSimpleAssessment => 5
          case Origins.NiEuVatOss          => 5
          case Origins.PfNiEuVatOss        => 4
          case Origins.NiEuVatIoss         => 5
          case Origins.PfNiEuVatIoss       => 4
          case Origins.AppSimpleAssessment => 3
          case _                           => 2
        }
      }

      def deriveCardBillingAddressRowIndex(origin: Origin): Int = {
        origin match {
          case Origins.BtaSa               => 4
          case Origins.PtaSa               => 4
          case Origins.ItSa                => 4
          case Origins.AlcoholDuty         => 4
          case Origins.BtaCt               => 4
          case Origins.PfCt                => 4
          case Origins.PfEpayeNi           => 4
          case Origins.PfEpayeP11d         => 4
          case Origins.BtaVat              => 4
          case Origins.VcVatReturn         => 4
          case Origins.VcVatOther          => 5
          case Origins.Ppt                 => 4
          case Origins.BtaEpayeBill        => 4
          case Origins.BtaEpayePenalty     => 4
          case Origins.BtaEpayeInterest    => 4
          case Origins.BtaEpayeGeneral     => 4
          case Origins.BtaClass1aNi        => 5
          case Origins.EconomicCrimeLevy   => 4
          case Origins.BtaSdil             => 4
          case Origins.PtaSimpleAssessment => 6
          case Origins.NiEuVatOss          => 6
          case Origins.PfNiEuVatOss        => 5
          case Origins.NiEuVatIoss         => 6
          case Origins.PfNiEuVatIoss       => 5
          case Origins.AppSimpleAssessment => 4
          case _                           => 3
        }
      }

    TestHelpers.implementedOrigins.foreach { origin: Origin =>

      val tdJourney: Journey[JourneySpecificData] = TestHelpers.deriveTestDataFromOrigin(origin).journeyBeforeBeginWebPayment

      val shouldBeAbleToChangeAmount: Boolean = origin match {
        case Origins.WcSa               => false
        case Origins.WcCt               => false
        case Origins.WcVat              => false
        case Origins.WcSimpleAssessment => false
        case Origins.WcXref             => false
        case Origins.VatC2c             => false
        case Origins.WcEpayeLpp         => false
        case Origins.WcClass1aNi        => false
        case Origins.WcEpayeNi          => false
        case Origins.WcEpayeLateCis     => false
        case Origins.WcEpayeSeta        => false
        case Origins.Mib                => false
        case Origins.BcPngr             => false
        case _                          => true
      }

      s"[${origin.entryName}] should render the amount row correctly" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(fakeRequest(tdJourney._id))
        val document = Jsoup.parse(contentAsString(result))
        val amountRowIndex = deriveAmountRowIndex(origin)
        val amountRow = document.select(".govuk-summary-list__row").asScala.toList(amountRowIndex)
        assertRow(
          element    = amountRow,
          keyText    = "Total to pay",
          valueText  = "£12.34",
          actionText = if (shouldBeAbleToChangeAmount) Some("Change Total to pay") else None,
          actionHref = if (shouldBeAbleToChangeAmount) Some("http://localhost:9056/pay/change-amount?showSummary=false&stayOnPayFrontend=false") else None
        )
      }

      s"[${origin.entryName}] should render the amount row correctly in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(fakeRequestWelsh(tdJourney._id))
        val document = Jsoup.parse(contentAsString(result))
        val amountRowIndex = deriveAmountRowIndex(origin)
        val amountRow = document.select(".govuk-summary-list__row").asScala.toList(amountRowIndex)
        assertRow(
          element    = amountRow,
          keyText    = "Cyfanswm i’w dalu",
          valueText  = "£12.34",
          actionText = if (shouldBeAbleToChangeAmount) Some("Newid Cyfanswm i’w dalu") else None,
          actionHref = if (shouldBeAbleToChangeAmount) Some("http://localhost:9056/pay/change-amount?showSummary=false&stayOnPayFrontend=false") else None
        )
      }

      //hint, this is so test without email address row does not become obsolete if we changed the value. Stops anyone "forgetting" to update the test.
      val emailAddressKeyText: String = "Email address"
      val emailAddressKeyTextWelsh: String = "Cyfeiriad e-bost"

      s"[${origin.entryName}] render the email address row correctly when there is an email in session" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(fakeRequest())
        val document = Jsoup.parse(contentAsString(result))
        val emailRow: Element = document.select(".govuk-summary-list__row").asScala.toList(deriveEmailRowIndex(origin))
        assertRow(emailRow, emailAddressKeyText, "blah@blah.com", Some("Change Email address"), Some("/pay-by-card/email-address"))
      }

      s"[${origin.entryName}] render the email address row correctly when there is an email in session in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(fakeRequestWelsh())
        val document = Jsoup.parse(contentAsString(result))
        val emailRow: Element = document.select(".govuk-summary-list__row").asScala.toList(deriveEmailRowIndex(origin))
        assertRow(emailRow, emailAddressKeyTextWelsh, "blah@blah.com", Some("Newid Cyfeiriad e-bost"), Some("/pay-by-card/email-address"))
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
        assertRow(cardBillingAddressRow, "Card billing address", "line1 AA0AA0", Some("Change Card billing address"), Some("/pay-by-card/address"))
      }

      s"[${origin.entryName}] render the card billing address row correctly in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(fakeRequestWelsh())
        val document = Jsoup.parse(contentAsString(result))
        val cardBillingAddressRow: Element = document.select(".govuk-summary-list__row").asScala.toList(deriveCardBillingAddressRowIndex(origin))
        assertRow(cardBillingAddressRow, "Cyfeiriad bilio", "line1 AA0AA0", Some("Newid Cyfeiriad bilio"), Some("/pay-by-card/address"))
      }

      s"[${origin.entryName}] should redirect to the Address page if no Address in session" in {
        PayApiStub.stubForFindBySessionId2xx(tdJourney)
        val result = systemUnderTest.renderPage(FakeRequest().withSessionId())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/pay-by-card/address")
      }
    }

    "[PfSa] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfSa))
      assertRow(referenceRow, "Unique Taxpayer Reference (UTR)", "1234567895K", Some("Change Unique Taxpayer Reference (UTR)"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfSa] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfSa))
      assertRow(referenceRow, "Cyfeirnod Unigryw y Trethdalwr (UTR)", "1234567895K", Some("Newid Cyfeirnod Unigryw y Trethdalwr (UTR)"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
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
      assertRow(referenceRow, "Payment reference", "XMADP0123456789", Some("Change Payment reference"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfAlcoholDuty] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfAlcoholDuty))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XMADP0123456789", Some("Newid Cyfeirnod y taliad"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
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

    "[WcVat] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcVat.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcVat))
      assertRow(referenceRow, "VAT registration number", "999964805", None, None)
    }

    "[WcVat] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcVat.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcVat))
      assertRow(referenceRow, "Rhif cofrestru TAW", "999964805", None, None)
    }

    "[WcVat] should render the charge reference row correctly when it's available" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcVatWithChargeReference.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(referenceRow, "VAT surcharge or penalty reference", "XE123456789012", None, None)
    }

    "[WcVat] should render the charge reference row correctly in welsh when it's available" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcVatWithChargeReference.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(referenceRow, "Gordal TAW neu cyfeirnod y gosb", "XE123456789012", None, None)
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

    "[WcSa] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcSa))
      assertRow(referenceRow, "Unique Taxpayer Reference (UTR)", "1234567895K", None, None)
    }

    "[WcSa] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcSa))
      assertRow(referenceRow, "Cyfeirnod Unigryw y Trethdalwr (UTR)", "1234567895K", None, None)
    }

    "[WcCt] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcCt))
      assertRow(referenceRow, "Payment reference", "1097172564A00101A", None, None)
    }

    "[WcCt] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcCt))
      assertRow(referenceRow, "Cyfeirnod y taliad", "1097172564A00101A", None, None)
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
      assertRow(referenceRow, "Payslip reference", "1097172564A00101A", Some("Change Payslip reference"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfCt] should render the Payslip reference row correctly in welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(referenceRow, "Cyfeirnod slip talu", "1097172564A00101A", Some("Newid Cyfeirnod slip talu"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
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
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfPpt))
      assertRow(referenceRow, "Reference number", "XAPPT0000012345", Some("Change Reference number"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfPpt] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfPpt))
      assertRow(referenceRow, "Cyfeirnod", "XAPPT0000012345", Some("Newid Cyfeirnod"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
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
      assertRow(referenceRow, "Payment reference", "XE123456789012", Some("Change Payment reference"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfAmls] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAmls.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfAmls))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE123456789012", Some("Newid Cyfeirnod y taliad"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
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

    "[PfEconomicCrimeLevy] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfEconomicCrimeLevy.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfEconomicCrimeLevy))
      assertRow(referenceRow, "Reference number", "XE123456789012", Some("Change Reference number"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfEconomicCrimeLevy] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfEconomicCrimeLevy.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfEconomicCrimeLevy))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE123456789012", Some("Newid Cyfeirnod y taliad"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[EconomicCrimeLevy] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.EconomicCrimeLevy.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.EconomicCrimeLevy))
      assertRow(referenceRow, "Reference number", "XE123456789012", None, None)
    }

    "[EconomicCrimeLevy] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.EconomicCrimeLevy.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.EconomicCrimeLevy))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE123456789012", None, None)
    }

    "[PfVatC2c] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatC2c.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfVatC2c))
      assertRow(referenceRow, "Reference number", "XVC1A2B3C4D5E6F", Some("Change Reference number"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfVatC2c] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatC2c.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfVatC2c))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XVC1A2B3C4D5E6F", Some("Newid Cyfeirnod y taliad"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[VatC2c] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VatC2c.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.VatC2c))
      assertRow(referenceRow, "Reference number", "XVC1A2B3C4D5E6F", None, None)
    }

    "[VatC2c] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VatC2c.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.VatC2c))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XVC1A2B3C4D5E6F", None, None)
    }

    "[BtaSa] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change Payment date"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[BtaSa] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid Dyddiad talu"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[PtaSa] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change Payment date"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[PtaSa] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid Dyddiad talu"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[BtaCt] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change Payment date"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[BtaCt] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid Dyddiad talu"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[Ppt] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change Payment date"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[Ppt] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid Dyddiad talu"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[PfEpayeNi] should render the Tax period row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfEpayeNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val taxPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(taxPeriodRow, "Tax period", "6 April 2024 to 5 July 2024 (first quarter)", Some("Change Tax period"), Some("http://localhost:9056/pay/change-employers-paye-period?fromCardPayment=true"))
    }

    "[PfEpayeNi] should render the Tax period row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfEpayeNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val taxPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(taxPeriodRow, "Cyfnod talu", "6 Ebrill 2024 i 5 Gorffennaf 2024 (chwarter cyntaf)", Some("Newid Cyfnod talu"), Some("http://localhost:9056/pay/change-employers-paye-period?fromCardPayment=true"))
    }

    "[PfEpayeP11d] should render the Tax year correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfEpayeP11d.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val taxPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(taxPeriodRow, "Tax year", "2024 to 2025", Some("Change Tax year"), Some("http://localhost:9056/pay/change-tax-year?fromCardPayment=true"))
    }

    "[PfEpayeP11d] should render the Tax year correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfEpayeP11d.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val taxPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(taxPeriodRow, "Blwyddyn dreth", "2024 i 2025", Some("Newid Blwyddyn dreth"), Some("http://localhost:9056/pay/change-tax-year?fromCardPayment=true"))
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

    "[NiEuVatOss] should render the Tax period correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatOss.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val returnPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(3)
      assertRow(returnPeriodRow, "Return period", "October to December 2024", None, None)
    }

    "[PfNiEuVatOss] should render the Tax period correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatOss.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val returnPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(2)
      assertRow(returnPeriodRow, "Return period", "October to December 2024", Some("Change Return period"), Some("http://localhost:9056/pay/change-vat-period?fromCardPayment=true"))
    }

    "[NiEuVatIoss] should render the Tax period correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatIoss.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val returnPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(3)
      assertRow(returnPeriodRow, "Return period", "June 2024", None, None)
    }

    "[PfNiEuVatIoss] should render the Tax period correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatIoss.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val returnPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(2)
      assertRow(returnPeriodRow, "Return period", "June 2024", Some("Change Return period"), Some("http://localhost:9056/pay/change-ioss-vat-period?fromCardPayment=true"))
    }

    "[BtaVat] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change Payment date"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[BtaVat] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid Dyddiad talu"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[VcVatReturn] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change Payment date"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[VcVatReturn] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid Dyddiad talu"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[VcVatOther] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change Payment date"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[VcVatOther] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid Dyddiad talu"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[BtaClass1aNi] should render the payment date row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Payment date", "Today", Some("Change Payment date"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[BtaClass1aNi] should render the payment date row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val paymentDateRow = document.select(".govuk-summary-list__row").asScala.toList(0)
      assertRow(paymentDateRow, "Dyddiad talu", "Heddiw", Some("Newid Dyddiad talu"), Some("http://localhost:9056/pay/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"))
    }

    "[PfSdlt] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdlt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfSdlt))
      assertRow(referenceRow, "Unique Transaction Reference Number (UTRN)", "123456789MA", None, None)
    }

    "[PfSdlt] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdlt.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfSdlt))
      assertRow(referenceRow, "Cyfeirnod Unigryw y Trafodyn (UTRN)", "123456789MA", None, None)
    }

    "[CapitalGainsTax] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.CapitalGainsTax.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.CapitalGainsTax))
      assertRow(referenceRow, "Payment reference", "XVCGTP001000290", None, None)
    }

    "[CapitalGainsTax] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.CapitalGainsTax.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.CapitalGainsTax))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XVCGTP001000290", None, None)
    }

    "[WcSimpleAssessment] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSimpleAssessment.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcSimpleAssessment))
      assertRow(referenceRow, "Payment reference", "XE123456789012", None, None)
    }

    "[WcSimpleAssessment] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSimpleAssessment.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcSimpleAssessment))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE123456789012", None, None)
    }

    "[WcClass1aNi] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcClass1aNi))
      assertRow(referenceRow, "Payment reference", "123PH456789002713", None, None)
    }

    "[WcClass1aNi] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcClass1aNi))
      assertRow(referenceRow, "Cyfeirnod y taliad", "123PH456789002713", None, None)
    }

    "[WcXref] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcXref.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcXref))
      assertRow(referenceRow, "Payment reference", "XE123456789012", None, None)
    }

    "[WcXref] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcXref.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcXref))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE123456789012", None, None)
    }

    "[WcEpayeLpp] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcEpayeLpp))
      assertRow(referenceRow, "Payment reference", "XE123456789012", None, None)
    }

    "[WcEpayeLpp] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcEpayeLpp))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE123456789012", None, None)
    }

    "[WcEpayeSeta] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcEpayeSeta))
      assertRow(referenceRow, "Payment reference", "XE123456789012", None, None)
    }

    "[WcEpayeSeta] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcEpayeSeta))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE123456789012", None, None)
    }

    "[WcEpayeNi] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcEpayeNi))
      assertRow(referenceRow, "Payment reference", "123PH456789002501", None, None)
    }

    "[WcEpayeNi] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcEpayeNi))
      assertRow(referenceRow, "Cyfeirnod y taliad", "123PH456789002501", None, None)
    }

    "[WcEpayeLateCis] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcEpayeLateCis))
      assertRow(referenceRow, "Penalty reference number", "XE123456789012", None, None)
    }

    "[WcEpayeLateCis] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcEpayeLateCis))
      assertRow(referenceRow, "Cyfeirnod y gosb", "XE123456789012", None, None)
    }

    "[PfChildBenefitRepayments] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfChildBenefitRepayments))
      assertRow(referenceRow, "Child benefit overpayment reference", "YA123456789123", Some("Change Child benefit overpayment reference"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfChildBenefitRepayments] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfChildBenefitRepayments))
      assertRow(referenceRow, "Cyfeirnod gordaliad Budd-dal Plant", "YA123456789123", Some("Newid Cyfeirnod gordaliad Budd-dal Plant"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[WcChildBenefitRepayments] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcChildBenefitRepayments.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcChildBenefitRepayments))
      assertRow(referenceRow, "Child benefit overpayment reference", "YA123456789123", None, None)
    }

    "[WcChildBenefitRepayments] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcChildBenefitRepayments.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.WcChildBenefitRepayments))
      assertRow(referenceRow, "Cyfeirnod gordaliad Budd-dal Plant", "YA123456789123", None, None)
    }

    "[BtaSdil] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaSdil))
      assertRow(referenceRow, "Payment reference", "XE1234567890123", None, None)
    }

    "[BtaSdil] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BtaSdil))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE1234567890123", None, None)
    }

    "[PfSdil] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfSdil))
      assertRow(referenceRow, "Payment reference", "XE1234567890123", Some("Change Payment reference"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfSdil] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfSdil))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE1234567890123", Some("Newid Cyfeirnod y taliad"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfP800] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfP800.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfP800))
      assertRow(referenceRow, "Reference number", "MA000003AP8002027", None, None)
    }

    "[PfP800] should render the payment reference row correctly in welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfP800.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfP800))
      assertRow(referenceRow, "Cyfeirnod", "MA000003AP8002027", None, None)
    }

    "[PfP800] should render the payment reference rows correctly (i.e. show the p800ChargeRef additionally, when there is one)" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfP800.journeyWithP800ChargeRefBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(referenceRow, "Charge reference", "BC007010065114", None, None)
    }

    "[PfP800] should render the payment reference rows correctly in welsh (i.e. show the p800ChargeRef additionally, when there is one)" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfP800.journeyWithP800ChargeRefBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(referenceRow, "Cyfeirnod y tâl", "BC007010065114", None, None)
    }

    "[PtaP800] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaP800.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PtaP800))
      assertRow(referenceRow, "Reference number", "MA000003AP8002027", None, None)
    }

    "[PtaP800] should render the payment reference row correctly in welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaP800.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PtaP800))
      assertRow(referenceRow, "Cyfeirnod", "MA000003AP8002027", None, None)
    }

    "[PtaP800] should render the payment reference rows correctly (i.e. show the p800ChargeRef additionally, when there is one)" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaP800.journeyWithP800ChargeRefBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(referenceRow, "Charge reference", "BC007010065114", None, None)
    }

    "[PtaP800] should render the payment reference rows correctly in welsh (i.e. show the p800ChargeRef additionally, when there is one)" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaP800.journeyWithP800ChargeRefBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(referenceRow, "Cyfeirnod y tâl", "BC007010065114", None, None)
    }

    "[PfSimpleAssessment] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSimpleAssessment.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfSimpleAssessment))
      assertRow(referenceRow, "Reference number", "XE123456789012", None, None)
    }

    "[PfSimpleAssessment] should render the payment reference row correctly in welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSimpleAssessment.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfSimpleAssessment))
      assertRow(referenceRow, "Cyfeirnod", "XE123456789012", None, None)
    }

    "[PtaSimpleAssessment] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSimpleAssessment.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PtaSimpleAssessment))
      assertRow(referenceRow, "Payment reference", "MA000003AP3022027", None, None)
    }

    "[PtaSimpleAssessment] should render the payment reference row correctly in welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSimpleAssessment.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PtaSimpleAssessment))
      assertRow(referenceRow, "Cyfeirnod y taliad", "MA000003AP3022027", None, None)
    }

    "[PtaSimpleAssessment] should render the charge reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSimpleAssessment.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(2)
      assertRow(referenceRow, "Charge reference", "BC007010065114", None, None)
    }

    "[PtaSimpleAssessment] should render the charge reference row correctly in welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSimpleAssessment.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(2)
      assertRow(referenceRow, "Cyfeirnod y tâl", "BC007010065114", None, None)
    }

    "[PtaSimpleAssessment] should render the tax year row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSimpleAssessment.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(3)
      assertRow(referenceRow, "Tax year", "6 April 2027 to 5 April 2028", None, None)
    }

    "[PtaSimpleAssessment] should render the tax year row correctly in welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSimpleAssessment.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(3)
      assertRow(referenceRow, "Blwyddyn dreth", "6 Ebrill 2027 i 5 Ebrill 2028", None, None)
    }

    "[PfJobRetentionScheme] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfJobRetentionScheme))
      assertRow(referenceRow, "Payment reference", "XJRS12345678901", Some("Change Payment reference"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfJobRetentionScheme] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfJobRetentionScheme))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XJRS12345678901", Some("Newid Cyfeirnod y taliad"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[JrsJobRetentionScheme] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.JrsJobRetentionScheme))
      assertRow(referenceRow, "Payment reference", "XJRS12345678901", None, None)
    }

    "[JrsJobRetentionScheme] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.JrsJobRetentionScheme))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XJRS12345678901", None, None)
    }

    "[PfCds] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCds.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfCds))
      assertRow(referenceRow, "CDS Reference Number", "CDSI191234567890", Some("Change CDS Reference Number"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[NiEuVatOss] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatOss.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.NiEuVatOss))
      assertRow(referenceRow, "Payment reference", "NI101747641Q424", None, None)
    }

    "[PfNiEuVatOss] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatOss.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfNiEuVatOss))
      assertRow(referenceRow, "Payment reference", "NI101747641Q424", None, None)
    }

    "[NiEuVatIoss] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatIoss.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.NiEuVatIoss))
      assertRow(referenceRow, "Payment reference", "IM1234567890M0624", None, None)
    }

    "[PfNiEuVatIoss] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatIoss.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfNiEuVatIoss))
      assertRow(referenceRow, "Payment reference", "IM1234567890M0624", None, None)
    }

    "[NiEuVatOss] should render the VAT Number correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatOss.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val returnPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(2)
      assertRow(returnPeriodRow, "VAT Number", "101747641", None, None)
    }

    "[PfNiEuVatOss] should render the VAT Number correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatOss.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val returnPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(returnPeriodRow, "VAT Number", "101747641", None, None)
    }

    "[NiEuVatIoss] should render the IOSS Number correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatIoss.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val returnPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(2)
      assertRow(returnPeriodRow, "IOSS Number", "IM1234567890", None, None)
    }

    "[PfNiEuVatIoss] should render the IOSS Number correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatIoss.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val returnPeriodRow = document.select(".govuk-summary-list__row").asScala.toList(1)
      assertRow(returnPeriodRow, "IOSS Number", "IM1234567890", None, None)
    }

    "[AppSa] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.AppSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.AppSa))
      assertRow(referenceRow, "Unique Taxpayer Reference (UTR)", "1234567890", None, None)
    }

    "[AppSa] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.AppSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.AppSa))
      assertRow(referenceRow, "Cyfeirnod Unigryw y Trethdalwr (UTR)", "1234567890", None, None)
    }

    "[AppSimpleAssessment] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.AppSimpleAssessment.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.AppSimpleAssessment))
      assertRow(referenceRow, "Payment reference", "MA000003AP3022023", None, None)
    }

    "[AppSimpleAssessment] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.AppSimpleAssessment.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.AppSimpleAssessment))
      assertRow(referenceRow, "Cyfeirnod y taliad", "MA000003AP3022023", None, None)
    }

    "[Mib] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.Mib.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.Mib))
      assertRow(referenceRow, "Declaration reference", "MIBI1234567891", None, None)
    }

    "[Mib] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.Mib.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.Mib))
      assertRow(referenceRow, "Cyfeirnod y taliad", "MIBI1234567891", None, None)
    }

    "[BcPngr] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BcPngr.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BcPngr))
      assertRow(referenceRow, "Reference number", "XAPR9876543210", None, None)
    }

    "[BcPngr] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.BcPngr.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.BcPngr))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XAPR9876543210", None, None)
    }

    "[PfTpes] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfTpes.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfTpes))
      assertRow(referenceRow, "Reference number", "XE123456789012", Some("Change Reference number"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfTpes] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfTpes.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfTpes))
      assertRow(referenceRow, "Cyfeirnod", "XE123456789012", Some("Newid Cyfeirnod"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfMgd] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfMgd.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfMgd))
      assertRow(referenceRow, "Reference number", "XE123456789012", Some("Change Reference number"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfMgd] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfMgd.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfMgd))
      assertRow(referenceRow, "Cyfeirnod", "XE123456789012", Some("Newid Cyfeirnod"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfGbPbRgDuty] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfGbPbRgDuty.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfGbPbRgDuty))
      assertRow(referenceRow, "Reference number", "XE123456789012", Some("Change Reference number"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfGbPbRgDuty] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfGbPbRgDuty.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfGbPbRgDuty))
      assertRow(referenceRow, "Cyfeirnod", "XE123456789012", Some("Newid Cyfeirnod"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfTrust] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfTrust.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfTrust))
      assertRow(referenceRow, "Reference number", "XE123456789012", Some("Change Reference number"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfTrust] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfTrust.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfTrust))
      assertRow(referenceRow, "Cyfeirnod", "XE123456789012", Some("Newid Cyfeirnod"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfPsAdmin] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPsAdmin.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfPsAdmin))
      assertRow(referenceRow, "Reference number", "XE123456789012", Some("Change Reference number"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfPsAdmin] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPsAdmin.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfPsAdmin))
      assertRow(referenceRow, "Cyfeirnod", "XE123456789012", Some("Newid Cyfeirnod"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfOther] should render the payment reference row correctly" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfOther.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequest())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfOther))
      assertRow(referenceRow, "Payment reference", "XE123456789012", Some("Change Payment reference"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "[PfOther] should render the payment reference row correctly in Welsh" in {
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfOther.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.renderPage(fakeRequestWelsh())
      val document = Jsoup.parse(contentAsString(result))
      val referenceRow = document.select(".govuk-summary-list__row").asScala.toList(deriveReferenceRowIndex(Origins.PfOther))
      assertRow(referenceRow, "Cyfeirnod y taliad", "XE123456789012", Some("Newid Cyfeirnod y taliad"), Some("http://localhost:9056/pay/pay-by-card-change-reference-number"))
    }

    "sanity check for implemented origins" in {
      // remember to add the singular tests for reference rows as well as fdp if applicable, they are not covered in the implementedOrigins forall tests
      TestHelpers.implementedOrigins.size shouldBe 67 withClue "** This dummy test is here to remind you to update the tests above. Bump up the expected number when an origin is added to implemented origins **"
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
      val expectedCardPaymentInitiatePaymentResponse = CardPaymentInitiatePaymentResponse("http://localhost:10155/this-would-be-iframe", "sometransactionref")
      CardPaymentStub.InitiatePayment.stubForInitiatePayment2xx(expectedCardPaymentInitiatePaymentResponse)
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)

      val result = systemUnderTest.submit(fakeRequest(TestJourneys.PfSa.journeyBeforeBeginWebPayment._id))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/pay-by-card/card-details?iframeUrl=http%3A%2F%2Flocalhost%3A10155%2Fthis-would-be-iframe")
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
      redirectLocation(result) shouldBe Some("/pay-by-card/card-details?iframeUrl=http%3A%2F%2Flocalhost%3A9975%2Fbarclays%2Fpages%2Fpaypage.jsf%2F600e1342-0714-4989-ac6c-c11c745f1ce6")
    }

    "should redirect to the Address page if there is no Address in session" in {
        def fakeRequestWithoutAddressInSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId()
      PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
      val result = systemUnderTest.submit(fakeRequestWithoutAddressInSession)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/pay-by-card/address")
    }
  }

}
