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
import payapi.corcommon.model.Origins
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.cardpaymentfrontend.models.{Link, PaymentMethod}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.ItSpec
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PtaSa.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfCt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaVat.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Ppt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfPpt.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.Amls.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
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
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }

        "render an option for open banking in welsh" in {
          PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfAmls.journeyBeforeBeginWebPayment)
          val result = systemUnderTest.renderPage(fakeWelshRequest)
          val document = Jsoup.parse(contentAsString(result))
          val listOfMethods = document.select("#payment-type-list").select("li")
          val openBankingBullet = listOfMethods.select("#open-banking-link")
          openBankingBullet.text() shouldBe "cyfrif banc"
          openBankingBullet.attr("href") shouldBe "/start-open-banking"
        }
      }

    }

    "POST /card-fees" - {
      "should redirect to the enter email address page" in {
        val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "/card-fees").withSessionId()
        PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
        val result = systemUnderTest.submit(fakeRequest)
        redirectLocation(result) shouldBe Some("/email-address")
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

    "linksAvailableOnFeesPage" - {

      val expectedOpenBankingLink = Link(
        href       = Call("GET", "/start-open-banking"),
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

      "should return the correct links for each origin" in {
        Origins.values.foreach { origin =>

          val expectedLinks = origin match {
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
            case Origins.PfSdlt                   => Seq.empty
            case Origins.PfCds                    => Seq.empty
            case Origins.PfOther                  => Seq.empty
            case Origins.PfP800                   => Seq.empty
            case Origins.PtaP800                  => Seq.empty
            case Origins.PfClass2Ni               => Seq.empty
            case Origins.PfInsurancePremium       => Seq.empty
            case Origins.PfPsAdmin                => Seq.empty
            case Origins.AppSa                    => Seq.empty
            case Origins.BtaVat                   => Seq(expectedOpenBankingLink, expectedVariableDirectDebitLink)
            case Origins.BtaEpayeBill             => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
            case Origins.BtaEpayePenalty          => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
            case Origins.BtaEpayeInterest         => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
            case Origins.BtaEpayeGeneral          => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
            case Origins.BtaClass1aNi             => Seq.empty
            case Origins.BtaCt                    => Seq(expectedOpenBankingLink, expectedOneOffDirectDebitLink)
            case Origins.BtaSdil                  => Seq.empty
            case Origins.BcPngr                   => Seq.empty
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
            case Origins.Mib                      => Seq.empty
            case Origins.PfClass3Ni               => Seq.empty
            case Origins.PfWineAndCider           => Seq.empty
            case Origins.PfBioFuels               => Seq.empty
            case Origins.PfAirPass                => Seq.empty
            case Origins.PfMgd                    => Seq.empty
            case Origins.PfBeerDuty               => Seq.empty
            case Origins.PfGamingOrBingoDuty      => Seq.empty
            case Origins.PfGbPbRgDuty             => Seq.empty
            case Origins.PfLandfillTax            => Seq.empty
            case Origins.PfSdil                   => Seq.empty
            case Origins.PfAggregatesLevy         => Seq.empty
            case Origins.PfClimateChangeLevy      => Seq.empty
            case Origins.PfSimpleAssessment       => Seq.empty
            case Origins.PtaSimpleAssessment      => Seq.empty
            case Origins.AppSimpleAssessment      => Seq.empty
            case Origins.PfTpes                   => Seq.empty
            case Origins.CapitalGainsTax          => Seq.empty
            case Origins.EconomicCrimeLevy        => Seq.empty
            case Origins.PfEconomicCrimeLevy      => Seq.empty
            case Origins.PfJobRetentionScheme     => Seq.empty
            case Origins.JrsJobRetentionScheme    => Seq.empty
            case Origins.PfImportedVehicles       => Seq.empty
            case Origins.PfChildBenefitRepayments => Seq.empty
            case Origins.NiEuVatOss               => Seq.empty
            case Origins.PfNiEuVatOss             => Seq.empty
            case Origins.NiEuVatIoss              => Seq.empty
            case Origins.PfNiEuVatIoss            => Seq.empty
            case Origins.PfAmls                   => Seq(expectedOpenBankingLink)
            case Origins.PfAted                   => Seq.empty
            case Origins.PfCdsDeferment           => Seq.empty
            case Origins.PfTrust                  => Seq.empty
            case Origins.PtaClass3Ni              => Seq.empty
            case Origins.AlcoholDuty              => Seq(expectedOpenBankingLink)
            case Origins.PfAlcoholDuty            => Seq(expectedOpenBankingLink)
            case Origins.VatC2c                   => Seq.empty
            case Origins.`3psSa`                  => Seq.empty
            case Origins.`3psVat`                 => Seq.empty
            case Origins.PfPillar2                => Seq.empty
            case Origins.PfVatC2c                 => Seq.empty
            case Origins.Pillar2                  => Seq.empty

          }

          // Required due to additions made to Controller to allow for checking a chargeReference. Could this be improved?
          val journeySpecificData: Option[JourneySpecificData] = origin match {
            case Origins.PfSa                     => Some(TestJourneys.PfSa.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.BtaSa                    => Some(TestJourneys.BtaSa.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.PtaSa                    => Some(TestJourneys.PtaSa.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.ItSa                     => Some(TestJourneys.ItSa.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.PfVat                    => Some(TestJourneys.PfVat.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.PfCt                     => Some(TestJourneys.PfCt.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.PfEpayeNi                => None
            case Origins.PfEpayeLpp               => None
            case Origins.PfEpayeSeta              => None
            case Origins.PfEpayeLateCis           => None
            case Origins.PfEpayeP11d              => None
            case Origins.PfSdlt                   => None
            case Origins.PfCds                    => None
            case Origins.PfOther                  => None
            case Origins.PfP800                   => None
            case Origins.PtaP800                  => None
            case Origins.PfClass2Ni               => None
            case Origins.PfInsurancePremium       => None
            case Origins.PfPsAdmin                => None
            case Origins.AppSa                    => None
            case Origins.BtaVat                   => Some(TestJourneys.BtaVat.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.BtaEpayeBill             => None
            case Origins.BtaEpayePenalty          => None
            case Origins.BtaEpayeInterest         => None
            case Origins.BtaEpayeGeneral          => None
            case Origins.BtaClass1aNi             => None
            case Origins.BtaCt                    => Some(TestJourneys.BtaCt.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.BtaSdil                  => None
            case Origins.BcPngr                   => None
            case Origins.Parcels                  => None
            case Origins.DdVat                    => None
            case Origins.DdSdil                   => None
            case Origins.VcVatReturn              => Some(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.VcVatOther               => Some(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.Amls                     => Some(TestJourneys.Amls.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.Ppt                      => None
            case Origins.PfCdsCash                => None
            case Origins.PfPpt                    => None
            case Origins.PfSpiritDrinks           => None
            case Origins.PfInheritanceTax         => None
            case Origins.Mib                      => None
            case Origins.PfClass3Ni               => None
            case Origins.PfWineAndCider           => None
            case Origins.PfBioFuels               => None
            case Origins.PfAirPass                => None
            case Origins.PfMgd                    => None
            case Origins.PfBeerDuty               => None
            case Origins.PfGamingOrBingoDuty      => None
            case Origins.PfGbPbRgDuty             => None
            case Origins.PfLandfillTax            => None
            case Origins.PfSdil                   => None
            case Origins.PfAggregatesLevy         => None
            case Origins.PfClimateChangeLevy      => None
            case Origins.PfSimpleAssessment       => None
            case Origins.PtaSimpleAssessment      => None
            case Origins.AppSimpleAssessment      => None
            case Origins.PfTpes                   => None
            case Origins.CapitalGainsTax          => None
            case Origins.EconomicCrimeLevy        => None
            case Origins.PfEconomicCrimeLevy      => None
            case Origins.PfJobRetentionScheme     => None
            case Origins.JrsJobRetentionScheme    => None
            case Origins.PfImportedVehicles       => None
            case Origins.PfChildBenefitRepayments => None
            case Origins.NiEuVatOss               => None
            case Origins.PfNiEuVatOss             => None
            case Origins.NiEuVatIoss              => None
            case Origins.PfNiEuVatIoss            => None
            case Origins.PfAmls                   => Some(TestJourneys.PfAmls.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.PfAted                   => None
            case Origins.PfCdsDeferment           => None
            case Origins.PfTrust                  => None
            case Origins.PtaClass3Ni              => None
            case Origins.AlcoholDuty              => Some(TestJourneys.AlcoholDuty.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.PfAlcoholDuty            => Some(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment.journeySpecificData)
            case Origins.VatC2c                   => None
            case Origins.`3psSa`                  => None
            case Origins.`3psVat`                 => None
            case Origins.PfPillar2                => None
            case Origins.PfVatC2c                 => None
            case Origins.Pillar2                  => None
          }

          journeySpecificData.map { jsd: JourneySpecificData =>
            systemUnderTest.linksAvailableOnFeesPage(jsd = jsd) shouldBe expectedLinks withClue s"links did not match expected for origin: ${origin.entryName}"
          }

        }
      }
    }
  }
}
