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

import org.scalatest.Assertion
import org.scalatest.prop.TableDrivenPropertyChecks
import payapi.cardpaymentjourney.model.journey.{Journey, JourneySpecificData, Url}
import payapi.corcommon.model.{Origin, Origins}
import play.api.test.FakeRequest
import uk.gov.hmrc.cardpaymentfrontend.models.paymentssurvey.{AuditOptions, PaymentSurveyJourneyRequest, SurveyBannerTitle, SurveyContentOptions}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.{ItSpec, TestHelpers}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.{TestJourneys, TestPaymentsSurveyData}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps.FakeRequestOps
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.PaymentsSurveyStub

class PaymentsSurveyServiceSpec extends ItSpec with TableDrivenPropertyChecks {

  val systemUnderTest: PaymentsSurveyService = app.injector.instanceOf[PaymentsSurveyService]

  "PaymentsSurveyService" - {
    "startPaySurvey" - {
      "return a future Url given call to PaymentsSurvey succeeds" in {
        PaymentsSurveyStub.stubForStartJourney2xx(TestPaymentsSurveyData.ssJResponse)
        val result = systemUnderTest.startPaySurvey(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)(FakeRequest())
        result.futureValue shouldBe Url("http://survey-redirect-url.com")
      }
      "fail when call to PaymentsSurvey fails" in {
        PaymentsSurveyStub.stubForStartJourney5xx()
        val result = systemUnderTest.startPaySurvey(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment)(FakeRequest())
        result.failed.futureValue.getMessage shouldBe s"POST of 'http://localhost:${wireMockPort.toString}/payments-survey/journey/start' returned 503. Response body: ''"
      }
    }

    "makeSsjJourneyRequest" - {

      val loggedInFakeRequest = FakeRequest().withAuthSession()
      val loggedOutFakeRequest = FakeRequest()

      "correctly build a PaymentSurveyJourneyRequest" - {
        TestHelpers.implementedOrigins.foreach { origin =>
          s"for origin: ${origin.entryName}" in {
            val (paymentSurveyJourneyRequest, loggedIn) = originToPaymentSurveyJourneyRequestAndLoggedInBool(origin)
            test(origin, paymentSurveyJourneyRequest, TestHelpers.deriveTestDataFromOrigin(origin).journeyAfterSucceedDebitWebPayment, loggedIn)
          }
        }

          def test(
              origin:                              Origin,
              expectedPaymentSurveyJourneyRequest: PaymentSurveyJourneyRequest,
              testJourney:                         Journey[JourneySpecificData],
              loggedIn:                            Boolean
          ): Assertion = {
            val result = systemUnderTest.makeSsjJourneyRequest(testJourney)(if (loggedIn) loggedInFakeRequest else loggedOutFakeRequest)
            result shouldBe expectedPaymentSurveyJourneyRequest withClue s"Failed for origin: ${origin.entryName}, check the ExtendedOrigin"
          }

          def originToPaymentSurveyJourneyRequestAndLoggedInBool(origin: Origin): (PaymentSurveyJourneyRequest, Boolean) = origin match {
            case Origins.PfSa => PaymentSurveyJourneyRequest(
              origin         = "PfSa",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "self-assessment",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("1234567895K"),
                liability = Some("self-assessment")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Self Assessment", welshValue = Some("Talu eich Hunanasesiad")
                )
              )
            ) -> false
            case Origins.PfVat => PaymentSurveyJourneyRequest(
              origin         = "PfVat",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "vat",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("999964805"),
                liability = Some("vat")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your VAT", welshValue = Some("Talu eich TAW")
                )
              )
            ) -> false
            case Origins.PfCt => PaymentSurveyJourneyRequest(
              origin         = "PfCt",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "corporation-tax",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("1097172564A00101A"),
                liability = Some("corporation-tax")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Corporation Tax", welshValue = Some("Talu eich Treth Gorfforaeth")
                )
              )
            ) -> false
            case Origins.PfEpayeNi => PaymentSurveyJourneyRequest(
              origin         = "PfEpayeNi",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "epaye",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("123PH456789002503"),
                liability = Some("epaye")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your employers’ PAYE and National Insurance", welshValue = Some("Talwch eich TWE a’ch Yswiriant Gwladol y cyflogwr")
                )
              )
            ) -> false
            case Origins.PfEpayeLpp => PaymentSurveyJourneyRequest(
              origin         = "PfEpayeLpp",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "paye-lpp",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("paye-lpp")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your PAYE late payment or filing penalty", welshValue = Some("Talu’ch cosb am dalu neu gyflwyno TWE yn hwyr")
                )
              )
            ) -> false
            case Origins.PfEpayeSeta => PaymentSurveyJourneyRequest(
              origin         = "PfEpayeSeta",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "paye-seta",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XA123456789012"),
                liability = Some("paye-seta")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your PAYE Settlement Agreement", welshValue = Some("Talwch eich Cytundeb Setliad TWE y cyflogwr")
                )
              )
            ) -> false
            case Origins.PfEpayeLateCis => PaymentSurveyJourneyRequest(
              origin         = "PfEpayeLateCis",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "paye-late-cis",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("paye-late-cis")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Construction Industry Scheme penalty", welshValue = Some("Talwch eich cosb - Cynllun y Diwydiant Adeiladu")
                )
              )
            ) -> false
            case Origins.PfEpayeP11d => PaymentSurveyJourneyRequest(
              origin         = "PfEpayeP11d",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "paye-p11d",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("123PH456789002513"),
                liability = Some("paye-p11d")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your employers’ Class 1A National Insurance (P11D bill)", welshValue = Some("Talu’ch Yswiriant Gwladol Dosbarth 1A y cyflogwr (bil P11D)")
                )
              )
            ) -> false
            case Origins.BtaSa => PaymentSurveyJourneyRequest(
              origin         = "BtaSa",
              returnMsg      = "Skip survey, return to business tax account",
              returnHref     = "/business-account",
              auditName      = "self-assessment",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("1234567895K"),
                liability = Some("self-assessment")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Self Assessment", welshValue = Some("Talu eich Hunanasesiad")
                )
              )
            ) -> true
            case Origins.BtaVat => PaymentSurveyJourneyRequest(
              origin         = "BtaVat",
              returnMsg      = "Skip survey, return to business tax account",
              returnHref     = "/business-account",
              auditName      = "vat",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("999964805"),
                liability = Some("vat")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your VAT", welshValue = Some("Talu eich TAW")
                )
              )
            ) -> true
            case Origins.BtaEpayeBill => PaymentSurveyJourneyRequest(
              origin         = "BtaEpayeBill",
              returnMsg      = "Skip survey, return to business tax account",
              returnHref     = "/business-account",
              auditName      = "epaye",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("123PH456789002702"),
                liability = Some("epaye")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your employers’ PAYE and National Insurance", welshValue = Some("Talwch eich TWE a’ch Yswiriant Gwladol y cyflogwr")
                )
              )
            ) -> true
            case Origins.BtaEpayePenalty => PaymentSurveyJourneyRequest(
              origin         = "BtaEpayePenalty",
              returnMsg      = "Skip survey, return to business tax account",
              returnHref     = "/business-account",
              auditName      = "paye-penalty",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("123PH45678900"),
                liability = Some("paye-penalty")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your PAYE late payment or filing penalty", welshValue = Some("Talu’ch cosb am dalu neu gyflwyno TWE yn hwyr")
                )
              )
            ) -> true
            case Origins.BtaEpayeInterest => PaymentSurveyJourneyRequest(
              origin         = "BtaEpayeInterest",
              returnMsg      = "Skip survey, return to business tax account",
              returnHref     = "/business-account",
              auditName      = "paye-interest",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("paye-interest")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay employers’ PAYE interest", welshValue = Some("Taliad llog TWE cyflogwr")
                )
              )
            ) -> true
            case Origins.BtaEpayeGeneral => PaymentSurveyJourneyRequest(
              origin         = "BtaEpayeGeneral",
              returnMsg      = "Skip survey, return to business tax account",
              returnHref     = "/business-account",
              auditName      = "epaye",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("123PH456789002702"),
                liability = Some("epaye")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your employers’ PAYE and National Insurance", welshValue = Some("Talwch eich TWE a’ch Yswiriant Gwladol y cyflogwr")
                )
              )
            ) -> true
            case Origins.BtaClass1aNi => PaymentSurveyJourneyRequest(
              origin         = "BtaClass1aNi",
              returnMsg      = "Skip survey, return to business tax account",
              returnHref     = "/business-account",
              auditName      = "class-1a-national-insurance",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("123PH456789002713"),
                liability = Some("class-1a-national-insurance")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your employers’ Class 1A National Insurance (P11D bill)", welshValue = Some("Talu’ch Yswiriant Gwladol Dosbarth 1A y cyflogwr (bil P11D)")
                )
              )
            ) -> true
            case Origins.BtaCt => PaymentSurveyJourneyRequest(
              origin         = "BtaCt",
              returnMsg      = "Skip survey, return to business tax account",
              returnHref     = "/business-account",
              auditName      = "corporation-tax",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("1097172564A00101A"),
                liability = Some("corporation-tax")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Corporation Tax", welshValue = Some("Talu eich Treth Gorfforaeth")
                )
              )
            ) -> true
            case Origins.VcVatReturn => PaymentSurveyJourneyRequest(
              origin         = "VcVatReturn",
              returnMsg      = "Skip survey, return to business tax account",
              returnHref     = "/business-account",
              auditName      = "vat",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("999964805"),
                liability = Some("vat")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Business tax account", welshValue = Some("Cyfrif treth busnes")
                )
              )
            ) -> true
            case Origins.ItSa => PaymentSurveyJourneyRequest(
              origin         = "ItSa",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "self-assessment",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("1234567895K"),
                liability = Some("self-assessment")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Self Assessment", welshValue = Some("Talu eich Hunanasesiad")
                )
              )
            ) -> true
            case Origins.Amls => PaymentSurveyJourneyRequest(
              origin         = "Amls",
              returnMsg      = "Skip survey, return to personal tax account",
              returnHref     = "/personal-account",
              auditName      = "anti-money-laundering",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("anti-money-laundering")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay Money Laundering Regulations fees", welshValue = Some("Talu Ffioedd Rheoliadau Gwyngalchu Arian")
                )
              )
            ) -> true
            case Origins.Ppt => PaymentSurveyJourneyRequest(
              origin         = "Ppt",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "plastic-packaging-tax",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("XAPPT0000012345"),
                liability = Some("plastic-packaging-tax")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Plastic Packaging Tax", welshValue = Some("Talu’ch Treth Deunydd Pacio Plastig")
                )
              )
            ) -> true
            case Origins.PfPpt => PaymentSurveyJourneyRequest(
              origin         = "PfPpt",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "plastic-packaging-tax",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XAPPT0000012345"),
                liability = Some("plastic-packaging-tax")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Plastic Packaging Tax", welshValue = Some("Talu’ch Treth Deunydd Pacio Plastig")
                )
              )
            ) -> false
            case Origins.PtaSa => PaymentSurveyJourneyRequest(
              origin         = "PtaSa",
              returnMsg      = "Skip survey, return to personal tax account",
              returnHref     = "/personal-account",
              auditName      = "self-assessment",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("1234567895K"),
                liability = Some("self-assessment")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Self Assessment", welshValue = Some("Talu eich Hunanasesiad")
                )
              )
            ) -> true
            case Origins.PfAmls => PaymentSurveyJourneyRequest(
              origin         = "PfAmls",
              returnMsg      = "Skip survey, return to personal tax account",
              returnHref     = "/personal-account",
              auditName      = "anti-money-laundering",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("anti-money-laundering")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay Money Laundering Regulations fees", welshValue = Some("Talu Ffioedd Rheoliadau Gwyngalchu Arian")
                )
              )
            ) -> false
            case Origins.AlcoholDuty => PaymentSurveyJourneyRequest(
              origin         = "AlcoholDuty",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "alcohol-duty",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("XMADP0123456789"),
                liability = Some("alcohol-duty")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Alcohol Duty", welshValue = Some("Talu’ch Toll Alcohol")
                )
              )
            ) -> true
            case Origins.PfAlcoholDuty => PaymentSurveyJourneyRequest(
              origin         = "PfAlcoholDuty",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "alcohol-duty",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XMADP0123456789"),
                liability = Some("alcohol-duty")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Alcohol Duty", welshValue = Some("Talu’ch Toll Alcohol")
                )
              )
            ) -> false
            case Origins.WcSa => PaymentSurveyJourneyRequest(
              origin         = "WcSa",
              returnMsg      = "Skip survey",
              returnHref     = "/business-account",
              auditName      = "self-assessment",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("1234567895K"),
                liability = Some("self-assessment")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Self Assessment", welshValue = Some("Talu eich Hunanasesiad")
                )
              )
            ) -> false
            case Origins.WcCt => PaymentSurveyJourneyRequest(
              origin         = "WcCt",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "corporation-tax",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("1097172564A00101A"),
                liability = Some("corporation-tax")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Corporation Tax", welshValue = Some("Talu eich Treth Gorfforaeth")
                )
              )
            ) -> false
            case Origins.WcVat => PaymentSurveyJourneyRequest(
              origin         = "WcVat",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "vat",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("999964805"),
                liability = Some("vat")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your VAT", welshValue = Some("Talu eich TAW")
                )
              )
            ) -> false
            case Origins.WcClass1aNi => PaymentSurveyJourneyRequest(
              origin         = "WcClass1aNi",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "class-1a-national-insurance",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("123PH456789002713"),
                liability = Some("class-1a-national-insurance")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your employers’ Class 1A National Insurance (P11D bill)", welshValue = Some("Talu’ch Yswiriant Gwladol Dosbarth 1A y cyflogwr (bil P11D)")
                )
              )
            ) -> false
            case Origins.WcXref => PaymentSurveyJourneyRequest(
              origin         = "WcXref",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "other-taxes",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("other-taxes")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your tax", welshValue = Some("Talwch eich treth")
                )
              )
            ) -> false
            case Origins.VcVatOther => PaymentSurveyJourneyRequest(
              origin         = "VcVatOther",
              returnMsg      = "Skip survey, return to business tax account",
              returnHref     = "/business-account",
              auditName      = "vat",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("999964805"),
                liability = Some("vat")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Business tax account", welshValue = Some("Cyfrif treth busnes")
                )
              )
            ) -> true
            case Origins.CapitalGainsTax => PaymentSurveyJourneyRequest(
              origin         = "CapitalGainsTax",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "capital-gains-tax",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("XVCGTP001000290"),
                liability = Some("capital-gains-tax")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Report and pay Capital Gains Tax on UK property", welshValue = Some("Rhoi gwybod am a thalu Treth Enillion Cyfalaf ar eiddo yn y DU")
                )
              )
            ) -> true
            case Origins.EconomicCrimeLevy => PaymentSurveyJourneyRequest(
              origin         = "EconomicCrimeLevy",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "economic-crime-levy",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("economic-crime-levy")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay Economic Crime Levy", welshValue = Some("Talu Ardoll Troseddau Economaidd")
                )
              )
            ) -> true
            case Origins.PfEconomicCrimeLevy => PaymentSurveyJourneyRequest(
              origin         = "PfEconomicCrimeLevy",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "economic-crime-levy",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("economic-crime-levy")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay Economic Crime Levy", welshValue = Some("Talu Ardoll Troseddau Economaidd")
                )
              )
            ) -> false
            case Origins.PfSdlt => PaymentSurveyJourneyRequest(
              origin         = "PfSdlt",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "stamp-duty",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("123456789MA"),
                liability = Some("stamp-duty")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Stamp Duty Land Tax", welshValue = Some("Talu eich Treth Dir y Tollau Stamp")
                )
              )
            ) -> false
            case Origins.VatC2c => PaymentSurveyJourneyRequest(
              origin         = "VatC2c",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "vat",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("XVC1A2B3C4D5E6F"),
                liability = Some("vat")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your import VAT", welshValue = Some("Talu eich TAW fewnforio")
                )
              )
            ) -> true
            case Origins.PfVatC2c => PaymentSurveyJourneyRequest(
              origin         = "PfVatC2c",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "vat",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XVC1A2B3C4D5E6F"),
                liability = Some("vat")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your import VAT", welshValue = Some("Talu eich TAW fewnforio")
                )
              )
            ) -> false
            case Origins.WcSimpleAssessment => PaymentSurveyJourneyRequest(
              origin         = "WcSimpleAssessment",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "simple-assessment",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("simple-assessment")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Simple Assessment", welshValue = Some("Talu eich Asesiad Syml")
                )
              )
            ) -> false
            case Origins.WcEpayeLpp => PaymentSurveyJourneyRequest(
              origin         = "WcEpayeLpp",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "epaye-late-payment-penalty",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("epaye-late-payment-penalty")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your PAYE late payment or filing penalty", welshValue = Some("Talu’ch cosb am dalu neu gyflwyno TWE yn hwyr")
                )
              )
            ) -> false
            case Origins.WcEpayeNi => PaymentSurveyJourneyRequest(
              origin         = "WcEpayeNi",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "epaye",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("123PH456789002501"),
                liability = Some("epaye")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your employers’ PAYE and National Insurance", welshValue = Some("Talwch eich TWE a’ch Yswiriant Gwladol y cyflogwr")
                )
              )
            ) -> false
            case Origins.WcEpayeLateCis => PaymentSurveyJourneyRequest(
              origin         = "WcEpayeLateCis",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "cis-late-filing-penalty",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("cis-late-filing-penalty")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Construction Industry Scheme penalty", welshValue = Some("Talwch eich cosb - Cynllun y Diwydiant Adeiladu")
                )
              )
            ) -> false
            case Origins.WcEpayeSeta => PaymentSurveyJourneyRequest(
              origin         = "WcEpayeSeta",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "epaye-settlement-agreement",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("epaye-settlement-agreement")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your PAYE Settlement Agreement", welshValue = Some("Talwch eich Cytundeb Setliad TWE y cyflogwr")
                )
              )
            ) -> false
            case Origins.PfChildBenefitRepayments => PaymentSurveyJourneyRequest(
              origin         = "PfChildBenefitRepayments",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "child-benefit-repayments",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("YA123456789123"),
                liability = Some("child-benefit-repayments")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Repay Child Benefit overpayments", welshValue = Some("Ad-dalu gordaliadau Budd-dal Plant")
                )
              )
            ) -> false
            case Origins.BtaSdil => PaymentSurveyJourneyRequest(
              origin         = "BtaSdil",
              returnMsg      = "Skip survey, return to business tax account",
              returnHref     = "/business-account",
              auditName      = "soft-drinks-industry-levy",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("XE1234567890123"),
                liability = Some("soft-drinks-industry-levy")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay the Soft Drinks Industry Levy", welshValue = Some("Talu Ardoll y Diwydiant Diodydd Ysgafn")
                )
              )
            ) -> true
            case Origins.PfSdil => PaymentSurveyJourneyRequest(
              origin         = "PfSdil",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "soft-drinks-industry-levy",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE1234567890123"),
                liability = Some("soft-drinks-industry-levy")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay the Soft Drinks Industry Levy", welshValue = Some("Talu Ardoll y Diwydiant Diodydd Ysgafn")
                )
              )
            ) -> false
            case Origins.NiEuVatOss => PaymentSurveyJourneyRequest(
              origin         = "NiEuVatOss",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "ni-eu-vat-oss",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("NI101747641Q424"),
                liability = Some("ni-eu-vat-oss")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = false,
                title            = SurveyBannerTitle(
                  englishValue = "Submit a One Stop Shop VAT return and pay VAT", welshValue = None
                )
              )
            ) -> false
            case Origins.PfNiEuVatOss => PaymentSurveyJourneyRequest(
              origin         = "PfNiEuVatOss",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "ni-eu-vat-oss",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("NI101747641Q424"),
                liability = Some("ni-eu-vat-oss")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = false,
                title            = SurveyBannerTitle(
                  englishValue = "Submit a One Stop Shop VAT return and pay VAT", welshValue = None
                )
              )
            ) -> false
            case Origins.NiEuVatIoss => PaymentSurveyJourneyRequest(
              origin         = "NiEuVatIoss",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "ni-eu-vat-ioss",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("IM1234567890M0624"),
                liability = Some("ni-eu-vat-ioss")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = false,
                title            = SurveyBannerTitle(
                  englishValue = "Submit an Import One Stop Shop VAT return and pay VAT", welshValue = None
                )
              )
            ) -> false
            case Origins.PfNiEuVatIoss => PaymentSurveyJourneyRequest(
              origin         = "PfNiEuVatIoss",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "ni-eu-vat-ioss",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("IM1234567890M0624"),
                liability = Some("ni-eu-vat-ioss")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = false,
                title            = SurveyBannerTitle(
                  englishValue = "Submit an Import One Stop Shop VAT return and pay VAT", welshValue = None
                )
              )
            ) -> false
            case Origins.PfP800 => PaymentSurveyJourneyRequest(
              origin         = "PfP800",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "p800-or-pa302",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("MA000003AP8002027"),
                liability = Some("p800-or-pa302")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Check how much Income Tax you paid", welshValue = Some("Gwirio faint o dreth incwm a daloch")
                )
              )
            ) -> true
            case Origins.PtaP800 => PaymentSurveyJourneyRequest(
              origin         = "PtaP800",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "p800-or-pa302",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("MA000003AP8002027"),
                liability = Some("p800-or-pa302")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Check how much Income Tax you paid", welshValue = Some("Gwirio faint o dreth incwm a daloch")
                )
              )
            ) -> true
            case Origins.PfSimpleAssessment => PaymentSurveyJourneyRequest(
              origin         = "PfSimpleAssessment",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "simple-assessment",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("simple-assessment")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Simple Assessment", welshValue = Some("Talu eich Asesiad Syml")
                )
              )
            ) -> false
            case Origins.PtaSimpleAssessment => PaymentSurveyJourneyRequest(
              origin         = "PtaSimpleAssessment",
              returnMsg      = "Skip survey, return to personal tax account",
              returnHref     = "/personal-account",
              auditName      = "p800-or-pa302",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("MA000003AP3022027"),
                liability = Some("p800-or-pa302")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Simple Assessment", welshValue = Some("Talu eich Asesiad Syml")
                )
              )
            ) -> true

            case Origins.PfJobRetentionScheme => PaymentSurveyJourneyRequest(
              origin         = "PfJobRetentionScheme",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "job-retention-scheme",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XJRS12345678901"),
                liability = Some("job-retention-scheme")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay Coronavirus Job Retention Scheme grants back",
                  welshValue   = Some("Talu grantiau’r Cynllun Cadw Swyddi yn sgil Coronafeirws yn ôl")
                )
              )
            ) -> false

            case Origins.JrsJobRetentionScheme => PaymentSurveyJourneyRequest(
              origin         = "JrsJobRetentionScheme",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "job-retention-scheme",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("XJRS12345678901"),
                liability = Some("job-retention-scheme")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay Coronavirus Job Retention Scheme grants back",
                  welshValue   = Some("Talu grantiau’r Cynllun Cadw Swyddi yn sgil Coronafeirws yn ôl")
                )
              )
            ) -> true

            case Origins.PfCds => PaymentSurveyJourneyRequest(
              origin         = "PfCds",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "cds",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("CDSI191234567890"),
                liability = Some("cds")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = false,
                title            = SurveyBannerTitle(
                  englishValue = "Customs Declaration Service", welshValue = None
                )
              )
            ) -> false

            case Origins.AppSa => PaymentSurveyJourneyRequest(
              origin         = "AppSa",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "self-assessment",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("1234567890K"),
                liability = Some("self-assessment")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Self Assessment", welshValue = Some("Talu eich Hunanasesiad")
                )
              )
            ) -> false

            case Origins.AppSimpleAssessment => PaymentSurveyJourneyRequest(
              origin         = "AppSimpleAssessment",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "simple-assessment",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("MA000003AP3022023"),
                liability = Some("simple-assessment")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your Simple Assessment", welshValue = Some("Talu eich Asesiad Syml")
                )
              )
            ) -> false

            case Origins.Mib => PaymentSurveyJourneyRequest(
              origin         = "Mib",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "merchandise-in-baggage",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("MIBI1234567891"),
                liability = Some("merchandise-in-baggage")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Declare commercial goods carried in accompanied baggage or small vehicles",
                  welshValue   = Some("Datgan nwyddau masnachol sy’n cael eu cario mewn bagiau neu gerbydau bach")
                )
              )
            ) -> true

            case Origins.BcPngr => PaymentSurveyJourneyRequest(
              origin         = "BcPngr",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "passengers",
              audit          = AuditOptions(
                userType  = "LoggedIn",
                journey   = Some("Successful"),
                orderId   = Some("XAPR9876543210"),
                liability = Some("passengers")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Check tax on goods you bring into the UK",
                  welshValue   = Some("Gwirio’r dreth ar nwyddau rydych yn dod â nhw i’r DU")
                )
              )
            ) -> true

            case Origins.PfTpes => PaymentSurveyJourneyRequest(
              origin         = "PfTpes",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "tax-penalties-and-enquiry-settlements",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("tax-penalties-and-enquiry-settlements")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay taxes, penalties or enquiry settlements", welshValue = Some("Talu trethi, cosbau neu setliadau ymholiadauhmmm")
                )
              )
            ) -> false

            case Origins.PfMgd => PaymentSurveyJourneyRequest(
              origin         = "PfMgd",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "machine-games-duty",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("machine-games-duty")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay Machine Games Duty", welshValue = Some("Talu’r Doll Peiriannau Hapchwarae")
                )
              )
            ) -> false

            case Origins.PfGbPbRgDuty => PaymentSurveyJourneyRequest(
              origin         = "PfGbPbRgDuty",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "general-betting-pool-betting-remote-gaming-duty",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("general-betting-pool-betting-remote-gaming-duty")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay General Betting, Pool Betting or Remote Gaming Duty", welshValue = Some("Talu Toll Betio Cyffredinol, Toll Cronfa Fetio neu Doll Hapchwarae o Bell")
                )
              )
            ) -> false

            case Origins.PfTrust => PaymentSurveyJourneyRequest(
              origin         = "PfTrust",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "pf-trust",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("pf-trust")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay a Trust Registration Service penalty charge", welshValue = Some("Talu tâl cosb y Gwasanaeth Cofrestru Ymddiriedolaethau")
                )
              )
            ) -> false

            case Origins.PfPsAdmin => PaymentSurveyJourneyRequest(
              origin         = "PfPsAdmin",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "pension-scheme-administrators",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("pension-scheme-administrators")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your pension scheme tax charges", welshValue = Some("Talu’ch taliadau treth ar gynllun pensiwn")
                )
              )
            ) -> false

            case Origins.PfOther => PaymentSurveyJourneyRequest(
              origin         = "PfOther",
              returnMsg      = "Skip survey",
              returnHref     = "https://www.gov.uk/government/organisations/hm-revenue-customs",
              auditName      = "other-taxes",
              audit          = AuditOptions(
                userType  = "LoggedOut",
                journey   = Some("Successful"),
                orderId   = Some("XE123456789012"),
                liability = Some("other-taxes")
              ),
              contentOptions = SurveyContentOptions(
                isWelshSupported = true,
                title            = SurveyBannerTitle(
                  englishValue = "Pay your tax", welshValue = Some("Talwch eich treth")
                )
              )
            ) -> false
            case Origins.Parcels             => throw new MatchError("Not implemented yet")
            case Origins.DdVat               => throw new MatchError("Not implemented yet")
            case Origins.DdSdil              => throw new MatchError("Not implemented yet")
            case Origins.PfCdsCash           => throw new MatchError("Not implemented yet")
            case Origins.PfSpiritDrinks      => throw new MatchError("Not implemented yet")
            case Origins.PfInheritanceTax    => throw new MatchError("Not implemented yet")
            case Origins.PfClass3Ni          => throw new MatchError("Not implemented yet")
            case Origins.PfWineAndCider      => throw new MatchError("Not implemented yet")
            case Origins.PfBioFuels          => throw new MatchError("Not implemented yet")
            case Origins.PfAirPass           => throw new MatchError("Not implemented yet")
            case Origins.PfBeerDuty          => throw new MatchError("Not implemented yet")
            case Origins.PfGamingOrBingoDuty => throw new MatchError("Not implemented yet")
            case Origins.PfLandfillTax       => throw new MatchError("Not implemented yet")
            case Origins.PfAggregatesLevy    => throw new MatchError("Not implemented yet")
            case Origins.PfClimateChangeLevy => throw new MatchError("Not implemented yet")
            case Origins.PfImportedVehicles  => throw new MatchError("Not implemented yet")
            case Origins.PfAted              => throw new MatchError("Not implemented yet")
            case Origins.PfCdsDeferment      => throw new MatchError("Not implemented yet")
            case Origins.PtaClass3Ni         => throw new MatchError("Not implemented yet")
            case Origins.`3psSa`             => throw new MatchError("Not implemented yet")
            case Origins.`3psVat`            => throw new MatchError("Not implemented yet")
            case Origins.Pillar2             => throw new MatchError("Not implemented yet")
            case Origins.PfPillar2           => throw new MatchError("Not implemented yet")
            case Origins.PfClass2Ni          => throw new MatchError("Not implemented yet")
            case Origins.PfInsurancePremium  => throw new MatchError("Not implemented yet")
            case Origins.WcClass2Ni          => throw new MatchError("Not implemented yet")

          }
      }
    }
  }

}
