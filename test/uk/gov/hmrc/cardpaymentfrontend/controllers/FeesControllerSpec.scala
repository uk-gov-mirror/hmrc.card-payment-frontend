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
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import payapi.cardpaymentjourney.model.journey.JourneySpecificData
import payapi.corcommon.model.{Origin, Origins}
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.cardpaymentfrontend.models.{Link, PaymentMethod}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.{ItSpec, TestHelpers}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps.FakeRequestOps
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.PayApiStub
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys

class FeesControllerSpec extends ItSpec {

  private val systemUnderTest: FeesController = app.injector.instanceOf[FeesController]

  "FeesController" - {
    "GET /card-fees" - {
      val fakeRequest = FakeRequest().withSessionId()
      val fakeWelshRequest = FakeRequest().withSessionId().withLangWelsh()

        def testStaticContentEnglish(document: Document): Assertion = {
          document.select("h1").text() shouldBe "Card fees"
          val para1 = document.select("#para1")
          para1.text() shouldBe "There is a non-refundable fee if you pay by corporate credit card or corporate debit card."
          val para2 = document.select("#para2")
          para2.text() shouldBe "There is no fee if you pay by:"
          val para3 = document.select("#para3")
          para3.text() shouldBe "You cannot pay using a personal credit card."
          val para4 = document.select("#para4")
          para4.text() shouldBe "Allow 3 working days for your payment to reach HMRC’s bank account."
        }

        def testStaticContentWelsh(document: Document): Assertion = {
          document.select("h1").text() shouldBe "Ffioedd cerdyn"
          val para1 = document.select("#para1")
          para1.text() shouldBe "Bydd ffi na ellir ei had-dalu yn cael ei chodi os talwch â cherdyn credyd corfforaethol neu gerdyn debyd corfforaethol."
          val para2 = document.select("#para2")
          para2.text() shouldBe "Nid oes ffi yn cael ei chodi os talwch drwy un o’r dulliau canlynol:"
          val para3 = document.select("#para3")
          para3.text() shouldBe "Ni allwch dalu â cherdyn credyd personol."
          val para4 = document.select("#para4")
          para4.text() shouldBe "Dylech ganiatáu 3 diwrnod gwaith i’ch taliad gyrraedd cyfrif banc CThEM."
        }

      "show the Title tab correctly in English" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.renderPage(fakeRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Card fees - Pay your Self Assessment - GOV.UK"
      }

      "show the Title tab correctly in Welsh" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.renderPage(fakeWelshRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.title shouldBe "Ffioedd cerdyn - Talu eich Hunanasesiad - GOV.UK"
      }

      "Title contains href which links to pay-frontend" in {
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.renderPage(fakeRequest)
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-header__content a").attr("href") shouldBe "http://localhost:9056/pay"
      }

      "for origin PfSa" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your Self Assessment"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich Hunanasesiad"
          testStaticContentWelsh(document)
        }

        "render three options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin BtaSa" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your Self Assessment"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich Hunanasesiad"
          testStaticContentWelsh(document)
        }

