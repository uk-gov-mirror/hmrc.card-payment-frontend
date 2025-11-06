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
import org.scalatest.Assertion
import payapi.cardpaymentjourney.model.barclays.BarclaysOrder
import payapi.cardpaymentjourney.model.journey.{Journey, JourneySpecificData, JsdBcPngr, JsdMib, JsdPfP800, JsdPtaP800, Url}
import payapi.corcommon.model.Origins._
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
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.{TestDataUtils, TestJourneys}
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
        langToggleText should contain theSameElementsAs List("English", "Newid yr iaith i’r Gymraeg Cymraeg") //checking the visually hidden text, it's simpler
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
        surveyWrapper.select("#survey-link-wrapper").html() shouldBe """<a class="govuk-link" href="/pay-by-card/start-payment-survey">Tell us what you think of this service</a> (takes 30 seconds)"""
      }

      "render the survey content correctly in welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val surveyWrapper = document.select("#survey-wrapper")
        surveyWrapper.hasClass("govuk-!-display-none-print") shouldBe true
        surveyWrapper.select("h2").text() shouldBe "Helpwch ni i wella ein gwasanaethau"
        surveyWrapper.select("#survey-content").text() shouldBe "Rydym yn defnyddio’ch adborth i wella ein gwasanaethau."
        surveyWrapper.select("#survey-link-wrapper").html() shouldBe """<a class="govuk-link" href="/pay-by-card/start-payment-survey">Rhowch wybod i ni beth yw eich barn am y gwasanaeth hwn</a> (mae’n cymryd 30 eiliad)"""
      }

      "should not send an email if there is not one in the session" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        status(result) shouldBe Status.OK
        EmailStub.verifyEmailWasNotSent()
      }

      "should render the webchat specific content for webchat origins" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("#web-chat-content").text() shouldBe "If you need further help with a tax bill, return to the webchat and speak with the webchat handler."
      }

      "should render the webchat specific content in welsh for webchat origins" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        document.select("#web-chat-content").text() shouldBe "Os oes angen cymorth pellach arnoch gyda’ch bil treth, bydd angen dychwelyd i’r sgwrs dros y we a siarad ag ymgynghorydd."
      }

      "should not render the webchat specific content for origins that are not webchat related" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select("#web-chat-content").size() shouldBe 0
        contentAsString(result) shouldNot contain("If you need further help with a tax bill, return to the webchat and speak with the webchat handler.")
      }

      "render the custom what happens next content for VatC2c Journeys" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.VatC2c.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val wrapper = document.select("#what-happens-next-wrapper")
        wrapper.select("h2").text() shouldBe "What happens next"
        wrapper.select("p").html() shouldBe "Your payment can take up to 5 days to show in your online tax account."
      }

      "render the custom what happens next content in welsh for VatC2c Journeys" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.VatC2c.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val wrapper = document.select("#what-happens-next-wrapper")
        wrapper.select("h2").text() shouldBe "Yr hyn sy’n digwydd nesaf"
        wrapper.select("p").html() shouldBe "Gall eich taliad gymryd hyd at 5 diwrnod ymddangos yn eich cyfrif treth ar-lein."
      }

      "render the custom what happens next content for signed out Journeys" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val wrapper = document.select("#what-happens-next-wrapper")
        wrapper.select("h2").text() shouldBe "What happens next"
        wrapper.select("p").html() shouldBe "If you have an online tax account, your payment can take up to 5 days to show."
      }

      "render the custom what happens next content in Welsh for signed out Journeys" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val wrapper = document.select("#what-happens-next-wrapper")
        wrapper.select("h2").text() shouldBe "Yr hyn sy’n digwydd nesaf"
        wrapper.select("p").html() shouldBe "Os oes gennych gyfrif treth ar-lein, gall eich taliad gymryd hyd at 5 diwrnod i ymddangos."
      }

      "render the custom what happens next content for PfCds" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCds.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val wrapper = document.select("#what-happens-next-wrapper")
        wrapper.select("h2").text() shouldBe "What happens next"
        wrapper.select("p").html() shouldBe "Your payment can take up to 5 days to show in your online tax account."
      }

      "render custom what happens next content for PfMgd" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfMgd.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val wrapper = document.select("#what-happens-next-wrapper")
        wrapper.select("h2").asList().get(0).text() shouldBe "What happens next"
        wrapper.select("p").asList().get(0).text() shouldBe "If you have an online tax account, your payment can take up to 5 days to show."
        wrapper.select("h2").asList().get(1).text() shouldBe "You can pay your future Machine Games Duty (MGD) bills by Direct Debit"
        wrapper.select("p").asList().get(1).text() shouldBe "You can set up a variable Direct Debit to make all your future Machine Games Duty payments automatically. Having a Direct Debit means you will not miss deadlines or get late payment charges."
        wrapper.select("a").attr("href").contains("/gg/sign-in?continue=/business-account/&origin=MGD-frontend") shouldBe true
        wrapper.select("p").asList().get(2).text() shouldBe "Set up your Direct Debit at least 5 days before your next payment is due to make sure you meet the payment deadline."
      }

      "render custom what happens next content for PfMgd in welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfMgd.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val wrapper = document.select("#what-happens-next-wrapper")
        wrapper.select("h2").asList().get(0).text() shouldBe "Yr hyn sy’n digwydd nesaf"
        wrapper.select("p").asList().get(0).text() shouldBe "Os oes gennych gyfrif treth ar-lein, gall eich taliad gymryd hyd at 5 diwrnod i ymddangos."
        wrapper.select("h2").asList().get(1).text() shouldBe "Gallwch dalu’ch biliau Toll Peiriannau Hapchwarae (MGD) yn y dyfodol drwy Ddebyd Uniongyrchol"
        wrapper.select("p").asList().get(1).text() shouldBe "Gallwch sefydlu Debyd Uniongyrchol newidiol i wneud eich holl daliadau Toll Peiriannau Hapchwarae yn awtomatig yn y dyfodol. Mae cael Debyd Uniongyrchol yn golygu na fyddwch yn methu dyddiadau cau nac yn wynebu costau am dalu’n hwyr."
        wrapper.select("p").asList().get(2).text() shouldBe "Sefydlwch eich Debyd Uniongyrchol o leiaf 5 diwrnod cyn dyddiad dyledus eich taliad nesaf er mwyn gwneud yn siŵr eich bod yn talu erbyn y dyddiad cau."
      }

      "should render the x reference/charge reference for PfVat when that's the appropriate reference" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatWithChargeReference.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val panel = document.body().select(".govuk-panel--confirmation")
        panel.select("h1").text() shouldBe "Payment received by HMRC"
        panel.select(".govuk-panel__body").html() shouldBe "Your payment reference\n<br>\n<strong>XE123456789012</strong>"
      }

      "should render the x reference/charge reference for WcVat when that's the appropriate reference" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatWithChargeReference.journeyAfterSucceedDebitWebPayment)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val panel = document.body().select(".govuk-panel--confirmation")
        panel.select("h1").text() shouldBe "Payment received by HMRC"
        panel.select(".govuk-panel__body").html() shouldBe "Your payment reference\n<br>\n<strong>XE123456789012</strong>"
      }

      "should render the extra row for p800ChargeReference when it's in the journey" - {
        "for PfP800" in {
          val testJourney: Journey[JsdPfP800] = TestDataUtils.intoSuccessWithOrder(TestJourneys.PfP800.journeyWithP800ChargeRefBeforeBeginWebPayment, TestDataUtils.debitCardOrder)
          PayApiStub.stubForFindBySessionId2xx(testJourney)
          val result = systemUnderTest.renderPage(fakeGetRequest)
          val document = Jsoup.parse(contentAsString(result))
          val panel = document.body().select(".govuk-panel--confirmation")
          panel.select("h1").text() shouldBe "Payment received by HMRC"
          panel.select(".govuk-panel__body").html() shouldBe "Your payment reference\n<br>\n<strong>MA000003AP8002027</strong>"
          testSummaryRows(
            testData                = testJourney,
            fakeRequest             = fakeGetRequest,
            expectedSummaryListRows = List(
              "Tax" -> "P800",
              "Date" -> "2 November 2027",
              "Charge reference" -> "BC007010065114",
              "Reference number" -> "MA000003AP8002027",
              "Amount" -> "£12.34"
            )
          )
        }
        "for PtaP800" in {
          val testJourney: Journey[JsdPtaP800] = TestDataUtils.intoSuccessWithOrder(TestJourneys.PtaP800.journeyWithP800ChargeRefBeforeBeginWebPayment, TestDataUtils.debitCardOrder)
          PayApiStub.stubForFindBySessionId2xx(testJourney)
          val result = systemUnderTest.renderPage(fakeGetRequest)
          val document = Jsoup.parse(contentAsString(result))
          val panel = document.body().select(".govuk-panel--confirmation")
          panel.select("h1").text() shouldBe "Payment received by HMRC"
          panel.select(".govuk-panel__body").html() shouldBe "Your payment reference\n<br>\n<strong>MA000003AP8002027</strong>"
          testSummaryRows(
            testData                = testJourney,
            fakeRequest             = fakeGetRequest,
            expectedSummaryListRows = List(
              "Tax" -> "P800",
              "Date" -> "2 November 2027",
              "Charge reference" -> "BC007010065114",
              "Reference number" -> "MA000003AP8002027",
              "Amount" -> "£12.34"
            )
          )
        }
      }

      "should render the custom payment complete page for Mib" in {
        val testJourney: Journey[JsdMib] = TestJourneys.Mib.journeyAfterSucceedCreditWebPayment
        PayApiStub.stubForFindBySessionId2xx(testJourney)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val panel = document.body().select(".govuk-panel--confirmation")
        panel.select("h1").text() shouldBe "Declaration complete"
        panel.select(".govuk-panel__body").html() shouldBe "Your reference number\n<br>\n<strong>MIBI1234567891</strong>"
        document.select("#email-paragraph").text() shouldBe "We have sent a confirmation email to the address provided."
        document.select("#print-link-wrapper").select("a").text() shouldBe "Print or save a copy of this page"
        val modsSpecificContent = document.select("#mods-specific-content")
        val whatHappensNext = modsSpecificContent.select("#what-happens-next-wrapper")
        whatHappensNext.select("h2").text() shouldBe "What you need to do next"
        whatHappensNext.select("p").text() shouldBe "Make sure that you:"
        val unorderedList = whatHappensNext.select("ul").select("li").asScala.toList
        unorderedList(0).html() shouldBe "go through the <strong>green channel</strong> (nothing to declare) at customs"
        unorderedList(1).text() shouldBe "take the declaration sent to the email provided"
        unorderedList(2).text() shouldBe "take the receipts or invoices for all the declared goods"
        val bringGoodsContent = modsSpecificContent.select("#bringing-eu-goods-wrapper")
        bringGoodsContent.select("h2").text() shouldBe "Bringing EU goods"
        bringGoodsContent.select("p").text() shouldBe "If you bring EU-produced goods that have a total value over £1,000, you need to carry proof they were made in the EU."
        val links = modsSpecificContent.select("#mods-links").select("p").asScala.toList
        links(0).html() shouldBe "<a class=\"govuk-link\" href=\"http://localhost:8281/declare-commercial-goods/make-another-declaration\">Make a new declaration</a>"
        links(1).html() shouldBe "<a class=\"govuk-link\" href=\"http://localhost:8281/declare-commercial-goods/add-goods-to-an-existing-declaration\">Add goods to an existing declaration</a>"
        document.select("#survey-wrapper").select("h2").text() shouldBe "Help us improve our services"
        document.select("#survey-wrapper").select("#survey-content").text() shouldBe "We use your feedback to make our services better."
        document.select("#survey-wrapper").select("#survey-link-wrapper").html() shouldBe "<a class=\"govuk-link\" href=\"http://localhost:8281/declare-commercial-goods/survey\">Tell us what you think of this service</a> (takes 30 seconds)"
      }

      "should render the custom payment complete page in welsh for Mib" in {
        val testJourney: Journey[JsdMib] = TestJourneys.Mib.journeyAfterSucceedCreditWebPayment
        PayApiStub.stubForFindBySessionId2xx(testJourney)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val panel = document.body().select(".govuk-panel--confirmation")
        panel.select("h1").text() shouldBe "Datganiad wedi’i gwblhau"
        panel.select(".govuk-panel__body").html() shouldBe "Eich cyfeirnod\n<br>\n<strong>MIBI1234567891</strong>"
        document.select("#email-paragraph").text() shouldBe "Rydym wedi anfon e-bost cadarnhau i’r cyfeiriad a roddwyd."
        document.select("#print-link-wrapper").select("a").text() shouldBe "Argraffu neu gadw copi o’r dudalen hon"
        val modsSpecificContent = document.select("#mods-specific-content")
        val whatHappensNext = modsSpecificContent.select("#what-happens-next-wrapper")
        whatHappensNext.select("h2").text() shouldBe "Yr hyn y mae angen i chi ei wneud nesaf"
        whatHappensNext.select("p").text() shouldBe "Gwnewch yn siŵr eich bod yn:"
        val unorderedList = whatHappensNext.select("ul").select("li").asScala.toList
        unorderedList(0).html() shouldBe "mynd drwy’r <strong>sianel wyrdd</strong> (dim byd i’w ddatgan) wrth y tollau"
        unorderedList(1).text() shouldBe "mynd â’r datganiad a anfonwyd at y cyfeiriad a roddwyd"
        unorderedList(2).text() shouldBe "mynd â’r derbynebau neu’r anfonebau ar gyfer yr holl nwyddau a ddatganwyd"
        val bringGoodsContent = modsSpecificContent.select("#bringing-eu-goods-wrapper")
        bringGoodsContent.select("h2").text() shouldBe "Dod â nwyddau o’r UE gyda chi"
        bringGoodsContent.select("p").text() shouldBe "Os ydych yn dod â nwyddau a gynhyrchwyd yn yr UE gyda chi y mae cyfanswm eu gwerth dros £1,000, mae’n rhaid i chi gario tystiolaeth y cawsant eu gwneud yn yr UE."
        val links = modsSpecificContent.select("#mods-links").select("p").asScala.toList
        links(0).html() shouldBe "<a class=\"govuk-link\" href=\"http://localhost:8281/declare-commercial-goods/make-another-declaration\">Gwneud datganiad newydd</a>"
        links(1).html() shouldBe "<a class=\"govuk-link\" href=\"http://localhost:8281/declare-commercial-goods/add-goods-to-an-existing-declaration\">Ychwanegu nwyddau i ddatganiad sy’n bodoli eisoes</a>"
        document.select("#survey-wrapper").select("h2").text() shouldBe "Helpwch ni i wella ein gwasanaethau"
        document.select("#survey-wrapper").select("#survey-content").text() shouldBe "Rydym yn defnyddio’ch adborth i wella ein gwasanaethau."
        document.select("#survey-wrapper").select("#survey-link-wrapper").html() shouldBe "<a class=\"govuk-link\" href=\"http://localhost:8281/declare-commercial-goods/survey\">Rhowch wybod i ni beth yw eich barn am y gwasanaeth hwn</a> (mae’n cymryd 30 eiliad)"
      }

      "should render the custom payment complete page for BcPngr" in {
        val testJourney: Journey[JsdBcPngr] = TestJourneys.BcPngr.journeyAfterSucceedCreditWebPayment
        PayApiStub.stubForFindBySessionId2xx(testJourney)
        val result = systemUnderTest.renderPage(fakeGetRequest)
        val document = Jsoup.parse(contentAsString(result))
        val panel = document.body().select(".govuk-panel--confirmation")
        panel.select("h1").text() shouldBe "Payment complete"
        panel.select(".govuk-panel__body").html() shouldBe "Your reference number\n<br>\n<strong>XAPR9876543210</strong>"
        document.select("#leading-content-wrapper").select("#p1").text() shouldBe "Make a note of your reference number, you may need to provide it to Border Force."
        document.select("#leading-content-wrapper").select("#p2").text() shouldBe "If you provided an email address, a copy of this receipt has been sent to you."

        val leadingInfoTableRows: List[Element] = document.select("#leading-info-table").select("tr").asScala.toList
        testTableRows(leadingInfoTableRows, List(
          "Name", "Bob Ross",
          "Date of payment", "25 November 2059",
          "Place of arrival in uk", "Heathrow",
          "Date of arrival", "2 November 2027",
          "Time of arrival", "04:28 PM",
          "Reference number", "XAPR9876543210",
          "Amount paid to HMRC", "£12.34",
          "Card fee (9.97%), non-refundable", "£1.23",
          "Total paid", "£13.57"
        )) withClue "leadingInfoTableRows"

        val itemsDeclaredTableRows = document.select("#items-declared-wrapper").select("tr").asScala.toList
        testTableRows(itemsDeclaredTableRows, List(
          "Item", "Price", "Purchased in", "Tax paid",
          "Booze", "124", "Vire Normandie", "£113",
          "Even more booze", "125", "Vire Normandie", "£116",
          "Total", "", "", "£229.00",
          "Amount paid previously", "", "£55",
          "Total paid now", "", "£1,234"
        )) withClue "itemsDeclaredTableRows"

        val paymentBreakdownTableRows = document.select("#payment-breakdown-wrapper").select("tr").asScala.toList
        testTableRows(paymentBreakdownTableRows, List(
          "Type of tax or duty", "Amount paid",
          "Customs", "£123.00",
          "Excise", "£66.00",
          "VAT", "£5.00",
          "Total", "£194.00"
        )) withClue "paymentBreakdownTableRows"

        document.select("#arriving-in-uk-wrapper").select("h2").text() shouldBe "What to do when you arrive in the UK"
        document.select("#arriving-in-uk-wrapper").select("#arriving-in-uk-p1").text() shouldBe "Go to the green ‘nothing to declare’ channel if these are the only items you are declaring. If asked, show your receipt on your mobile phone or tablet to a member of Border Force, or provide your reference number."

        document.select("#amending-declaration-wrapper").select("h2").text() shouldBe "Amending your declaration"
        document.select("#amending-declaration-wrapper").select("#amending-declaration-p1").text() shouldBe "You can use this service to add goods to your existing declaration before you arrive in the UK. You will need to enter your reference number."
        document.select("#amending-declaration-wrapper").select("#amending-declaration-p2").html() shouldBe "If you need to remove goods from your declaration <a class=\"govuk-link\" href=\"https://www.gov.uk/government/publications/request-a-refund-of-overpaid-vat-or-duty-for-goods-declared-using-the-online-service-for-passengers\">visit GOV.UK to request a refund</a>."

        document.select("#survey-wrapper").select("h2").text() shouldBe "Help us improve our services"
        document.select("#survey-wrapper").select("#survey-content").text() shouldBe "We use your feedback to make our services better."
        document.select("#survey-wrapper").select("#survey-link-wrapper").html() shouldBe "<a class=\"govuk-link\" href=\"https://www.tax.service.gov.uk/feedback/passengers\">Tell us what you think of this service</a> (takes 30 seconds)"
      }

      "should render the custom payment complete page in welsh for BcPngr" in {
        val testJourney: Journey[JsdBcPngr] = TestJourneys.BcPngr.journeyAfterSucceedCreditWebPayment
        PayApiStub.stubForFindBySessionId2xx(testJourney)
        val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
        val document = Jsoup.parse(contentAsString(result))
        val panel = document.body().select(".govuk-panel--confirmation")
        println(document.toString)
        panel.select("h1").text() shouldBe "Taliad wedi’i gwblhau"
        panel.select(".govuk-panel__body").html() shouldBe "Cyfeirnod\n<br>\n<strong>XAPR9876543210</strong>"
        document.select("#leading-content-wrapper").select("#p1").text() shouldBe "Gwnewch nodyn o’ch cyfeirnod, mae’n bosibl y bydd angen i chi ei roi i Lu’r Ffiniau."
        document.select("#leading-content-wrapper").select("#p2").text() shouldBe "Os gwnaethoch roi cyfeiriad e-bost, mae copi o’r dderbynneb hon wedi’i hanfon atoch"

        val leadingInfoTableRows: List[Element] = document.select("#leading-info-table").select("tr").asScala.toList
        testTableRows(leadingInfoTableRows, List(
          "Enw", "Bob Ross",
          "Dyddiad y taliad", "25 Tachwedd 2059",
          "Man cyrraedd y DU", "Heathrow",
          "Dyddiad cyrraedd", "2 Tachwedd 2027",
          "Amser cyrraedd", "04:28 PM",
          "Cyfeirnod", "XAPR9876543210",
          "Swm a dalwyd i CThEM", "£12.34",
          "Ffi cerdyn (9.97%), ni ellir ei ad-dalu", "£1.23",
          "Cyfanswm a dalwyd", "£13.57"
        ))

        val itemsDeclaredTableRows = document.select("#items-declared-wrapper").select("tr").asScala.toList
        testTableRows(itemsDeclaredTableRows, List(
          "Eitem", "Pris", "Prynwyd yn", "Treth a dalwyd",
          "Booze", "124", "Vire Normandie", "£113",
          "Even more booze", "125", "Vire Normandie", "£116",
          "Cyfanswm", "", "", "£229.00",
          "Swm a dalwyd yn flaenorol", "", "£55",
          "Cyfanswm wedi’i dalu nawr", "", "£1,234"
        ))

        val paymentBreakdownTableRows = document.select("#payment-breakdown-wrapper").select("tr").asScala.toList
        testTableRows(paymentBreakdownTableRows, List(
          "Math o dreth neu doll", "Swm a dalwyd",
          "Tollau", "£123.00",
          "Ecséis", "£66.00",
          "TAW", "£5.00",
          "Cyfanswm", "£194.00"
        ))

        document.select("#arriving-in-uk-wrapper").select("h2").text() shouldBe "Yr hyn i’w wneud pan fyddwch yn cyrraedd y DU"
        document.select("#arriving-in-uk-wrapper").select("#arriving-in-uk-p1").text() shouldBe "Ewch i’r sianel werdd ar gyfer ’dim i’w ddatgan’ os mai dyma’r unig eitemau rydych chi’n eu datgan. Os gofynnir i chi, dangoswch eich derbynneb ar eich ffôn symudol neu lechen i aelod o Lu’r Ffiniau, neu rhowch eich cyfeirnod."

        document.select("#amending-declaration-wrapper").select("h2").text() shouldBe "Diwygio’ch datganiad"
        document.select("#amending-declaration-wrapper").select("#amending-declaration-p1").text() shouldBe "Gallwch ddefnyddio’r gwasanaeth hwn i ychwanegu nwyddau at eich datganiad presennol cyn i chi gyrraedd y DU. Bydd angen i chi nodi’ch cyfeirnod."
        document.select("#amending-declaration-wrapper").select("#amending-declaration-p2").html() shouldBe "Os oes angen i chi dynnu nwyddau o’ch datganiad, <a class=\"govuk-link\" href=\"https://www.gov.uk/government/publications/request-a-refund-of-overpaid-vat-or-duty-for-goods-declared-using-the-online-service-for-passengers\">ewch GOV.UK i ofyn am ad-daliad</a>."

        document.select("#survey-wrapper").select("h2").text() shouldBe "Helpwch ni i wella ein gwasanaethau"
        document.select("#survey-wrapper").select("#survey-content").text() shouldBe "Rydym yn defnyddio’ch adborth i wella ein gwasanaethau."
        document.select("#survey-wrapper").select("#survey-link-wrapper").html() shouldBe "<a class=\"govuk-link\" href=\"https://www.tax.service.gov.uk/feedback/passengers\">Rhowch wybod i ni beth yw eich barn am y gwasanaeth hwn</a> (mae’n cymryd 30 eiliad)"
      }

        def testTableRows(tableRows: List[Element], expectedTableData: List[String]): Assertion = {
          val tableData = tableRows.flatMap(_.select("td").asScala.toList.map(_.text()))
          tableData should contain theSameElementsInOrderAs expectedTableData
        }

        def testSummaryRows(testData: Journey[JourneySpecificData], fakeRequest: FakeRequest[_], expectedSummaryListRows: List[(String, String)]): Assertion = {
          PayApiStub.stubForFindBySessionId2xx(testData)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val summaryListRows: List[Element] = document.select(".govuk-summary-list__row").asScala.toList
          val keyValuePairsOfSummaryRows: List[(String, String)] =
            summaryListRows.map(row => row.select(".govuk-summary-list__key").text() -> row.select(".govuk-summary-list__value").text())

          keyValuePairsOfSummaryRows should contain theSameElementsInOrderAs expectedSummaryListRows
        }

      "should have a test for all origins below this one" in {
        TestHelpers.implementedOrigins.size shouldBe 67 withClue "** This dummy test is here to remind you to update the tests below. Bump up the expected number when an origin is added to implemented origins **"
      }

      TestHelpers.implementedOrigins
        .filterNot(_ == Origins.BcPngr) // BcPngr has it's own customised page, so we ignore it for these tests.
        .foreach { origin =>

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
                wrapper.select("h2").text() shouldBe "What happens next"
                wrapper.select("p").html() shouldBe "Your payment can take up to 5 days to show in your <a class=\"govuk-link\" href=\"https://www.return-url.com\">online tax account.</a>"
              }

              "render the custom what happens next content in welsh" in {
                PayApiStub.stubForFindBySessionId2xx(testScenario.debitCardJourney)
                val result = systemUnderTest.renderPage(fakeGetRequestInWelsh)
                val document = Jsoup.parse(contentAsString(result))
                val wrapper = document.select("#what-happens-next-wrapper")
                wrapper.select("h2").text() shouldBe "Yr hyn sy’n digwydd nesaf"
                wrapper.select("p").html() shouldBe "Gall eich taliad gymryd hyd at 5 diwrnod ymddangos yn eich <a class=\"govuk-link\" href=\"https://www.return-url.com\">cyfrif treth ar-lein.</a>"
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

    case Origins.WcSa => TestScenarioInfo(
      debitCardJourney                = TestJourneys.WcSa.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.WcSa.journeyAfterSucceedCreditWebPayment,
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

    case Origins.WcCt => TestScenarioInfo(
      debitCardJourney                = TestJourneys.WcCt.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.WcCt.journeyAfterSucceedCreditWebPayment,
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

    case Origins.WcVat => TestScenarioInfo(
      debitCardJourney                = TestJourneys.WcVat.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.WcVat.journeyAfterSucceedCreditWebPayment,
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

    case Origins.PfSdlt => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfSdlt.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfSdlt.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Stamp Duty Land Tax",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Treth Dir y Tollau Stamp",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Stamp Duty Land Tax",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Treth Dir y Tollau Stamp",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )
    case Origins.CapitalGainsTax => TestScenarioInfo(
      debitCardJourney                = TestJourneys.CapitalGainsTax.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.CapitalGainsTax.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Capital Gains Tax on UK property",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Treth Enillion Cyfalaf ar eiddo yn y DU",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Capital Gains Tax on UK property",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Treth Enillion Cyfalaf ar eiddo yn y DU",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.EconomicCrimeLevy => TestScenarioInfo(
      debitCardJourney                = TestJourneys.EconomicCrimeLevy.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.EconomicCrimeLevy.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Economic Crime Levy",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Ardoll Troseddau Economaidd",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Economic Crime Levy",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Ardoll Troseddau Economaidd",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case Origins.PfEconomicCrimeLevy => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfEconomicCrimeLevy.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfEconomicCrimeLevy.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Economic Crime Levy",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Ardoll Troseddau Economaidd",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Economic Crime Levy",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Ardoll Troseddau Economaidd",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.VatC2c => TestScenarioInfo(
      debitCardJourney                = TestJourneys.VatC2c.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.VatC2c.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Import VAT",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "TAW fewnforio",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Import VAT",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "TAW fewnforio",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.PfVatC2c => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfVatC2c.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfVatC2c.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Import VAT",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "TAW fewnforio",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Import VAT",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "TAW fewnforio",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.WcSimpleAssessment => TestScenarioInfo(
      debitCardJourney                = TestJourneys.WcSimpleAssessment.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.WcSimpleAssessment.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Simple Assessment",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Asesiad Syml",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Simple Assessment",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Asesiad Syml",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.WcClass1aNi => TestScenarioInfo(
      debitCardJourney                = TestJourneys.WcClass1aNi.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.WcClass1aNi.journeyAfterSucceedCreditWebPayment,
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

    case Origins.WcXref => TestScenarioInfo(
      debitCardJourney                = TestJourneys.WcXref.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.WcXref.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Other taxes, penalties and enquiry settlements",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Trethi, cosbau a setliadau ymholiadau eraill",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Other taxes, penalties and enquiry settlements",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Trethi, cosbau a setliadau ymholiadau eraill",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Origins.WcEpayeLpp => TestScenarioInfo(
      debitCardJourney                = TestJourneys.WcEpayeLpp.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.WcEpayeLpp.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Employers’ PAYE late payment penalty",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Cosb y Cyflogwr am dalu TWE yn hwyr",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Employers’ PAYE late payment penalty",
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

    case Origins.WcEpayeNi => TestScenarioInfo(
      debitCardJourney                = TestJourneys.WcEpayeNi.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.WcEpayeNi.journeyAfterSucceedCreditWebPayment,
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

    case Origins.WcEpayeLateCis => TestScenarioInfo(
      debitCardJourney                = TestJourneys.WcEpayeLateCis.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.WcEpayeLateCis.journeyAfterSucceedCreditWebPayment,
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

    case Origins.WcEpayeSeta => TestScenarioInfo(
      debitCardJourney                = TestJourneys.WcEpayeSeta.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.WcEpayeSeta.journeyAfterSucceedCreditWebPayment,
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

    case PfChildBenefitRepayments => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfChildBenefitRepayments.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfChildBenefitRepayments.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Repay Child Benefit overpayments",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Ad-dalu gordaliadau Budd-dal Plant",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Repay Child Benefit overpayments",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Ad-dalu gordaliadau Budd-dal Plant",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case WcChildBenefitRepayments => TestScenarioInfo(
      debitCardJourney                = TestJourneys.WcChildBenefitRepayments.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.WcChildBenefitRepayments.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Repay Child Benefit overpayments",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Ad-dalu gordaliadau Budd-dal Plant",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Repay Child Benefit overpayments",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Ad-dalu gordaliadau Budd-dal Plant",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case BtaSdil => TestScenarioInfo(
      debitCardJourney                = TestJourneys.BtaSdil.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.BtaSdil.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Soft Drinks Industry Levy",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Ardoll y Diwydiant Diodydd Ysgafn",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Soft Drinks Industry Levy",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Ardoll y Diwydiant Diodydd Ysgafn",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case PfSdil => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfSdil.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfSdil.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Soft Drinks Industry Levy",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Ardoll y Diwydiant Diodydd Ysgafn",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Soft Drinks Industry Levy",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Ardoll y Diwydiant Diodydd Ysgafn",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case PtaP800 => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PtaP800.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PtaP800.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "P800",
        "Date" -> "2 November 2027",
        "Reference number" -> "MA000003AP8002027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "P800",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Cyfeirnod" -> "MA000003AP8002027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "P800",
        "Date" -> "2 November 2027",
        "Reference number" -> "MA000003AP8002027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "P800",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Cyfeirnod" -> "MA000003AP8002027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case PfP800 => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfP800.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfP800.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "P800",
        "Date" -> "2 November 2027",
        "Reference number" -> "MA000003AP8002027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "P800",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Cyfeirnod" -> "MA000003AP8002027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "P800",
        "Date" -> "2 November 2027",
        "Reference number" -> "MA000003AP8002027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "P800",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Cyfeirnod" -> "MA000003AP8002027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case PtaSimpleAssessment => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PtaSimpleAssessment.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PtaSimpleAssessment.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Simple Assessment",
        "Date" -> "2 November 2027",
        "Charge reference" -> "BC007010065114",
        "Tax year" -> "6 April 2027 to 5 April 2028",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Asesiad Syml",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Cyfeirnod y tâl" -> "BC007010065114",
        "Blwyddyn dreth" -> "6 Ebrill 2027 i 5 Ebrill 2028",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Simple Assessment",
        "Date" -> "2 November 2027",
        "Charge reference" -> "BC007010065114",
        "Tax year" -> "6 April 2027 to 5 April 2028",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Asesiad Syml",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Cyfeirnod y tâl" -> "BC007010065114",
        "Blwyddyn dreth" -> "6 Ebrill 2027 i 5 Ebrill 2028",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = true
    )

    case PfSimpleAssessment => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfSimpleAssessment.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfSimpleAssessment.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Simple Assessment",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Asesiad Syml",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Simple Assessment",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Asesiad Syml",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case PfJobRetentionScheme => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfJobRetentionScheme.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfJobRetentionScheme.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Pay Coronavirus Job Retention Scheme grants back",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Talu grantiau’r Cynllun Cadw Swyddi yn sgil Coronafeirws yn ôl",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Pay Coronavirus Job Retention Scheme grants back",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Talu grantiau’r Cynllun Cadw Swyddi yn sgil Coronafeirws yn ôl",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case JrsJobRetentionScheme => TestScenarioInfo(
      debitCardJourney                = TestJourneys.JrsJobRetentionScheme.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.JrsJobRetentionScheme.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Pay Coronavirus Job Retention Scheme grants back",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Talu grantiau’r Cynllun Cadw Swyddi yn sgil Coronafeirws yn ôl",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Pay Coronavirus Job Retention Scheme grants back",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Talu grantiau’r Cynllun Cadw Swyddi yn sgil Coronafeirws yn ôl",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case PfCds => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfCds.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfCds.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "CDS",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = None,
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "CDS",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = None,
      hasWelshTest                    = false,
      hasAReturnUrl                   = false
    )

    case NiEuVatOss => TestScenarioInfo(
      debitCardJourney                = TestJourneys.NiEuVatOss.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.NiEuVatOss.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "VAT One Stop Shop Union scheme",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = None,
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "VAT One Stop Shop Union scheme",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = None,
      hasWelshTest                    = false,
      hasAReturnUrl                   = false
    )

    case PfNiEuVatOss => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfNiEuVatOss.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfNiEuVatOss.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "VAT One Stop Shop Union scheme",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = None,
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "VAT One Stop Shop Union scheme",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = None,
      hasWelshTest                    = false,
      hasAReturnUrl                   = false
    )

    case NiEuVatIoss => TestScenarioInfo(
      debitCardJourney                = TestJourneys.NiEuVatIoss.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.NiEuVatIoss.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "VAT Import One Stop Shop",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = None,
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "VAT Import One Stop Shop",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = None,
      hasWelshTest                    = false,
      hasAReturnUrl                   = false
    )

    case PfNiEuVatIoss => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfNiEuVatIoss.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfNiEuVatIoss.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "VAT Import One Stop Shop",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = None,
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "VAT Import One Stop Shop",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = None,
      hasWelshTest                    = false,
      hasAReturnUrl                   = false
    )

    case AppSa => TestScenarioInfo(
      debitCardJourney                = TestJourneys.AppSa.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.AppSa.journeyAfterSucceedCreditWebPayment,
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

    case AppSimpleAssessment => TestScenarioInfo(
      debitCardJourney                = TestJourneys.AppSimpleAssessment.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.AppSimpleAssessment.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Simple Assessment",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Asesiad Syml",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Simple Assessment",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Asesiad Syml",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case Mib => TestScenarioInfo(
      debitCardJourney                = TestJourneys.Mib.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.Mib.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Payment" -> "Commercial goods carried in accompanied baggage or small vehicles",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Taliad" -> "Nwyddau masnachol sy’n cael eu cario mewn bagiau neu gerbydau bach",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Payment" -> "Commercial goods carried in accompanied baggage or small vehicles",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Taliad" -> "Nwyddau masnachol sy’n cael eu cario mewn bagiau neu gerbydau bach",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case PfTpes => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfTpes.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfTpes.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Other taxes, penalties and enquiry settlements",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Trethi, cosbau a setliadau ymholiadau eraill",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Other taxes, penalties and enquiry settlements",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Trethi, cosbau a setliadau ymholiadau eraill",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case PfMgd => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfMgd.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfMgd.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Machine Games Duty",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Toll Peiriannau Hapchwarae",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Machine Games Duty",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Toll Peiriannau Hapchwarae",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case PfGbPbRgDuty => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfGbPbRgDuty.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfGbPbRgDuty.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "General Betting, Pool Betting or Remote Gaming Duty",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Toll Betio Cyffredinol, Toll Cronfa Fetio neu Doll Hapchwarae o Bell",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "General Betting, Pool Betting or Remote Gaming Duty",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Toll Betio Cyffredinol, Toll Cronfa Fetio neu Doll Hapchwarae o Bell",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case PfTrust => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfTrust.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfTrust.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Trust Registration Service penalty charge",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Tâl cosb y Gwasanaeth Cofrestru Ymddiriedolaethau",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Trust Registration Service penalty charge",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Tâl cosb y Gwasanaeth Cofrestru Ymddiriedolaethau",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case PfPsAdmin => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfPsAdmin.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfPsAdmin.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Pension scheme tax charges",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Taliadau treth gynllun pensiwn",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Pension scheme tax charges",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Taliadau treth gynllun pensiwn",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case PfOther => TestScenarioInfo(
      debitCardJourney                = TestJourneys.PfOther.journeyAfterSucceedDebitWebPayment,
      creditCardJourney               = TestJourneys.PfOther.journeyAfterSucceedCreditWebPayment,
      englishSummaryRowsDebitCard     = List(
        "Tax" -> "Other taxes, penalties and enquiry settlements",
        "Date" -> "2 November 2027",
        "Amount" -> "£12.34"
      ),
      maybeWelshSummaryRowsDebitCard  = Some(List(
        "Treth" -> "Trethi, cosbau a setliadau ymholiadau eraill",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm" -> "£12.34"
      )),
      englishSummaryRowsCreditCard    = List(
        "Tax" -> "Other taxes, penalties and enquiry settlements",
        "Date" -> "2 November 2027",
        "Amount paid to HMRC" -> "£12.34",
        "Card fee (9.97%), non-refundable" -> "£1.23",
        "Total paid" -> "£13.57"
      ),
      maybeWelshSummaryRowsCreditCard = Some(List(
        "Treth" -> "Trethi, cosbau a setliadau ymholiadau eraill",
        "Dyddiad" -> "2 Tachwedd 2027",
        "Swm a dalwyd i CThEM" -> "£12.34",
        "Ffi cerdyn (9.97%), ni ellir ei ad-dalu" -> "£1.23",
        "Cyfanswm a dalwyd" -> "£13.57"
      )),
      hasWelshTest                    = true,
      hasAReturnUrl                   = false
    )

    case o: Origin => throw new MatchError(s"Add testdata for new origin you've added [${o.entryName}]. Add it to implemented origins.")
  }

}
