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
import org.jsoup.select.Elements
import payapi.cardpaymentjourney.model.barclays.BarclaysOrder
import payapi.cardpaymentjourney.model.journey.{Journey, JourneySpecificData, Url}
import payapi.corcommon.model.barclays.{CardCategories, TransactionReference}
import payapi.corcommon.model.{AmountInPence, JourneyId, Origin, Origins}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.Status
import uk.gov.hmrc.cardpaymentfrontend.controllers.PaymentCompleteControllerSpec.{TestScenarioInfo, originToTdAndSummaryListRows}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps._
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.{EmailStub, PayApiStub}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys
import uk.gov.hmrc.cardpaymentfrontend.testsupport.{ItSpec, TestHelpers}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow, Value}

import java.time.LocalDateTime
import scala.jdk.CollectionConverters.ListHasAsScala

class PaymentCompleteControllerSpec extends ItSpec {

  "PaymentCompleteController" - {

    val systemUnderTest = app.injector.instanceOf[PaymentCompleteController]
    val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/payment-complete").withSessionId()
    val fakeGetRequestInWelsh: FakeRequest[AnyContentAsEmpty.type] = fakeGetRequest.withLangWelsh()

    "GET /payment-complete" - {

      "render the page with the language toggle" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        status(result) shouldBe Status.OK
        val document = Jsoup.parse(contentAsString(result))
        val langToggleText: List[String] = document.select(".hmrc-language-select__list-item").eachText().asScala.toList
        langToggleText should contain theSameElementsAs List("English", "Newid yr iaith ir Gymraeg Cymraeg") //checking the visually hidden text, it's simpler
      }

      "show the Title tab correctly in English" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Payment received by HMRC - Pay your Self Assessment - GOV.UK"
      }

