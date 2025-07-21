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

package uk.gov.hmrc.cardpaymentfrontend.services

import payapi.cardpaymentjourney.model.journey.{Journey, JourneySpecificData, JsdPfSa}
import payapi.corcommon.model.AmountInPence
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import uk.gov.hmrc.cardpaymentfrontend.actions.JourneyRequest
import uk.gov.hmrc.cardpaymentfrontend.models.Languages.English
import uk.gov.hmrc.cardpaymentfrontend.models.cardpayment._
import uk.gov.hmrc.cardpaymentfrontend.models.payapirequest.{FailWebPaymentRequest, SucceedWebPaymentRequest}
import uk.gov.hmrc.cardpaymentfrontend.models.{Address, EmailAddress}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.ItSpec
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps.FakeRequestOps
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.{AuditConnectorStub, CardPaymentStub, EmailStub, PayApiStub}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime

class CardPaymentServiceSpec extends ItSpec {

  override protected lazy val configOverrides: Map[String, Any] = Map[String, Any](
    "auditing.enabled" -> true
  )

  val systemUnderTest: CardPaymentService = app.injector.instanceOf[CardPaymentService]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  def fakeJourneyRequest(journey: Journey[JourneySpecificData], withEmail: Boolean): JourneyRequest[AnyContent] = {
    if (withEmail) new JourneyRequest(journey, FakeRequest().withEmailAndAddressInSession(journey._id))
    else new JourneyRequest(journey, FakeRequest().withAddressInSession(journey._id))
  }

  val testJourneyBeforeBeginWebPayment: Journey[JsdPfSa] = TestJourneys.PfSa.journeyBeforeBeginWebPayment
  val testJourneyAfterBeginWebPayment: Journey[JsdPfSa] = TestJourneys.PfSa.journeyAfterBeginWebPayment
  val testAddress: Address = Address("made up street", postcode = "AA11AA", country = "GBR")
  val testEmail: EmailAddress = EmailAddress("some@email.com")