        "render three options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin PtaSa" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your Self Assessment"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich Hunanasesiad"
          testStaticContentWelsh(document)
        }

        "render three options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin ItSa" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your Self Assessment"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich Hunanasesiad"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for bank transfer" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#bank-transfer-link")
          openBankingBullet.text() shouldBe "bank transfer"
          openBankingBullet.attr("href") shouldBe "http://localhost:9056/pay/bac"
        }

        "render an option for bank transfer in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#bank-transfer-link")
          openBankingBullet.text() shouldBe "drosglwyddiad banc"
          openBankingBullet.attr("href") shouldBe "http://localhost:9056/pay/bac"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.ItSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin WcSa" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your Self Assessment"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich Hunanasesiad"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin WcCt" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your Corporation Tax"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich Treth Gorfforaeth"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin WcVat" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your VAT"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich TAW"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin WcClass1aNi" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your employers’ Class 1A National Insurance (P11D bill)"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu’ch Yswiriant Gwladol Dosbarth 1A y cyflogwr (bil P11D)"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin WcXref" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcXref.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your tax"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcXref.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talwch eich treth"
          testStaticContentWelsh(document)
        }

        "not render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcXref.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.size() shouldBe 0
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcXref.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcXref.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcXref.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin PfAlcoholDuty" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your Alcohol Duty"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu’ch Toll Alcohol"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin BtaCt" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your Corporation Tax"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich Treth Gorfforaeth"
          testStaticContentWelsh(document)
        }

        "render three options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin PfCt" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your Corporation Tax"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich Treth Gorfforaeth"
          testStaticContentWelsh(document)
        }

        "render three options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin PfVat" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your VAT"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich TAW"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }

        "render an option for variable direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#variable-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (variable)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-direct-debit"
        }

        "render an option for variable direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#variable-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (newidiol)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-direct-debit"
        }

        "render NO option for direct debit when is a Surcharge payment" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatWithChargeReference.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val variableOffDirectDebitBullet = listOfMethods.select("#variable-direct-debit-link")
          variableOffDirectDebitBullet.text() shouldBe empty
          variableOffDirectDebitBullet.hasAttr("href") shouldBe false
        }

        "render NO option for direct debit when is a Surcharge payment in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatWithChargeReference.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val variableOffDirectDebitBullet = listOfMethods.select("#variable-direct-debit-link")
          variableOffDirectDebitBullet.text() shouldBe empty
          variableOffDirectDebitBullet.hasAttr("href") shouldBe false
        }

      }

      "for origin BtaVat" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your VAT"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich TAW"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }

        "render an option for variable direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#variable-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (variable)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-direct-debit"
        }

        "render an option for variable direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#variable-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (newidiol)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-direct-debit"
        }

      }

      "for origin VcVatReturn" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Business tax account"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Cyfrif treth busnes"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }

        "render an option for variable direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#variable-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (variable)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-direct-debit"
        }

        "render an option for variable direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#variable-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (newidiol)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-direct-debit"
        }

      }

      "for origin VcVatOther" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Business tax account"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Cyfrif treth busnes"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin Ppt" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your Plastic Packaging Tax"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu’ch Treth Deunydd Pacio Plastig"
          testStaticContentWelsh(document)
        }

        "render three options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin PfPpt" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your Plastic Packaging Tax"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu’ch Treth Deunydd Pacio Plastig"
          testStaticContentWelsh(document)
        }

        "render three options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin BtaEpayeBill" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your employers’ PAYE and National Insurance"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talwch eich TWE a’ch Yswiriant Gwladol y cyflogwr"
          testStaticContentWelsh(document)
        }

        "render four options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 4
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for variable direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val variableDirectDebitBullet = listOfMethods.select("#variable-direct-debit-link")
          variableDirectDebitBullet.text() shouldBe "Direct Debit (variable)"
          variableDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin BtaEpayeGeneral" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your employers’ PAYE and National Insurance"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talwch eich TWE a’ch Yswiriant Gwladol y cyflogwr"
          testStaticContentWelsh(document)
        }

        "render four options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin BtaEpayeInterest" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay employers’ PAYE interest"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Taliad llog TWE cyflogwr"
          testStaticContentWelsh(document)
        }

        "render four options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin BtaEpayePenalty" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your PAYE late payment or filing penalty"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu’ch cosb am dalu neu gyflwyno TWE yn hwyr"
          testStaticContentWelsh(document)
        }

        "render four options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin BtaClass1aNi" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your employers’ Class 1A National Insurance (P11D bill)"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu’ch Yswiriant Gwladol Dosbarth 1A y cyflogwr (bil P11D)"
          testStaticContentWelsh(document)
        }

        "render four options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin Amls" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Amls.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay Money Laundering Regulations fees"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Amls.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu Ffioedd Rheoliadau Gwyngalchu Arian"
          testStaticContentWelsh(document)
        }

        "render four options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Amls.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Amls.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Amls.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }
      }

      "for origin PfAmls" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAmls.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay Money Laundering Regulations fees"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAmls.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu Ffioedd Rheoliadau Gwyngalchu Arian"
          testStaticContentWelsh(document)
        }

        "render four options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAmls.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAmls.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAmls.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }
      }

      "for origin VatC2c" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VatC2c.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your import VAT"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VatC2c.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich TAW fewnforio"
          testStaticContentWelsh(document)
        }

        "render four options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VatC2c.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VatC2c.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VatC2c.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }
      }

      "for origin PfVatC2c" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatC2c.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your import VAT"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatC2c.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu eich TAW fewnforio"
          testStaticContentWelsh(document)
        }

        "render four options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatC2c.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatC2c.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVatC2c.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }
      }

      "for origin WcEpayeLpp" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your PAYE late payment or filing penalty"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu’ch cosb am dalu neu gyflwyno TWE yn hwyr"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin WcEpayeNi" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your employers’ PAYE and National Insurance"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talwch eich TWE a’ch Yswiriant Gwladol y cyflogwr"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin BtaSdil" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay the Soft Drinks Industry Levy"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu Ardoll y Diwydiant Diodydd Ysgafn"
          testStaticContentWelsh(document)
        }

        "render three options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }

        "render an option for variable direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-direct-debit"
        }

        "render an option for variable direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-direct-debit"
        }
      }

      "for origin PfSdil" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay the Soft Drinks Industry Levy"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu Ardoll y Diwydiant Diodydd Ysgafn"
          testStaticContentWelsh(document)
        }

        "render three options for other ways to pay when the sdil reference is not a penalty reference" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render two options for other ways to pay when the sdil reference is a penalty reference" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyWithPenaltyReferenceBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking when sdil reference is a penalty reference" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyWithPenaltyReferenceBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card when sdil reference is a penalty reference" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyWithPenaltyReferenceBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }

        "render an option for Direct debit when sdil reference is not a penalty reference" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-direct-debit"
        }

        "render an option for Direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSdil.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#direct-debit-link")
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-direct-debit"
        }
      }

      "for origin WcEpayeLateCis" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your Construction Industry Scheme penalty"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talwch eich cosb - Cynllun y Diwydiant Adeiladu"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin WcEpayeSeta" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay your PAYE Settlement Agreement"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talwch eich Cytundeb Setliad TWE y cyflogwr"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin PfChildBenefitRepayments" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Repay Child Benefit overpayments"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Ad-dalu gordaliadau Budd-dal Plant"
          testStaticContentWelsh(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin PfJobRetentionScheme" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay Coronavirus Job Retention Scheme grants back"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu grantiau’r Cynllun Cadw Swyddi yn sgil Coronafeirws yn ôl"
          testStaticContentWelsh(document)
        }

        "render three options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render an option for bank transfer" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#bank-transfer-link")
          openBankingBullet.text() shouldBe "bank transfer"
          openBankingBullet.attr("href") shouldBe "http://localhost:9056/pay/bac"
        }

        "render an option for bank transfer in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#bank-transfer-link")
          openBankingBullet.text() shouldBe "drosglwyddiad banc"
          openBankingBullet.attr("href") shouldBe "http://localhost:9056/pay/bac"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 1
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 1
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }

      "for origin JrsJobRetentionScheme" - {

        "render three options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 3
        }

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Pay Coronavirus Job Retention Scheme grants back"
          testStaticContentEnglish(document)
        }

        "the static content correctly in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Talu grantiau’r Cynllun Cadw Swyddi yn sgil Coronafeirws yn ôl"
          testStaticContentWelsh(document)
        }

        "render an option for bank transfer" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#bank-transfer-link")
          openBankingBullet.text() shouldBe "bank transfer"
          openBankingBullet.attr("href") shouldBe "http://localhost:9056/pay/bac"
        }

        "render an option for bank transfer in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#bank-transfer-link")
          openBankingBullet.text() shouldBe "drosglwyddiad banc"
          openBankingBullet.attr("href") shouldBe "http://localhost:9056/pay/bac"
        }

        "render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 1
          oneOffDirectDebitBullet.text() shouldBe "Direct Debit (one-off payment)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for one off direct debit in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 1
          oneOffDirectDebitBullet.text() shouldBe "Debyd Uniongyrchol (taliad untro)"
          oneOffDirectDebitBullet.attr("href") shouldBe "http://localhost:9056/pay/pay-by-one-off-direct-debit"
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

        "render an option for personal debit card in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "cerdyn debyd personol"
        }
      }
      "for origin PfCds" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCds.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Customs Declaration Service"
          testStaticContentEnglish(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCds.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCds.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCds.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCds.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }
      }

      "for origin NiEuVatOss" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatOss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Submit a One Stop Shop VAT return and pay VAT"
          testStaticContentEnglish(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatOss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatOss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatOss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatOss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

      }

      "for origin PfNiEuVatOss" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatOss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Submit a One Stop Shop VAT return and pay VAT"
          testStaticContentEnglish(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatOss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatOss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatOss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatOss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

      }

      "for origin NiEuVatIoss" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatIoss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Submit an Import One Stop Shop VAT return and pay VAT"
          testStaticContentEnglish(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatIoss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatIoss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatIoss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.NiEuVatIoss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

      }

      "for origin PfNiEuVatIoss" - {

        "render the static content correctly" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatIoss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-header__service-name").html shouldBe "Submit an Import One Stop Shop VAT return and pay VAT"
          testStaticContentEnglish(document)
        }

        "render two options for other ways to pay" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatIoss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          listOfMethods.size() shouldBe 2
        }

        "render an option for open banking" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatIoss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "bank account"
          openBankingBullet.attr("href") shouldBe "/pay-by-card/start-open-banking"
        }

        "not render an option for one off direct debit" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatIoss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val oneOffDirectDebitBullet = listOfMethods.select("#one-off-direct-debit-link")
          oneOffDirectDebitBullet.size shouldBe 0
        }

        "render an option for personal debit card" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfNiEuVatIoss.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val cardBullet = listOfMethods.select("#personal-debit-card")
          cardBullet.text() shouldBe "personal debit card"
        }

      }

      "for origin Mib" - {
        "should redirect to /pay-by-card/address" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Mib.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          redirectLocation(result) shouldBe Some("/pay-by-card/address")
        }
      }

      "for origin BcPngr" - {
        "should redirect to /pay-by-card/address" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BcPngr.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeRequest)
          redirectLocation(result) shouldBe Some("/pay-by-card/address")
        }
      }
    }

    "POST /card-fees" - {
      "should redirect to the enter email address page" in {
        val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "/card-fees").withSessionId()
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.submit(fakeRequest)
        redirectLocation(result) shouldBe Some("/pay-by-card/email-address")
      }
    }

    "paymentMethodToBeShown" - {
      "should return true if the payment method passed in is within the list of provided payment methods" in {
        val result = systemUnderTest.paymentMethodToBeShown(
          PaymentMethod.OpenBanking, Set(PaymentMethod.OneOffDirectDebit, PaymentMethod.OpenBanking)
        )
        result shouldBe true
      }
      "should return false if the payment method passed in is not within the list of provided payment methods" in {
        val result = systemUnderTest.paymentMethodToBeShown(
          PaymentMethod.OpenBanking, Set(PaymentMethod.OneOffDirectDebit, PaymentMethod.Card)
        )
        result shouldBe false
      }
    }

    "originHasMdtpDirectDebit" - {

      "should return true" - {

        "when Jsd is JsdBtaSdil" in {
          systemUnderTest.journeyShouldShowMdtpDirectDebit(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment.journeySpecificData) shouldBe true
        }

        "when Jsd is JsdPfSdil and the journey softDrinksIndustryLevyRef matches softDrinksIndustryLevyRefRegex (i.e. not a penalty reference)" in {
          systemUnderTest.journeyShouldShowMdtpDirectDebit(TestJourneys.PfSdil.journeyBeforeBeginWebPayment.journeySpecificData) shouldBe true
        }

      }

      "should return false" - {

        "when Jsd is JsdPfSdil and the journey softDrinksIndustryLevyRef matches softDrinksIndustryLevyPenaltyRefRegex (i.e. is a penalty reference)" in {
          systemUnderTest.journeyShouldShowMdtpDirectDebit(TestJourneys.PfSdil.journeyWithPenaltyReferenceBeforeBeginWebPayment.journeySpecificData) shouldBe false
        }

        "for all other origins" in {
          TestHelpers.implementedOrigins
            .diff[Origin](Seq[Origin](Origins.BtaSdil, Origins.PfSdil)) // i.e. these have been tested above
            .foreach { origin =>
              val td = TestHelpers.deriveTestDataFromOrigin(origin)
              val result = systemUnderTest.journeyShouldShowMdtpDirectDebit(td.journeyBeforeBeginWebPayment.journeySpecificData)
              result shouldBe false withClue s"did not return false for origin: [ ${origin.entryName} ], check originHasMdtpDirectDebit function."
            }
        }
      }
    }

    "linksAvailableOnFeesPage" - {

      val expectedOpenBankingLink = Link(
        href       = Call("GET", "/pay-by-card/start-open-banking"),
        linkId     = "open-banking-link",
        messageKey = "card-fees.para2.open-banking"
      )

      val expectedBankTransferLink = Link(
        href       = Call("GET", "http://localhost:9056/pay/bac"),
        linkId     = "bank-transfer-link",
        messageKey = "card-fees.para2.bank-transfer"
      )

      val expectedOneOffDirectDebitLink = Link(
        href       = Call("GET", "http://localhost:9056/pay/pay-by-one-off-direct-debit"),
        linkId     = "one-off-direct-debit-link",
        messageKey = "card-fees.para2.one-off-direct-debit"
      )

      val expectedVariableDirectDebitLink = Link(
        href       = Call("GET", "http://localhost:9056/pay/pay-by-direct-debit"),
        linkId     = "variable-direct-debit-link",
        messageKey = "card-fees.para2.variable-direct-debit"
      )

      val expectedDirectDebitLink = Link(
        href       = Call("GET", "http://localhost:9056/pay/pay-by-direct-debit"),
        linkId     = "direct-debit-link",
        messageKey = "card-fees.para2.direct-debit"
      )

      TestHelpers.implementedOrigins
        .filterNot(o => o == Origins.BcPngr || o == Origins.Mib) // mib and bcpngr do not have a card fees page.
        .foreach { origin =>
          s"should return the correct links for each origin: ${origin.entryName}" in {

            val expectedLinks: Seq[Link] = origin match {
              case Origins.PfSa                     => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.BtaSa                    => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.PtaSa                    => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.ItSa                     => Seq(expectedBankTransferLink)
              case Origins.PfVat                    => Seq(expectedOpenBankingLink, expectedVariableDirectDebitLink)
              case Origins.PfCt                     => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.PfEpayeNi                => Seq(expectedOpenBankingLink, expectedVariableDirectDebitLink, expectedOneOffDirectDebitLink)
              case Origins.PfEpayeLpp               => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.PfEpayeSeta              => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.PfEpayeLateCis           => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.PfEpayeP11d              => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.PfSdlt                   => Seq(expectedOpenBankingLink)
              case Origins.PfCds                    => Seq(expectedOpenBankingLink)
              case Origins.PfOther                  => Seq.empty
              case Origins.PfP800                   => Seq.empty
              case Origins.PtaP800                  => Seq.empty
              case Origins.PfClass2Ni               => Seq.empty
              case Origins.PfInsurancePremium       => Seq.empty
              case Origins.PfPsAdmin                => Seq(expectedOpenBankingLink)
              case Origins.AppSa                    => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.BtaVat                   => Seq(expectedOpenBankingLink, expectedVariableDirectDebitLink)
              case Origins.BtaEpayeBill             => Seq(expectedOpenBankingLink, expectedVariableDirectDebitLink, expectedOneOffDirectDebitLink)
              case Origins.BtaEpayePenalty          => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.BtaEpayeInterest         => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.BtaEpayeGeneral          => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.BtaClass1aNi             => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.BtaCt                    => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.BtaSdil                  => Seq(expectedOpenBankingLink, expectedDirectDebitLink)
              case Origins.BcPngr                   => Seq.empty[Link]
              case Origins.Parcels                  => Seq.empty
              case Origins.DdVat                    => Seq.empty
              case Origins.DdSdil                   => Seq.empty
              case Origins.VcVatReturn              => Seq(expectedOpenBankingLink, expectedVariableDirectDebitLink)
              case Origins.VcVatOther               => Seq(expectedOpenBankingLink)
              case Origins.Amls                     => Seq(expectedOpenBankingLink)
              case Origins.Ppt                      => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.PfCdsCash                => Seq.empty
              case Origins.PfPpt                    => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.PfSpiritDrinks           => Seq.empty
              case Origins.PfInheritanceTax         => Seq.empty
              case Origins.Mib                      => Seq.empty[Link]
              case Origins.PfClass3Ni               => Seq.empty
              case Origins.PfWineAndCider           => Seq.empty
              case Origins.PfBioFuels               => Seq.empty
              case Origins.PfAirPass                => Seq.empty
              case Origins.PfMgd                    => Seq(expectedOpenBankingLink, expectedVariableDirectDebitLink, expectedOneOffDirectDebitLink)
              case Origins.PfBeerDuty               => Seq.empty
              case Origins.PfGamingOrBingoDuty      => Seq.empty
              case Origins.PfGbPbRgDuty             => Seq(expectedOpenBankingLink)
              case Origins.PfLandfillTax            => Seq.empty
              case Origins.PfSdil                   => Seq(expectedOpenBankingLink, expectedDirectDebitLink)
              case Origins.PfAggregatesLevy         => Seq.empty
              case Origins.PfClimateChangeLevy      => Seq.empty
              case Origins.PfSimpleAssessment       => Seq(expectedOpenBankingLink)
              case Origins.PtaSimpleAssessment      => Seq(expectedOpenBankingLink)
              case Origins.AppSimpleAssessment      => Seq(expectedBankTransferLink)
              case Origins.PfTpes                   => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.CapitalGainsTax          => Seq(expectedOpenBankingLink)
              case Origins.EconomicCrimeLevy        => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.PfEconomicCrimeLevy      => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.PfJobRetentionScheme     => Seq(expectedBankTransferLink, expectedOneOffDirectDebitLink)
              case Origins.JrsJobRetentionScheme    => Seq(expectedBankTransferLink, expectedOneOffDirectDebitLink)
              case Origins.PfImportedVehicles       => Seq.empty
              case Origins.PfChildBenefitRepayments => Seq(expectedOpenBankingLink)
              case Origins.NiEuVatOss               => Seq(expectedOpenBankingLink)
              case Origins.PfNiEuVatOss             => Seq(expectedOpenBankingLink)
              case Origins.NiEuVatIoss              => Seq(expectedOpenBankingLink)
              case Origins.PfNiEuVatIoss            => Seq(expectedOpenBankingLink)
              case Origins.PfAmls                   => Seq(expectedOpenBankingLink)
              case Origins.PfAted                   => Seq.empty
              case Origins.PfCdsDeferment           => Seq.empty
              case Origins.PfTrust                  => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
              case Origins.PtaClass3Ni              => Seq.empty
              case Origins.AlcoholDuty              => Seq(expectedOpenBankingLink)
              case Origins.PfAlcoholDuty            => Seq(expectedOpenBankingLink)
              case Origins.VatC2c                   => Seq(expectedOpenBankingLink)
              case Origins.`3psSa`                  => Seq.empty
              case Origins.`3psVat`                 => Seq.empty
              case Origins.PfPillar2                => Seq.empty
              case Origins.PfVatC2c                 => Seq(expectedOpenBankingLink)
              case Origins.Pillar2                  => Seq.empty
              case Origins.WcSa                     => Seq(expectedOpenBankingLink)
              case Origins.WcCt                     => Seq(expectedOpenBankingLink)
              case Origins.WcVat                    => Seq(expectedOpenBankingLink)
              case Origins.WcSimpleAssessment       => Seq(expectedOpenBankingLink)
              case Origins.WcClass1aNi              => Seq(expectedOpenBankingLink)
              case Origins.WcXref                   => Seq.empty
              case Origins.WcEpayeLpp               => Seq(expectedOpenBankingLink)
              case Origins.WcEpayeNi                => Seq(expectedOpenBankingLink)
              case Origins.WcEpayeLateCis           => Seq(expectedOpenBankingLink)
              case Origins.WcEpayeSeta              => Seq(expectedOpenBankingLink)
              case Origins.WcClass2Ni               => Seq.empty
            }

            val journeySpecificData: JourneySpecificData = TestHelpers.deriveTestDataFromOrigin(origin).journeyBeforeBeginWebPayment.journeySpecificData
            systemUnderTest.linksAvailableOnFeesPage(journeySpecificData) shouldBe expectedLinks withClue s"links did not match expected for origin: ${origin.entryName}"
          }
        }
    }
  }
}