      "show the Title tab correctly in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Taliad wedi dod i law CThEM - Talu eich Hunanasesiad - GOV.UK"
      }

      "render the page without a back link" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val backLink: Elements = document.select(".govuk-back-link")
        backLink.size() shouldBe 0
      }

      "render the h1 panel correctly" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val panel = document.body().select(".govuk-panel--confirmation")
        panel.select("h1").text() shouldBe "Payment received by HMRC"
        panel.select(".govuk-panel__body").html() shouldBe "Your payment reference\n<br>\n<strong>1234567895K</strong>"
      }

      "render the h1 panel correctly in welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val panel = document.body().select(".govuk-panel--confirmation")
        panel.select("h1").text() shouldBe "Taliad wedi dod i law CThEM"
        panel.select(".govuk-panel__body").html() shouldBe "Eich cyfeirnod talu\n<br>\n<strong>1234567895K</strong>"
      }

      "render paragraph about email address when email is provided" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val requestForTest = fakeGetRequest.withEmailInSession(JourneyId("TestJourneyId-44f9-ad7f-01e1d3d8f151"))
        val result = systemUnderTest.renderPage(requestForTest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("#email-paragraph").html() shouldBe "We have sent a confirmation email to <strong>blah@blah.com</strong>"
      }

      "render paragraph about email address in welsh when email is provided" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val requestForTest = fakeGetRequestInWelsh.withEmailInSession(JourneyId("TestJourneyId-44f9-ad7f-01e1d3d8f151"))
        val result = systemUnderTest.renderPage(requestForTest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("#email-paragraph").html() shouldBe "Rydym wedi anfon e-bost cadarnhau <strong>blah@blah.com</strong>"
      }

      "not render paragraph about email address when email is not provided" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("#email-paragraph").size() shouldBe 0
      }

      "render the print link correctly" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val printLinkWrapper = document.select("#print-link-wrapper")
        printLinkWrapper.hasClass("govuk-!-display-none-print") shouldBe true
        printLinkWrapper.hasClass("js-visible") shouldBe true
        val printLink = printLinkWrapper.select("a")
        printLink.hasClass("govuk-link") shouldBe true
        printLink.attr("href") shouldBe "#print-dialogue"
        printLink.text() shouldBe "Print your payment confirmation"
      }

      "render the print link correctly in welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val printLinkWrapper = document.select("#print-link-wrapper")
        printLinkWrapper.hasClass("govuk-!-display-none-print") shouldBe true
        printLinkWrapper.hasClass("js-visible") shouldBe true
        val printLink = printLinkWrapper.select("a")
        printLink.hasClass("govuk-link") shouldBe true
        printLink.attr("href") shouldBe "#print-dialogue"
        printLink.text() shouldBe "Argraffwch cadarnhad o’ch taliad"
      }

      "render the survey content correctly" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val surveyWrapper = document.select("#survey-wrapper")
        surveyWrapper.hasClass("govuk-!-display-none-print") shouldBe true
        surveyWrapper.select("h2").text() shouldBe "Help us improve our services"
        surveyWrapper.select("#survey-content").text() shouldBe "We use your feedback to make our services better."
        surveyWrapper.select("#survey-link-wrapper").html() shouldBe """<a class="govuk-link" href="/start-payment-survey">Tell us what you think of this service</a> (takes 30 seconds)"""
      }

      "render the survey content correctly in welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val surveyWrapper = document.select("#survey-wrapper")
        surveyWrapper.hasClass("govuk-!-display-none-print") shouldBe true
        surveyWrapper.select("h2").text() shouldBe "Helpwch ni i wella ein gwasanaethau"
        surveyWrapper.select("#survey-content").text() shouldBe "Rydym yn defnyddio’ch adborth i wella ein gwasanaethau."
        surveyWrapper.select("#survey-link-wrapper").html() shouldBe """<a class="govuk-link" href="/start-payment-survey">Rhowch wybod i ni beth yw eich barn am y gwasanaeth hwn</a> (mae’n cymryd 30 eiliad)"""
      }

      "should not send an email if there is not one in the session" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        status(result) shouldBe Status.OK
        EmailStub.verifyEmailWasNotSent()
      }

        def testSummaryRows(testData: Journey[JourneySpecificData], fakeRequest: FakeRequest[_], expectedSummaryListRows: List[(String, String)]) = {
          PayApiStub.stubForFindBySessionId2xx(testData)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val summaryListRows: List[Element] = document.select(".govuk-summary-list__row").asScala.toList
          val keyValuePairsOfSummaryRows: List[(String, String)] =
            summaryListRows.map(row => row.select(".govuk-summary-list__key").text() -> row.select(".govuk-summary-list__value").text())

          keyValuePairsOfSummaryRows should contain theSameElementsInOrderAs expectedSummaryListRows
        }

      "should have a test for all origins below this one" in {
        TestHelpers.implementedOrigins.size shouldBe 26 withClue "** This dummy test is here to remind you to update the tests below. Bump up the expected number when an origin is added to implemented origins **"
      }

      TestHelpers.implementedOrigins.foreach { origin =>

        val testScenario: TestScenarioInfo = originToTdAndSummaryListRows(origin)

        s"for origin ${origin.entryName}" - {
          //generic table content tests all origins should have.
          "when paying by debit card" - {

            "render the summary list correctly" in {
              testSummaryRows(testScenario.debitCardJourney, fakeGetRequest, testScenario.englishSummaryRowsDebitCard)
            }

            if (testScenario.hasWelshTest) {
              "render the summary list correctly in welsh" in {
                testSummaryRows(testScenario.debitCardJourney, fakeGetRequestInWelsh, testScenario.maybeWelshSummaryRowsDebitCard.getOrElse(throw new RuntimeException("test failed, missing welsh when it's expected")))
              }
            }
          }

          "when paying by a card that incurs a surcharge" - {

            "render the summary list correctly when payment has a surcharge" in {
              testSummaryRows(testScenario.creditCardJourney, fakeGetRequest, testScenario.englishSummaryRowsCreditCard)
            }

            if (testScenario.hasWelshTest) {
              "render the summary list correctly in welsh when payment has a surcharge" in {
                testSummaryRows(testScenario.creditCardJourney, fakeGetRequestInWelsh, testScenario.maybeWelshSummaryRowsCreditCard.getOrElse(throw new RuntimeException("test failed, missing welsh when it's expected")))
              }
            }
          }

          //i.e. a logged in journey, so it has a returnUrl which is to bta or similar
          if (testScenario.hasAReturnUrl) {

            "render the custom what happens next content" in {
              PayApiStub.stubForFindBySessionId2xx(testScenario.debitCardJourney)
              val result = systemUnderTest.renderPage(fakeGetRequest)
              val document = Jsoup.parse(contentAsString(result))
              val wrapper = document.select("#what-happens-next-wrapper")
              wrapper.select("h4").text() shouldBe "What happens next"
              wrapper.select("p").html() shouldBe "Your payment will take 3 to 5 days to show in your <a class=\"govuk-link\" href=\"https://www.return-url.com\">HMRC online account.</a>"
            }

            "render the custom what happens next content in welsh" in {
              PayApiStub.stubForFindBySessionId2xx(testScenario.debitCardJourney)
              val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
              val document = Jsoup.parse(contentAsString(result))
              val wrapper = document.select("#what-happens-next-wrapper")
              wrapper.select("h4").text() shouldBe "Yr hyn sy’n digwydd nesaf"
              wrapper.select("p").html() shouldBe "Bydd eich taliad yn cymryd 3 i 5 diwrnod i ymddangos yn eich <a class=\"govuk-link\" href=\"https://www.return-url.com\">cyfrif CThEM ar-lein.</a>"
            }

          }
        }
      }
    }

    "buildAmountsSummaryListRow" - {
        def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
      "should return just the amount row when card type is debit" in {
        implicit val messages: Messages = messagesApi.preferred(fakeGetRequest)
        val summaryListRow: Seq[SummaryListRow] = PaymentCompleteController.buildAmountsSummaryListRow(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        summaryListRow shouldBe Seq(SummaryListRow(Key(Text("Amount"), ""), Value(Text("£12.34"), ""), "", None))
      }

      "should return just the amount row in welsh when card type is debit" in {
        implicit val messages: Messages = messagesApi.preferred(fakeGetRequestInWelsh)
        val summaryListRow: Seq[SummaryListRow] = PaymentCompleteController.buildAmountsSummaryListRow(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        summaryListRow shouldBe Seq(SummaryListRow(Key(Text("Swm"), ""), Value(Text("£12.34"), ""), "", None))
      }

      "should return just the amount row with the amount in GDS format (i.e. don't show £x.00 when pennies is 0)" in {
        implicit val messages: Messages = messagesApi.preferred(fakeGetRequest)
        val journey = TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment.copy(amountInPence = Some(AmountInPence(123000)))
        val summaryListRow: Seq[SummaryListRow] =
          PaymentCompleteController.buildAmountsSummaryListRow(journey)
        summaryListRow shouldBe Seq(SummaryListRow(Key(Text("Amount"), ""), Value(Text("£1,230"), ""), "", None))
      }

      "should return amount paid, card fee and total paid rows when card type is credit" in {
        implicit val messages: Messages = messagesApi.preferred(fakeGetRequest)
        val summaryListRows: Seq[SummaryListRow] = PaymentCompleteController.buildAmountsSummaryListRow(TestJourneys.PfSa.journeyAfterSucceedCreditWebPayment)
        val expectedSummaryListRows = Seq(
          SummaryListRow(Key(Text("Amount paid to HMRC"), ""), Value(Text("£12.34"), ""), "", None),
          SummaryListRow(Key(HtmlContent("<nobr>Card fee (9.97%),</nobr><br/><nobr>non-refundable</nobr>"), ""), Value(Text("£1.23"), ""), "", None),
          SummaryListRow(Key(Text("Total paid"), ""), Value(Text("£13.57"), ""), "", None)
        )
        summaryListRows shouldBe expectedSummaryListRows
      }

      "should return amount paid, card fee and total paid rows in welsh when card type is credit" in {
        implicit val messages: Messages = messagesApi.preferred(fakeGetRequestInWelsh)
        val summaryListRows: Seq[SummaryListRow] = PaymentCompleteController.buildAmountsSummaryListRow(TestJourneys.PfSa.journeyAfterSucceedCreditWebPayment)
        val expectedSummaryListRows = Seq(
          SummaryListRow(Key(Text("Swm a dalwyd i CThEM"), ""), Value(Text("£12.34"), ""), "", None),
          SummaryListRow(Key(HtmlContent("<nobr>Ffi cerdyn (9.97%),</nobr><br/><nobr>ni ellir ei ad-dalu</nobr>"), ""), Value(Text("£1.23"), ""), "", None),
          SummaryListRow(Key(Text("Cyfanswm a dalwyd"), ""), Value(Text("£13.57"), ""), "", None)
        )
        summaryListRows shouldBe expectedSummaryListRows
      }

      "should return the relevant amounts for credit card payments in GDS format (i.e. don't show £x.00 when pennies is 0)" in {
        implicit val messages: Messages = messagesApi.preferred(fakeGetRequest)
        val orderWithPoundsZeroPennies = Some(BarclaysOrder(
          transactionReference = TransactionReference("Some-transaction-ref"),
          iFrameUrl            = Url("some-url"),
          cardCategory         = Some(CardCategories.credit),
          commissionInPence    = Some(AmountInPence(12300)),
          paidOn               = Some(LocalDateTime.parse("2027-11-02T16:28:55.185"))
        ))
        val journey = TestJourneys.PfSa.journeyAfterSucceedCreditWebPayment.copy(order         = orderWithPoundsZeroPennies, amountInPence = Some(AmountInPence(1234000)))
        val summaryListRows: Seq[SummaryListRow] = PaymentCompleteController.buildAmountsSummaryListRow(journey)
        val expectedSummaryListRows = Seq(
          SummaryListRow(Key(Text("Amount paid to HMRC"), ""), Value(Text("£12,340"), ""), "", None),
          SummaryListRow(Key(HtmlContent("<nobr>Card fee (1.00%),</nobr><br/><nobr>non-refundable</nobr>"), ""), Value(Text("£123"), ""), "", None),
          SummaryListRow(Key(Text("Total paid"), ""), Value(Text("£12,463"), ""), "", None)
        )
        summaryListRows shouldBe expectedSummaryListRows
      }
    }
  }

}