  "CardPaymentService" - {

    "initiatePayment" - {

      implicit val journeyRequest: JourneyRequest[_] = fakeJourneyRequest(journey   = testJourneyBeforeBeginWebPayment, withEmail = true)

      val cardPaymentInitiatePaymentRequest = CardPaymentInitiatePaymentRequest(
        redirectUrl         = "http://localhost:10155/return-to-hmrc",
        clientId            = "SAEE",
        purchaseDescription = "1234567895K",
        purchaseAmount      = AmountInPence(1234),
        billingAddress      = BarclaycardAddress(
          line1       = "made up street",
          postCode    = "AA11AA",
          countryCode = "GBR"
        ),
        emailAddress        = Some(EmailAddress("some@email.com")),
        transactionNumber   = "00001999999999"
      )
      val expectedCardPaymentInitiatePaymentResponse = CardPaymentInitiatePaymentResponse("someiframeurl", "sometransactionref")

      "should return a CardPaymentInitiatePaymentResponse when card-payment backend returns one" in {
        CardPaymentStub.InitiatePayment.stubForInitiatePayment2xx(cardPaymentInitiatePaymentRequest, expectedCardPaymentInitiatePaymentResponse)
        val result = systemUnderTest.initiatePayment(testJourneyBeforeBeginWebPayment, testAddress, Some(testEmail), English).futureValue
        result shouldBe expectedCardPaymentInitiatePaymentResponse
      }

      "should update pay-api journey with BeginWebPaymentRequest when call to card-payment backend succeeds" in {
        CardPaymentStub.InitiatePayment.stubForInitiatePayment2xx(cardPaymentInitiatePaymentRequest, expectedCardPaymentInitiatePaymentResponse)
        systemUnderTest.initiatePayment(testJourneyBeforeBeginWebPayment, testAddress, Some(testEmail), English).futureValue
        PayApiStub.verifyUpdateBeginWebPayment(1, testJourneyBeforeBeginWebPayment._id.value)
      }

      "should trigger an explicit paymentAttempt audit event" in {
        CardPaymentStub.InitiatePayment.stubForInitiatePayment2xx(cardPaymentInitiatePaymentRequest, expectedCardPaymentInitiatePaymentResponse)
        systemUnderTest.initiatePayment(testJourneyBeforeBeginWebPayment, testAddress, Some(testEmail), English).futureValue
        PayApiStub.verifyUpdateBeginWebPayment(1, testJourneyBeforeBeginWebPayment._id.value)
        AuditConnectorStub.verifyEventAudited(
          auditType  = "PaymentAttempt",
          auditEvent = Json.parse(
            """
              |{
              | "address": {
              |   "line1": "made up street",
              |   "postcode": "AA11AA",
              |   "country": "GBR"
              | },
              | "emailAddress": "blah@blah.com",
              | "loggedIn": false,
              | "merchantCode": "SAEE",
              | "paymentOrigin": "PfSa",
              | "paymentReference": "1234567895K",
              | "paymentTaxType": "selfAssessment",
              | "paymentTotal": 12.34,
              | "transactionReference": "sometransactionref"
              |}""".stripMargin
          ).as[JsObject]
        )
      }
    }

    "finishPayment" - {
      val testTime = LocalDateTime.now(FrozenTime.clock)
      val testCardPaymentResult = CardPaymentResult(CardPaymentFinishPaymentResponses.Successful, AdditionalPaymentInfo(Some("debit"), Some(123), Some(testTime)))

      "should return Some[CardPaymentResult] when one is returned from card-payment backend" in {
        CardPaymentStub.AuthAndCapture.stubForAuthAndCapture2xx("sometransactionref", testCardPaymentResult)
        val result = systemUnderTest.finishPayment("sometransactionref", testJourneyAfterBeginWebPayment._id.value, English)(fakeJourneyRequest(testJourneyAfterBeginWebPayment, withEmail = false), messagesApi).futureValue
        result shouldBe Some(CardPaymentResult(CardPaymentFinishPaymentResponses.Successful, AdditionalPaymentInfo(Some("debit"), Some(123), Some(testTime))))
      }

      "should update pay-api with SucceedWebPaymentRequest when call to card-payment backend succeeds" in {
        CardPaymentStub.AuthAndCapture.stubForAuthAndCapture2xx("sometransactionref", testCardPaymentResult)
        systemUnderTest.finishPayment("sometransactionref", testJourneyAfterBeginWebPayment._id.value, English)(fakeJourneyRequest(testJourneyAfterBeginWebPayment, withEmail = false), messagesApi).futureValue
        PayApiStub.verifyUpdateSucceedWebPayment(1, testJourneyAfterBeginWebPayment._id.value, testTime)
      }

      "should update pay-api with FailWebPaymentRequest when call to card-payment backend indicates failure" in {
        val testCardPaymentResult = CardPaymentResult(CardPaymentFinishPaymentResponses.Failed, AdditionalPaymentInfo(Some("debit"), None, Some(testTime)))
        CardPaymentStub.AuthAndCapture.stubForAuthAndCapture2xx("sometransactionref", testCardPaymentResult)
        systemUnderTest.finishPayment("sometransactionref", testJourneyAfterBeginWebPayment._id.value, English)(fakeJourneyRequest(testJourneyAfterBeginWebPayment, withEmail = false), messagesApi).futureValue
        PayApiStub.verifyUpdateFailWebPayment(1, testJourneyAfterBeginWebPayment._id.value, testTime)
      }

      "should update pay-api with CancelWebPaymentRequest when call to card-payment backend indicates Cancelled" in {
        val testCardPaymentResult = CardPaymentResult(CardPaymentFinishPaymentResponses.Cancelled, AdditionalPaymentInfo(None, None, None))
        CardPaymentStub.AuthAndCapture.stubForAuthAndCapture2xx("sometransactionref", testCardPaymentResult)
        systemUnderTest.finishPayment("sometransactionref", testJourneyAfterBeginWebPayment._id.value, English)(fakeJourneyRequest(testJourneyAfterBeginWebPayment, withEmail = false), messagesApi).futureValue
        PayApiStub.verifyUpdateCancelWebPayment(1, testJourneyAfterBeginWebPayment._id.value)
      }

      "should send an email when there is one in session, aswell as update pay-api when journey is in sent state" in {
        CardPaymentStub.AuthAndCapture.stubForAuthAndCapture2xx("sometransactionref", testCardPaymentResult)
        systemUnderTest.finishPayment("sometransactionref", testJourneyAfterBeginWebPayment._id.value, English)(fakeJourneyRequest(testJourneyAfterBeginWebPayment, withEmail = true), messagesApi).futureValue
        PayApiStub.verifyUpdateSucceedWebPayment(1, testJourneyAfterBeginWebPayment._id.value, testTime)
        EmailStub.verifySomeEmailWasSent()
      }

      "should not send an email when there isn't one in session" in {
        CardPaymentStub.AuthAndCapture.stubForAuthAndCapture2xx("sometransactionref", testCardPaymentResult)
        systemUnderTest.finishPayment("sometransactionref", testJourneyAfterBeginWebPayment._id.value, English)(fakeJourneyRequest(testJourneyAfterBeginWebPayment, withEmail = false), messagesApi).futureValue
        PayApiStub.verifyUpdateSucceedWebPayment(1, testJourneyAfterBeginWebPayment._id.value, testTime)
        EmailStub.verifyEmailWasNotSent()
      }

      "should trigger an explicit paymentStatus audit event" in {
        CardPaymentStub.AuthAndCapture.stubForAuthAndCapture2xx("sometransactionref", testCardPaymentResult)

        systemUnderTest.finishPayment(
          "sometransactionref", testJourneyAfterBeginWebPayment._id.value, English
        )(
            fakeJourneyRequest(testJourneyAfterBeginWebPayment, withEmail = true), messagesApi
          )
          .futureValue

        PayApiStub.verifyUpdateSucceedWebPayment(1, testJourneyAfterBeginWebPayment._id.value, testTime)
        AuditConnectorStub.verifyEventAudited(
          auditType  = "PaymentResult",
          auditEvent = Json.parse(
            """
              |{
              | "address": {
              |  "line1" : "line1",
              |  "postcode" : "AA0AA0",
              |  "country" : "GBR"
              | },
              | "emailAddress": "blah@blah.com",
              | "loggedIn": false,
              | "merchantCode": "SAEE",
              | "paymentOrigin": "PfSa",
              | "paymentStatus" : "Successful",
              | "paymentReference": "1234567895K",
              | "paymentTaxType": "selfAssessment",
              | "paymentTotal": 12.34,
              | "transactionReference": "sometransactionref"
              |}""".stripMargin
          ).as[JsObject]
        )
      }
    }

    "cancelPayment" - {

      "should call the cancelPayment in the connector and the card-payment backend when given a valid journeyRequest" in {
        systemUnderTest.cancelPayment()(fakeJourneyRequest(testJourneyAfterBeginWebPayment, false)).futureValue
        CardPaymentStub.CancelPayment.verifyOne("Some-transaction-ref", "SAEE")
      }

      "should call the cancelPayment in the connector with welsh clientId when lang is welsh" in {
        systemUnderTest.cancelPayment()(new JourneyRequest(testJourneyAfterBeginWebPayment, FakeRequest().withLangWelsh())).futureValue
        CardPaymentStub.CancelPayment.verifyOne("Some-transaction-ref", "SAEC")
      }

      "should update pay api state with cancelled" in {
        systemUnderTest.cancelPayment()(fakeJourneyRequest(testJourneyAfterBeginWebPayment, false)).futureValue
        CardPaymentStub.CancelPayment.verifyOne("Some-transaction-ref", "SAEE")
        PayApiStub.verifyUpdateCancelWebPayment(1, testJourneyAfterBeginWebPayment._id.value)
      }
    }

    "cardPaymentResultIntoUpdateWebPaymentRequest" - {
      "return None when response inside CardPaymentResult is CardPaymentFinishPaymentResponses.Cancelled" in {
        val testCardPaymentResult = CardPaymentResult(CardPaymentFinishPaymentResponses.Cancelled, AdditionalPaymentInfo(None, None, None))
        val result = systemUnderTest.cardPaymentResultIntoUpdateWebPaymentRequest(testCardPaymentResult)
        result shouldBe None
      }

      "return Some[SucceedWebPaymentRequest] with payment info when response inside CardPaymentResult is CardPaymentFinishPaymentResponses.Successful " in {
        val testCardPaymentResult = CardPaymentResult(CardPaymentFinishPaymentResponses.Successful, AdditionalPaymentInfo(Some("cardcategory"), Some(123), Some(LocalDateTime.now(FrozenTime.clock))))
        val result = systemUnderTest.cardPaymentResultIntoUpdateWebPaymentRequest(testCardPaymentResult)
        result shouldBe Some(SucceedWebPaymentRequest("cardcategory", Some(123), LocalDateTime.parse("2059-11-25T16:33:51.880")))
      }

      "return Some[FailWebPaymentRequest] when response inside CardPaymentResult is CardPaymentFinishPaymentResponses.Failed" in {
        val testCardPaymentResult = CardPaymentResult(CardPaymentFinishPaymentResponses.Failed, AdditionalPaymentInfo(Some("cardcategory"), None, Some(LocalDateTime.now(FrozenTime.clock))))
        val result = systemUnderTest.cardPaymentResultIntoUpdateWebPaymentRequest(testCardPaymentResult)
        result shouldBe Some(FailWebPaymentRequest(LocalDateTime.parse("2059-11-25T16:33:51.880"), "cardcategory"))
      }
    }
  }
}