object PaymentCompleteControllerSpec {

  final case class TestScenarioInfo(
      debitCardJourney:                Journey[JourneySpecificData],
      creditCardJourney:               Journey[JourneySpecificData],
      englishSummaryRowsDebitCard:     List[(String, String)],
      maybeWelshSummaryRowsDebitCard:  Option[List[(String, String)]],
      englishSummaryRowsCreditCard:    List[(String, String)],
      maybeWelshSummaryRowsCreditCard: Option[List[(String, String)]],
      hasWelshTest:                    Boolean,
      hasAReturnUrl:                   Boolean
  )

  //helper function, so you can just add td to match case and testing is done for you :)
  def originToTdAndSummaryListRows: Origin => TestScenarioInfo = {

    case Origins.PfSa => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfSa.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Self Assessment",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Hunanasesiad",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Self Assessment",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Hunanasesiad",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.BtaSa => TestScenarioInfo(
      debitCardJourney                = TestJourneys.BtaSa.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.BtaSa.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Self Assessment",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Hunanasesiad",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Self Assessment",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Hunanasesiad",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.PtaSa => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PtaSa.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PtaSa.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Self Assessment",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Hunanasesiad",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Self Assessment",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Hunanasesiad",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.ItSa => TestScenarioInfo(
      debitCardJourney                = TestJourneys.ItSa.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.ItSa.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Self Assessment",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Hunanasesiad",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Self Assessment",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Hunanasesiad",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.AlcoholDuty => TestScenarioInfo(
      debitCardJourney                = TestJourneys.AlcoholDuty.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.AlcoholDuty.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Alcohol Duty",
        "Date" -> "2 November 2027",
        "Charge reference" -> "XE1234567890123",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Toll Alcohol",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Cyfeirnod y tâl" -> "XE1234567890123",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Alcohol Duty",
        "Date" -> "2 November 2027",
        "Charge reference" -> "XE1234567890123",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Toll Alcohol",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Cyfeirnod y tâl" -> "XE1234567890123",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.PfAlcoholDuty => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfAlcoholDuty.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfAlcoholDuty.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Alcohol Duty",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Toll Alcohol",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Alcohol Duty",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Toll Alcohol",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.BtaCt => TestScenarioInfo(
      debitCardJourney                = TestJourneys.BtaCt.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.BtaCt.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Corporation Tax",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Treth Gorfforaeth",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Corporation Tax",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Treth Gorfforaeth",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.PfCt => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfCt.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfCt.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Corporation Tax",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Treth Gorfforaeth",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Corporation Tax",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Treth Gorfforaeth",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.Ppt => TestScenarioInfo(
      debitCardJourney                = TestJourneys.Ppt.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.Ppt.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Plastic Packaging Tax",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Dreth Deunydd Pacio Plastig",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Plastic Packaging Tax",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Dreth Deunydd Pacio Plastig",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.PfPpt => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfPpt.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfPpt.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Plastic Packaging Tax",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Dreth Deunydd Pacio Plastig",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Plastic Packaging Tax",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Dreth Deunydd Pacio Plastig",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.PfEpayeNi => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfEpayeNi.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfEpayeNi.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Employers PAYE and National Insurance",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "TWE ac Yswiriant Gwladol y Cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Employers PAYE and National Insurance",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "TWE ac Yswiriant Gwladol y Cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.PfEpayeLpp => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfEpayeLpp.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfEpayeLpp.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Employers PAYE late payment penalty",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Cosb y Cyflogwr am dalu TWE yn hwyr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Employers PAYE late payment penalty",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Cosb y Cyflogwr am dalu TWE yn hwyr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.PfEpayeLateCis => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfEpayeLateCis.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfEpayeLateCis.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Construction Industry Scheme (CIS) late filing penalty",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Cynllun y Diwydiant Adeiladu (CIS) - cosb am dalun hwyr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Construction Industry Scheme (CIS) late filing penalty",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Cynllun y Diwydiant Adeiladu (CIS) - cosb am dalun hwyr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.PfEpayeP11d => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfEpayeP11d.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfEpayeP11d.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Employers’ Class 1A National Insurance",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Yswiriant Gwladol Dosbarth 1A y Cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Employers’ Class 1A National Insurance",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Yswiriant Gwladol Dosbarth 1A y Cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.PfEpayeSeta => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfEpayeSeta.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfEpayeSeta.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Employers’ PAYE Settlement Agreement",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Cytundeb Setliad TWE y Cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Employers’ PAYE Settlement Agreement",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Cytundeb Setliad TWE y Cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.PfVat => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfVat.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfVat.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "VAT",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "TAW",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "VAT",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "TAW",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.BtaVat => TestScenarioInfo(
      debitCardJourney                = TestJourneys.BtaVat.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.BtaVat.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "VAT",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "TAW",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "VAT",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "TAW",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.VcVatReturn => TestScenarioInfo(
      debitCardJourney                = TestJourneys.VcVatReturn.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.VcVatReturn.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "VAT",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "TAW",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "VAT",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "TAW",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.VcVatOther => TestScenarioInfo(
      debitCardJourney                = TestJourneys.VcVatOther.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.VcVatOther.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "VAT",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "TAW",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "VAT",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "TAW",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.BtaEpayeBill => TestScenarioInfo(
      debitCardJourney                = TestJourneys.BtaEpayeBill.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.BtaEpayeBill.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "PAYE and National Insurance",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "TWE a’ch Yswiriant Gwladol y cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "PAYE and National Insurance",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "TWE a’ch Yswiriant Gwladol y cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.BtaEpayePenalty => TestScenarioInfo(
      debitCardJourney                = TestJourneys.BtaEpayePenalty.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.BtaEpayePenalty.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "PAYE and National Insurance",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "TWE a’ch Yswiriant Gwladol y cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "PAYE and National Insurance",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "TWE a’ch Yswiriant Gwladol y cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.BtaEpayeInterest => TestScenarioInfo(
      debitCardJourney                = TestJourneys.BtaEpayeInterest.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.BtaEpayeInterest.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "PAYE and National Insurance",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "TWE a’ch Yswiriant Gwladol y cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "PAYE and National Insurance",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "TWE a’ch Yswiriant Gwladol y cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.BtaEpayeGeneral => TestScenarioInfo(
      debitCardJourney                = TestJourneys.BtaEpayeGeneral.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.BtaEpayeGeneral.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "PAYE and National Insurance",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "TWE a’ch Yswiriant Gwladol y cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "PAYE and National Insurance",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "TWE a’ch Yswiriant Gwladol y cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.BtaClass1aNi => TestScenarioInfo(
      debitCardJourney                = TestJourneys.BtaClass1aNi.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.BtaClass1aNi.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Employers’ Class 1A National Insurance",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Yswiriant Gwladol Dosbarth 1A y Cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Employers’ Class 1A National Insurance",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Yswiriant Gwladol Dosbarth 1A y Cyflogwr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.Amls => TestScenarioInfo(
      debitCardJourney                = TestJourneys.Amls.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.Amls.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Money Laundering Regulations fees",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Ffioedd Rheoliadau Gwyngalchu Arian",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Money Laundering Regulations fees",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Ffioedd Rheoliadau Gwyngalchu Arian",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.PfAmls => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfAmls.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfAmls.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Money Laundering Regulations fees",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Ffioedd Rheoliadau Gwyngalchu Arian",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Money Laundering Regulations fees",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Ffioedd Rheoliadau Gwyngalchu Arian",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case o: Origin => throw new MatchError(s"Add testdata for now origin you've added [${o.entryName}] to implemented origins.")
  }

}
