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

import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor6}
import payapi.cardpaymentjourney.model.journey._
import payapi.corcommon.model.{AmountInPence, Origin, Origins}
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.cardpaymentfrontend.models.EmailAddress
import uk.gov.hmrc.cardpaymentfrontend.models.email.{EmailParameters, EmailRequest}
import uk.gov.hmrc.cardpaymentfrontend.models.extendedorigins.ExtendedOrigin.OriginExtended
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestHelpers.implementedOrigins
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps.FakeRequestOps
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.EmailStub
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.{JourneyStatuses, TestJourneys}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys._
import uk.gov.hmrc.cardpaymentfrontend.testsupport.{ItSpec, TestHelpers}
import uk.gov.hmrc.cardpaymentfrontend.util.SafeEquals.EqualsOps
import uk.gov.hmrc.http.HeaderCarrier

class EmailServiceSpec extends ItSpec with TableDrivenPropertyChecks {

  val systemUnderTest: EmailService = app.injector.instanceOf[EmailService]

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/").withSessionId()
  val fakeRequestInWelsh: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withLangWelsh()

  "buildEmailParameters should return EmailParameters" - {
    val commission = Some("1.23")

    // needed for compiler. if you're adding a new extended origin, add the jsd to this type/list of types.
    type JsdBounds = JsdPfEpayeNi with JsdAlcoholDuty with JsdPfPpt with JsdBtaEpayeBill with JsdPfSa with JsdPpt with JsdVcVatReturn with JsdPfEpayeP11d with JsdCapitalGainsTax with JsdBtaCt with JsdPfEpayeLateCis with JsdPfEpayeLpp with JsdPfCt with JsdBtaVat with JsdPfSdlt with JsdBtaEpayeInterest with JsdAmls with JsdBtaEpayeGeneral with JsdPfEpayeSeta with JsdPtaSa with JsdBtaClass1aNi with JsdVcVatOther with JsdPfAmls with JsdPfVat with JsdItSa with JsdBtaSa with JsdPfAlcoholDuty with JsdBtaEpayePenalty with JsdEconomicCrimeLevy with JsdPfEconomicCrimeLevy with JsdWcSa with JsdWcCt with JsdWcVat with JsdVatC2c with JsdPfVatC2c with JsdWcSimpleAssessment with JsdWcXref with JsdWcEpayeLpp with JsdWcEpayeNi with JsdWcEpayeLateCis with JsdWcClass1aNi with JsdPfChildBenefitRepayments with JsdBtaSdil with JsdPfSdil with JsdPtaP800 with JsdPfP800 with JsdPtaSimpleAssessment with JsdPfSimpleAssessment with JsdPfJobRetentionScheme with JsdJrsJobRetentionScheme with JsdWcEpayeSeta with JsdNiEuVatOss with JsdPfNiEuVatOss with JsdNiEuVatIoss with JsdPfNiEuVatIoss with JsdPfCds with JsdAppSa with JsdAppSimpleAssessment with JsdMib with JsdBcPngr with JsdPfTpes with JsdPfMgd with JsdPfGbPbRgDuty with JsdPfTrust with JsdPfOther with JsdPfPsAdmin with JsdWcChildBenefitRepayments

    val scenarios: TableFor6[JourneyStatuses[_ >: JsdBounds <: JourneySpecificData], String, String, Option[String], Option[String], String] = Table(
      ("Journey", "Tax Type", "Tax Reference", "Commission", "Total Paid", "lang"),
      (PfSa, "Self Assessment", "ending with 7895K", None, None, "en"),
      (PfSa, "Self Assessment", "ending with 7895K", commission, Some("13.57"), "en"),
      (PfSa, "Hunanasesiad", "yn gorffen gyda 7895K", None, None, "cy"),
      (PfSa, "Hunanasesiad", "yn gorffen gyda 7895K", commission, Some("13.57"), "cy"),

      (BtaSa, "Self Assessment", "ending with 7895K", None, None, "en"),
      (BtaSa, "Self Assessment", "ending with 7895K", commission, Some("13.57"), "en"),
      (BtaSa, "Hunanasesiad", "yn gorffen gyda 7895K", None, None, "cy"),
      (BtaSa, "Hunanasesiad", "yn gorffen gyda 7895K", commission, Some("13.57"), "cy"),

      (PtaSa, "Self Assessment", "ending with 7895K", None, None, "en"),
      (PtaSa, "Self Assessment", "ending with 7895K", commission, Some("13.57"), "en"),
      (PtaSa, "Hunanasesiad", "yn gorffen gyda 7895K", None, None, "cy"),
      (PtaSa, "Hunanasesiad", "yn gorffen gyda 7895K", commission, Some("13.57"), "cy"),

      (ItSa, "Self Assessment", "ending with 7895K", None, None, "en"),
      (ItSa, "Self Assessment", "ending with 7895K", commission, Some("13.57"), "en"),
      (ItSa, "Hunanasesiad", "yn gorffen gyda 7895K", None, None, "cy"),
      (ItSa, "Hunanasesiad", "yn gorffen gyda 7895K", commission, Some("13.57"), "cy"),

      (WcSa, "Self Assessment", "ending with 7895K", None, None, "en"),
      (WcSa, "Self Assessment", "ending with 7895K", commission, Some("13.57"), "en"),
      (WcSa, "Hunanasesiad", "yn gorffen gyda 7895K", None, None, "cy"),
      (WcSa, "Hunanasesiad", "yn gorffen gyda 7895K", commission, Some("13.57"), "cy"),

      (AlcoholDuty, "Alcohol Duty", "ending with 56789", None, None, "en"),
      (AlcoholDuty, "Alcohol Duty", "ending with 56789", commission, Some("13.57"), "en"),
      (AlcoholDuty, "Toll Alcohol", "yn gorffen gyda 56789", None, None, "cy"),
      (AlcoholDuty, "Toll Alcohol", "yn gorffen gyda 56789", commission, Some("13.57"), "cy"),

      (PfAlcoholDuty, "Alcohol Duty", "ending with 56789", None, None, "en"),
      (PfAlcoholDuty, "Alcohol Duty", "ending with 56789", commission, Some("13.57"), "en"),
      (PfAlcoholDuty, "Toll Alcohol", "yn gorffen gyda 56789", None, None, "cy"),
      (PfAlcoholDuty, "Toll Alcohol", "yn gorffen gyda 56789", commission, Some("13.57"), "cy"),

      (BtaCt, "Corporation Tax", "ending with 0101A", None, None, "en"),
      (BtaCt, "Corporation Tax", "ending with 0101A", commission, Some("13.57"), "en"),
      (BtaCt, "Treth Gorfforaeth", "yn gorffen gyda 0101A", None, None, "cy"),
      (BtaCt, "Treth Gorfforaeth", "yn gorffen gyda 0101A", commission, Some("13.57"), "cy"),

      (PfCt, "Corporation Tax", "ending with 0101A", None, None, "en"),
      (PfCt, "Corporation Tax", "ending with 0101A", commission, Some("13.57"), "en"),
      (PfCt, "Treth Gorfforaeth", "yn gorffen gyda 0101A", None, None, "cy"),
      (PfCt, "Treth Gorfforaeth", "yn gorffen gyda 0101A", commission, Some("13.57"), "cy"),

      (WcCt, "Corporation Tax", "ending with 0101A", None, None, "en"),
      (WcCt, "Corporation Tax", "ending with 0101A", commission, Some("13.57"), "en"),
      (WcCt, "Treth Gorfforaeth", "yn gorffen gyda 0101A", None, None, "cy"),
      (WcCt, "Treth Gorfforaeth", "yn gorffen gyda 0101A", commission, Some("13.57"), "cy"),

      (PfEpayeNi, "Employers’ PAYE and National Insurance", "ending with 02503", None, None, "en"),
      (PfEpayeNi, "Employers’ PAYE and National Insurance", "ending with 02503", commission, Some("13.57"), "en"),
      (PfEpayeNi, "TWE ac Yswiriant Gwladol y Cyflogwr", "yn gorffen gyda 02503", None, None, "cy"),
      (PfEpayeNi, "TWE ac Yswiriant Gwladol y Cyflogwr", "yn gorffen gyda 02503", commission, Some("13.57"), "cy"),

      (PfEpayeLpp, "Employers’ PAYE late payment penalty", "ending with 89012", None, None, "en"),
      (PfEpayeLpp, "Employers’ PAYE late payment penalty", "ending with 89012", commission, Some("13.57"), "en"),
      (PfEpayeLpp, "Cosb y Cyflogwr am dalu TWE yn hwyr", "yn gorffen gyda 89012", None, None, "cy"),
      (PfEpayeLpp, "Cosb y Cyflogwr am dalu TWE yn hwyr", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (PfEpayeSeta, "Employers’ PAYE Settlement Agreement", "ending with 89012", None, None, "en"),
      (PfEpayeSeta, "Employers’ PAYE Settlement Agreement", "ending with 89012", commission, Some("13.57"), "en"),
      (PfEpayeSeta, "Cytundeb Setliad TWE y Cyflogwr", "yn gorffen gyda 89012", None, None, "cy"),
      (PfEpayeSeta, "Cytundeb Setliad TWE y Cyflogwr", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (PfEpayeLateCis, "Construction Industry Scheme (CIS) late filing penalty", "ending with 89012", None, None, "en"),
      (PfEpayeLateCis, "Construction Industry Scheme (CIS) late filing penalty", "ending with 89012", commission, Some("13.57"), "en"),
      (PfEpayeLateCis, "Cynllun y Diwydiant Adeiladu (CIS) - cosb am dalu’n hwyr", "yn gorffen gyda 89012", None, None, "cy"),
      (PfEpayeLateCis, "Cynllun y Diwydiant Adeiladu (CIS) - cosb am dalu’n hwyr", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (PfEpayeP11d, "Employers’ Class 1A National Insurance", "ending with 02513", None, None, "en"),
      (PfEpayeP11d, "Employers’ Class 1A National Insurance", "ending with 02513", commission, Some("13.57"), "en"),
      (PfEpayeP11d, "Yswiriant Gwladol Dosbarth 1A y Cyflogwr", "yn gorffen gyda 02513", None, None, "cy"),
      (PfEpayeP11d, "Yswiriant Gwladol Dosbarth 1A y Cyflogwr", "yn gorffen gyda 02513", commission, Some("13.57"), "cy"),

      (PfVat, "Vat", "ending with 64805", None, None, "en"),
      (PfVat, "Vat", "ending with 64805", commission, Some("13.57"), "en"),
      (PfVat, "TAW", "yn gorffen gyda 64805", None, None, "cy"),
      (PfVat, "TAW", "yn gorffen gyda 64805", commission, Some("13.57"), "cy"),

      (BtaVat, "Vat", "ending with 64805", None, None, "en"),
      (BtaVat, "Vat", "ending with 64805", commission, Some("13.57"), "en"),
      (BtaVat, "TAW", "yn gorffen gyda 64805", None, None, "cy"),
      (BtaVat, "TAW", "yn gorffen gyda 64805", commission, Some("13.57"), "cy"),

      (WcVat, "Vat", "ending with 64805", None, None, "en"),
      (WcVat, "Vat", "ending with 64805", commission, Some("13.57"), "en"),
      (WcVat, "TAW", "yn gorffen gyda 64805", None, None, "cy"),
      (WcVat, "TAW", "yn gorffen gyda 64805", commission, Some("13.57"), "cy"),

      (VcVatOther, "Vat", "ending with 64805", None, None, "en"),
      (VcVatOther, "Vat", "ending with 64805", commission, Some("13.57"), "en"),
      (VcVatOther, "TAW", "yn gorffen gyda 64805", None, None, "cy"),
      (VcVatOther, "TAW", "yn gorffen gyda 64805", commission, Some("13.57"), "cy"),

      (VcVatReturn, "Vat", "ending with 64805", None, None, "en"),
      (VcVatReturn, "Vat", "ending with 64805", commission, Some("13.57"), "en"),
      (VcVatReturn, "TAW", "yn gorffen gyda 64805", None, None, "cy"),
      (VcVatReturn, "TAW", "yn gorffen gyda 64805", commission, Some("13.57"), "cy"),

      (Ppt, "Plastic Packaging Tax", "ending with 12345", None, None, "en"),
      (Ppt, "Plastic Packaging Tax", "ending with 12345", commission, Some("13.57"), "en"),
      (Ppt, "Dreth Deunydd Pacio Plastig", "yn gorffen gyda 12345", None, None, "cy"),
      (Ppt, "Dreth Deunydd Pacio Plastig", "yn gorffen gyda 12345", commission, Some("13.57"), "cy"),

      (PfPpt, "Plastic Packaging Tax", "ending with 12345", None, None, "en"),
      (PfPpt, "Plastic Packaging Tax", "ending with 12345", commission, Some("13.57"), "en"),
      (PfPpt, "Dreth Deunydd Pacio Plastig", "yn gorffen gyda 12345", None, None, "cy"),
      (PfPpt, "Dreth Deunydd Pacio Plastig", "yn gorffen gyda 12345", commission, Some("13.57"), "cy"),

      (BtaEpayeBill, "Employers’ PAYE and National Insurance", "ending with 02702", None, None, "en"),
      (BtaEpayeBill, "Employers’ PAYE and National Insurance", "ending with 02702", commission, Some("13.57"), "en"),
      (BtaEpayeBill, "TWE ac Yswiriant Gwladol y Cyflogwr", "yn gorffen gyda 02702", None, None, "cy"),
      (BtaEpayeBill, "TWE ac Yswiriant Gwladol y Cyflogwr", "yn gorffen gyda 02702", commission, Some("13.57"), "cy"),

      (BtaEpayePenalty, "Employers’ PAYE late payment or filing penalty", "ending with 78900", None, None, "en"),
      (BtaEpayePenalty, "Employers’ PAYE late payment or filing penalty", "ending with 78900", commission, Some("13.57"), "en"),
      (BtaEpayePenalty, "Cosb y Cyflogwr am dalu TWE yn hwyr", "yn gorffen gyda 78900", None, None, "cy"),
      (BtaEpayePenalty, "Cosb y Cyflogwr am dalu TWE yn hwyr", "yn gorffen gyda 78900", commission, Some("13.57"), "cy"),

      (BtaEpayeGeneral, "Employers’ PAYE and National Insurance", "ending with 02702", None, None, "en"),
      (BtaEpayeGeneral, "Employers’ PAYE and National Insurance", "ending with 02702", commission, Some("13.57"), "en"),
      (BtaEpayeGeneral, "TWE ac Yswiriant Gwladol y Cyflogwr", "yn gorffen gyda 02702", None, None, "cy"),
      (BtaEpayeGeneral, "TWE ac Yswiriant Gwladol y Cyflogwr", "yn gorffen gyda 02702", commission, Some("13.57"), "cy"),

      (BtaEpayeInterest, "Employers’ PAYE interest payment", "ending with 89012", None, None, "en"),
      (BtaEpayeInterest, "Employers’ PAYE interest payment", "ending with 89012", commission, Some("13.57"), "en"),
      (BtaEpayeInterest, "Taliad llog TWE cyflogwr", "yn gorffen gyda 89012", None, None, "cy"),
      (BtaEpayeInterest, "Taliad llog TWE cyflogwr", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (BtaClass1aNi, "Employers’ Class 1A National Insurance", "ending with 02713", None, None, "en"),
      (BtaClass1aNi, "Employers’ Class 1A National Insurance", "ending with 02713", commission, Some("13.57"), "en"),
      (BtaClass1aNi, "Yswiriant Gwladol Dosbarth 1A y Cyflogwr", "yn gorffen gyda 02713", None, None, "cy"),
      (BtaClass1aNi, "Yswiriant Gwladol Dosbarth 1A y Cyflogwr", "yn gorffen gyda 02713", commission, Some("13.57"), "cy"),

      (Amls, "Money Laundering Regulation Fees", "ending with 89012", None, None, "en"),
      (Amls, "Money Laundering Regulation Fees", "ending with 89012", commission, Some("13.57"), "en"),
      (Amls, "Ffioedd Rheoliadau Gwyngalchu Arian", "yn gorffen gyda 89012", None, None, "cy"),
      (Amls, "Ffioedd Rheoliadau Gwyngalchu Arian", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (PfAmls, "Money Laundering Regulation Fees", "ending with 89012", None, None, "en"),
      (PfAmls, "Money Laundering Regulation Fees", "ending with 89012", commission, Some("13.57"), "en"),
      (PfAmls, "Ffioedd Rheoliadau Gwyngalchu Arian", "yn gorffen gyda 89012", None, None, "cy"),
      (PfAmls, "Ffioedd Rheoliadau Gwyngalchu Arian", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (EconomicCrimeLevy, "Economic Crime Levy", "ending with 89012", None, None, "en"),
      (EconomicCrimeLevy, "Economic Crime Levy", "ending with 89012", commission, Some("13.57"), "en"),
      (EconomicCrimeLevy, "Ardoll Troseddau Economaidd", "yn gorffen gyda 89012", None, None, "cy"),
      (EconomicCrimeLevy, "Ardoll Troseddau Economaidd", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (PfEconomicCrimeLevy, "Economic Crime Levy", "ending with 89012", None, None, "en"),
      (PfEconomicCrimeLevy, "Economic Crime Levy", "ending with 89012", commission, Some("13.57"), "en"),
      (PfEconomicCrimeLevy, "Ardoll Troseddau Economaidd", "yn gorffen gyda 89012", None, None, "cy"),
      (PfEconomicCrimeLevy, "Ardoll Troseddau Economaidd", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (PfSdlt, "Stamp Duty Land Tax", "ending with 789MA", None, None, "en"),
      (PfSdlt, "Stamp Duty Land Tax", "ending with 789MA", commission, Some("13.57"), "en"),
      (PfSdlt, "Treth Dir y Tollau Stamp", "yn gorffen gyda 789MA", None, None, "cy"),
      (PfSdlt, "Treth Dir y Tollau Stamp", "yn gorffen gyda 789MA", commission, Some("13.57"), "cy"),

      (CapitalGainsTax, "Capital Gains Tax on UK property", "ending with 00290", None, None, "en"),
      (CapitalGainsTax, "Capital Gains Tax on UK property", "ending with 00290", commission, Some("13.57"), "en"),
      (CapitalGainsTax, "Treth Enillion Cyfalaf ar eiddo yn y DU", "yn gorffen gyda 00290", None, None, "cy"),
      (CapitalGainsTax, "Treth Enillion Cyfalaf ar eiddo yn y DU", "yn gorffen gyda 00290", commission, Some("13.57"), "cy"),

      (VatC2c, "Import VAT", "ending with D5E6F", None, None, "en"),
      (VatC2c, "Import VAT", "ending with D5E6F", commission, Some("13.57"), "en"),
      (VatC2c, "TAW fewnforio", "yn gorffen gyda D5E6F", None, None, "cy"),
      (VatC2c, "TAW fewnforio", "yn gorffen gyda D5E6F", commission, Some("13.57"), "cy"),

      (PfVatC2c, "Import VAT", "ending with D5E6F", None, None, "en"),
      (PfVatC2c, "Import VAT", "ending with D5E6F", commission, Some("13.57"), "en"),
      (PfVatC2c, "TAW fewnforio", "yn gorffen gyda D5E6F", None, None, "cy"),
      (PfVatC2c, "TAW fewnforio", "yn gorffen gyda D5E6F", commission, Some("13.57"), "cy"),

      (WcSimpleAssessment, "Simple Assessment", "ending with 89012", None, None, "en"),
      (WcSimpleAssessment, "Simple Assessment", "ending with 89012", commission, Some("13.57"), "en"),
      (WcSimpleAssessment, "Asesiad Syml", "yn gorffen gyda 89012", None, None, "cy"),
      (WcSimpleAssessment, "Asesiad Syml", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (WcXref, "Other taxes, penalties and enquiry settlements", "ending with 89012", None, None, "en"),
      (WcXref, "Other taxes, penalties and enquiry settlements", "ending with 89012", commission, Some("13.57"), "en"),
      (WcXref, "Trethi, cosbau a setliadau ymholiadau eraill", "yn gorffen gyda 89012", None, None, "cy"),
      (WcXref, "Trethi, cosbau a setliadau ymholiadau eraill", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (WcEpayeLpp, "Employers’ PAYE late payment penalty", "ending with 89012", None, None, "en"),
      (WcEpayeLpp, "Employers’ PAYE late payment penalty", "ending with 89012", commission, Some("13.57"), "en"),
      (WcEpayeLpp, "Cosb y Cyflogwr am dalu TWE yn hwyr", "yn gorffen gyda 89012", None, None, "cy"),
      (WcEpayeLpp, "Cosb y Cyflogwr am dalu TWE yn hwyr", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (WcEpayeNi, "Employers’ PAYE and National Insurance", "ending with 02501", None, None, "en"),
      (WcEpayeNi, "Employers’ PAYE and National Insurance", "ending with 02501", commission, Some("13.57"), "en"),
      (WcEpayeNi, "TWE ac Yswiriant Gwladol y Cyflogwr", "yn gorffen gyda 02501", None, None, "cy"),
      (WcEpayeNi, "TWE ac Yswiriant Gwladol y Cyflogwr", "yn gorffen gyda 02501", commission, Some("13.57"), "cy"),

      (WcEpayeLateCis, "Construction Industry Scheme (CIS) late filing penalty", "ending with 89012", None, None, "en"),
      (WcEpayeLateCis, "Construction Industry Scheme (CIS) late filing penalty", "ending with 89012", commission, Some("13.57"), "en"),
      (WcEpayeLateCis, "Cynllun y Diwydiant Adeiladu (CIS) - cosb am dalu’n hwyr", "yn gorffen gyda 89012", None, None, "cy"),
      (WcEpayeLateCis, "Cynllun y Diwydiant Adeiladu (CIS) - cosb am dalu’n hwyr", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (WcEpayeSeta, "Employers’ PAYE Settlement Agreement", "ending with 89012", None, None, "en"),
      (WcEpayeSeta, "Employers’ PAYE Settlement Agreement", "ending with 89012", commission, Some("13.57"), "en"),
      (WcEpayeSeta, "Cytundeb Setliad TWE y Cyflogwr", "yn gorffen gyda 89012", None, None, "cy"),
      (WcEpayeSeta, "Cytundeb Setliad TWE y Cyflogwr", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (WcClass1aNi, "Employers’ Class 1A National Insurance", "ending with 02713", None, None, "en"),
      (WcClass1aNi, "Employers’ Class 1A National Insurance", "ending with 02713", commission, Some("13.57"), "en"),
      (WcClass1aNi, "Yswiriant Gwladol Dosbarth 1A y Cyflogwr", "yn gorffen gyda 02713", None, None, "cy"),
      (WcClass1aNi, "Yswiriant Gwladol Dosbarth 1A y Cyflogwr", "yn gorffen gyda 02713", commission, Some("13.57"), "cy"),

      (PfChildBenefitRepayments, "Repay Child Benefit overpayments", "ending with 89123", None, None, "en"),
      (PfChildBenefitRepayments, "Repay Child Benefit overpayments", "ending with 89123", commission, Some("13.57"), "en"),
      (PfChildBenefitRepayments, "Ad-dalu gordaliadau Budd-dal Plant", "yn gorffen gyda 89123", None, None, "cy"),
      (PfChildBenefitRepayments, "Ad-dalu gordaliadau Budd-dal Plant", "yn gorffen gyda 89123", commission, Some("13.57"), "cy"),

      (WcChildBenefitRepayments, "Repay Child Benefit overpayments", "ending with 89123", None, None, "en"),
      (WcChildBenefitRepayments, "Repay Child Benefit overpayments", "ending with 89123", commission, Some("13.57"), "en"),
      (WcChildBenefitRepayments, "Ad-dalu gordaliadau Budd-dal Plant", "yn gorffen gyda 89123", None, None, "cy"),
      (WcChildBenefitRepayments, "Ad-dalu gordaliadau Budd-dal Plant", "yn gorffen gyda 89123", commission, Some("13.57"), "cy"),

      (BtaSdil, "Soft Drinks Industry Levy", "ending with 90123", None, None, "en"),
      (BtaSdil, "Soft Drinks Industry Levy", "ending with 90123", commission, Some("13.57"), "en"),
      (BtaSdil, "Ardoll y Diwydiant Diodydd Ysgafn", "yn gorffen gyda 90123", None, None, "cy"),
      (BtaSdil, "Ardoll y Diwydiant Diodydd Ysgafn", "yn gorffen gyda 90123", commission, Some("13.57"), "cy"),

      (PfSdil, "Soft Drinks Industry Levy", "ending with 90123", None, None, "en"),
      (PfSdil, "Soft Drinks Industry Levy", "ending with 90123", commission, Some("13.57"), "en"),
      (PfSdil, "Ardoll y Diwydiant Diodydd Ysgafn", "yn gorffen gyda 90123", None, None, "cy"),
      (PfSdil, "Ardoll y Diwydiant Diodydd Ysgafn", "yn gorffen gyda 90123", commission, Some("13.57"), "cy"),

      (PfP800, "P800", "ending with 02027", None, None, "en"),
      (PfP800, "P800", "ending with 02027", commission, Some("13.57"), "en"),
      (PfP800, "P800", "yn gorffen gyda 02027", None, None, "cy"),
      (PfP800, "P800", "yn gorffen gyda 02027", commission, Some("13.57"), "cy"),

      (PtaP800, "P800", "ending with 02027", None, None, "en"),
      (PtaP800, "P800", "ending with 02027", commission, Some("13.57"), "en"),
      (PtaP800, "P800", "yn gorffen gyda 02027", None, None, "cy"),
      (PtaP800, "P800", "yn gorffen gyda 02027", commission, Some("13.57"), "cy"),

      (PfSimpleAssessment, "Simple Assessment", "ending with 89012", None, None, "en"),
      (PfSimpleAssessment, "Simple Assessment", "ending with 89012", commission, Some("13.57"), "en"),
      (PfSimpleAssessment, "Asesiad Syml", "yn gorffen gyda 89012", None, None, "cy"),
      (PfSimpleAssessment, "Asesiad Syml", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (PtaSimpleAssessment, "Simple Assessment", "ending with 22027", None, None, "en"),
      (PtaSimpleAssessment, "Simple Assessment", "ending with 22027", commission, Some("13.57"), "en"),
      (PtaSimpleAssessment, "Asesiad Syml", "yn gorffen gyda 22027", None, None, "cy"),
      (PtaSimpleAssessment, "Asesiad Syml", "yn gorffen gyda 22027", commission, Some("13.57"), "cy"),

      (PfJobRetentionScheme, "Pay Coronavirus Job Retention Scheme grants back", "ending with 78901", None, None, "en"),
      (PfJobRetentionScheme, "Pay Coronavirus Job Retention Scheme grants back", "ending with 78901", commission, Some("13.57"), "en"),
      (PfJobRetentionScheme, "Talu grantiau’r Cynllun Cadw Swyddi yn sgil Coronafeirws yn ôl", "yn gorffen gyda 78901", None, None, "cy"),
      (PfJobRetentionScheme, "Talu grantiau’r Cynllun Cadw Swyddi yn sgil Coronafeirws yn ôl", "yn gorffen gyda 78901", commission, Some("13.57"), "cy"),

      (JrsJobRetentionScheme, "Pay Coronavirus Job Retention Scheme grants back", "ending with 78901", None, None, "en"),
      (JrsJobRetentionScheme, "Pay Coronavirus Job Retention Scheme grants back", "ending with 78901", commission, Some("13.57"), "en"),
      (JrsJobRetentionScheme, "Talu grantiau’r Cynllun Cadw Swyddi yn sgil Coronafeirws yn ôl", "yn gorffen gyda 78901", None, None, "cy"),
      (JrsJobRetentionScheme, "Talu grantiau’r Cynllun Cadw Swyddi yn sgil Coronafeirws yn ôl", "yn gorffen gyda 78901", commission, Some("13.57"), "cy"),

      (PfCds, "CDS", "ending with 67890", None, None, "en"),
      (PfCds, "CDS", "ending with 67890", commission, Some("13.57"), "en"),

      (NiEuVatOss, "VAT One Stop Shop Union scheme", "ending with 1Q424", None, None, "en"),
      (NiEuVatOss, "VAT One Stop Shop Union scheme", "ending with 1Q424", commission, Some("13.57"), "en"),

      (PfNiEuVatOss, "VAT One Stop Shop Union scheme", "ending with 1Q424", None, None, "en"),
      (PfNiEuVatOss, "VAT One Stop Shop Union scheme", "ending with 1Q424", commission, Some("13.57"), "en"),

      (NiEuVatIoss, "VAT Import One Stop Shop", "ending with M0624", None, None, "en"),
      (NiEuVatIoss, "VAT Import One Stop Shop", "ending with M0624", commission, Some("13.57"), "en"),

      (PfNiEuVatIoss, "VAT Import One Stop Shop", "ending with M0624", None, None, "en"),
      (PfNiEuVatIoss, "VAT Import One Stop Shop", "ending with M0624", commission, Some("13.57"), "en"),

      (AppSa, "Self Assessment", "ending with 7890K", None, None, "en"),
      (AppSa, "Self Assessment", "ending with 7890K", commission, Some("13.57"), "en"),
      (AppSa, "Hunanasesiad", "yn gorffen gyda 7890K", None, None, "cy"),
      (AppSa, "Hunanasesiad", "yn gorffen gyda 7890K", commission, Some("13.57"), "cy"),

      (AppSimpleAssessment, "Simple Assessment", "ending with 22023", None, None, "en"),
      (AppSimpleAssessment, "Simple Assessment", "ending with 22023", commission, Some("13.57"), "en"),
      (AppSimpleAssessment, "Asesiad Syml", "yn gorffen gyda 22023", None, None, "cy"),
      (AppSimpleAssessment, "Asesiad Syml", "yn gorffen gyda 22023", commission, Some("13.57"), "cy"),

      //Mib/BcPngr not needed to be tested, we don't send emails for them. I think the email is handled by the corresponding tenant services.

      (PfTpes, "Other taxes, penalties and enquiry settlements", "ending with 89012", None, None, "en"),
      (PfTpes, "Other taxes, penalties and enquiry settlements", "ending with 89012", commission, Some("13.57"), "en"),
      (PfTpes, "Trethi, cosbau a setliadau ymholiadau eraill", "yn gorffen gyda 89012", None, None, "cy"),
      (PfTpes, "Trethi, cosbau a setliadau ymholiadau eraill", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (PfOther, "Other taxes, penalties and enquiry settlements", "ending with 89012", None, None, "en"),
      (PfOther, "Other taxes, penalties and enquiry settlements", "ending with 89012", commission, Some("13.57"), "en"),
      (PfOther, "Trethi, cosbau a setliadau ymholiadau eraill", "yn gorffen gyda 89012", None, None, "cy"),
      (PfOther, "Trethi, cosbau a setliadau ymholiadau eraill", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (PfMgd, "Machine Games Duty", "ending with 89012", None, None, "en"),
      (PfMgd, "Machine Games Duty", "ending with 89012", commission, Some("13.57"), "en"),
      (PfMgd, "Toll Peiriannau Hapchwarae", "yn gorffen gyda 89012", None, None, "cy"),
      (PfMgd, "Toll Peiriannau Hapchwarae", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (PfGbPbRgDuty, "General Betting, Pool Betting or Remote Gaming Duty", "ending with 89012", None, None, "en"),
      (PfGbPbRgDuty, "General Betting, Pool Betting or Remote Gaming Duty", "ending with 89012", commission, Some("13.57"), "en"),
      (PfGbPbRgDuty, "Toll Betio Cyffredinol, Toll Cronfa Fetio neu Doll Hapchwarae o Bell", "yn gorffen gyda 89012", None, None, "cy"),
      (PfGbPbRgDuty, "Toll Betio Cyffredinol, Toll Cronfa Fetio neu Doll Hapchwarae o Bell", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (PfTrust, "Trust Registration Service penalty charge", "ending with 89012", None, None, "en"),
      (PfTrust, "Trust Registration Service penalty charge", "ending with 89012", commission, Some("13.57"), "en"),
      (PfTrust, "Tâl cosb y Gwasanaeth Cofrestru Ymddiriedolaethau", "yn gorffen gyda 89012", None, None, "cy"),
      (PfTrust, "Tâl cosb y Gwasanaeth Cofrestru Ymddiriedolaethau", "yn gorffen gyda 89012", commission, Some("13.57"), "cy"),

      (PfPsAdmin, "Pension scheme tax charges", "ending with 89012", None, None, "en"),
      (PfPsAdmin, "Pension scheme tax charges", "ending with 89012", commission, Some("13.57"), "en"),
      (PfPsAdmin, "Taliadau treth gynllun pensiwn", "yn gorffen gyda 89012", None, None, "cy"),
      (PfPsAdmin, "Taliadau treth gynllun pensiwn", "yn gorffen gyda 89012", commission, Some("13.57"), "cy")
    )

    forAll(scenarios) { (j, taxType, taxReference, commission, totalPaid, lang) =>
      val cardType = if (commission.isDefined) "credit" else "debit"
      val origin = j.journeyBeforeBeginWebPayment.origin
      val request: FakeRequest[AnyContentAsEmpty.type] = if (lang == "en") fakeRequest else fakeRequestInWelsh
      val journey: Journey[_ >: JsdBounds <: JourneySpecificData] = if (cardType == "credit") j.journeyAfterSucceedCreditWebPayment else j.journeyAfterSucceedDebitWebPayment

      s"when origin is ${origin.entryName}, card type is $cardType in $lang" in {
        val expectedResult: EmailParameters = EmailParameters(
          taxType          = taxType,
          taxReference     = taxReference,
          paymentReference = "Some-transaction-ref",
          amountPaid       = "12.34",
          commission       = commission,
          totalPaid        = totalPaid
        )

        systemUnderTest.buildEmailParameters(journey)(request) shouldBe expectedResult
      }
    }

    implementedOrigins
      .filterNot(_ == Origins.Mib) // no email for this Origin
      .filterNot(_ == Origins.BcPngr) // no email for this Origin
      .foreach { origin =>
        s"for journey with origin ${origin.entryName}, test scenario should exist" in {
          scenarios.exists { scenario =>
            scenario._1.journeyBeforeBeginWebPayment.origin == origin
          } shouldBe true withClue s"Test scenario missing for origin: ${origin.entryName}"
        }
      }

    "should have a messages populated for emailTaxTypeMessageKey for all origins" in {
      val messages: MessagesApi = app.injector.instanceOf[MessagesApi]
      val implementedOrigins: Seq[Origin] = TestHelpers.implementedOrigins
      implementedOrigins
        .filterNot(_ == Origins.Mib) // no email for this Origin
        .filterNot(_ == Origins.BcPngr) // no email for this Origin
        .foreach { origin =>
          val msgKey = origin.lift.emailTaxTypeMessageKey

          msgKey.isEmpty shouldBe false withClue s"email.tax-name message key missing for origin: ${origin.entryName}"
          //Check if the message is defined in either messages file, Doesn't matter which one
          //doesn't seem to care what the language is, if it exists in one of the language file, it'll return true
          messages.isDefinedAt(msgKey)(Lang("en")) shouldBe true withClue s"email.tax-name message key missing for origin: ${origin.entryName}"
          //Check if the message is different in both languages, if they are the same the message is not in both files
          // for PfP800 and PtaP800, the english can be the same as welsh...
          // for PfCds there is no welsh as it isn't supported...
          if (origin =!= Origins.PfP800 && origin =!= Origins.PtaP800 && origin =!= Origins.PfCds && origin =!= Origins.NiEuVatOss && origin =!= Origins.PfNiEuVatOss && origin =!= Origins.NiEuVatIoss && origin =!= Origins.PfNiEuVatIoss) {
            messages.preferred(Seq(Lang("en")))(msgKey) should not be messages.preferred(Seq(Lang("cy")))(msgKey)
          }
        }
    }
  }

  "obfuscateReference" - {
    "should obfuscate the tax reference, appending a string and taking the right most characters" - {
      "in English" in {
        val result = systemUnderTest.obfuscateReference("123456789K")(fakeRequest)
        result shouldBe "ending with 6789K"
      }
      "in Welsh" in {
        val result = systemUnderTest.obfuscateReference("123456789K")(fakeRequestInWelsh)
        result shouldBe "yn gorffen gyda 6789K"
      }
    }
  }

  "hasCardFees" - {
    "should return true when AmountInPence is Some and greater than 0" in {
      val result = systemUnderTest.hasCardFees(Some(AmountInPence(1)))
      result shouldBe true
    }

    "should return false when AmountInPence is None" in {
      val result = systemUnderTest.hasCardFees(None)
      result shouldBe false
    }

    "should return false when AmountInPence is Some but it's 0" in {
      val result = systemUnderTest.hasCardFees(Some(AmountInPence(0)))
      result shouldBe false
    }
  }

  "EmailService" - {
    "buildEmailRequest" - {
      "should return an EmailRequest" - {
        "in english when PLAY_LANG cookie in request is en" in {
          val expectedResult: EmailRequest = EmailRequest(
            to         = List(EmailAddress("joe_bloggs@gmail.com")),
            templateId = "payment_successful",
            parameters = EmailParameters(
              taxType          = "Self Assessment",
              taxReference     = "ending with 7895K",
              paymentReference = "Some-transaction-ref",
              amountPaid       = "12.34",
              commission       = None,
              totalPaid        = None
            ),
            force      = false
          )

          val result = systemUnderTest.buildEmailRequest(
            TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment,
            emailAddress = EmailAddress("joe_bloggs@gmail.com"),
            isEnglish    = true
          )(fakeRequest.withEmailInSession(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment._id, EmailAddress("joe_bloggs@gmail.com")))
          result shouldBe expectedResult
        }

        "request in welsh when PLAY_LANG cookie in request is cy" in {
          val expectedResult: EmailRequest = EmailRequest(
            to         = List(EmailAddress("joe_bloggs@gmail.com")),
            templateId = "payment_successful_cy",
            parameters = EmailParameters(
              taxType          = "Hunanasesiad",
              taxReference     = "yn gorffen gyda 7895K",
              paymentReference = "Some-transaction-ref",
              amountPaid       = "12.34",
              commission       = None,
              totalPaid        = None
            ),
            force      = false
          )

          val result = systemUnderTest.buildEmailRequest(
            TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment,
            emailAddress = EmailAddress("joe_bloggs@gmail.com"),
            isEnglish    = false
          )(fakeRequestInWelsh.withEmailInSession(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment._id, EmailAddress("joe_bloggs@gmail.com")))
          result shouldBe expectedResult
        }

        "with total paid and commission in when commission is greater than 0" in {
          val expectedResult: EmailRequest = EmailRequest(
            to         = List(EmailAddress("joe_bloggs@gmail.com")),
            templateId = "payment_successful",
            parameters = EmailParameters(
              taxType          = "Self Assessment",
              taxReference     = "ending with 7895K",
              paymentReference = "Some-transaction-ref",
              amountPaid       = "12.34",
              commission       = Some("1.23"),
              totalPaid        = Some("13.57")
            ),
            force      = false
          )

          val result = systemUnderTest.buildEmailRequest(
            TestJourneys.PfSa.journeyAfterSucceedCreditWebPayment,
            emailAddress = EmailAddress("joe_bloggs@gmail.com"),
            isEnglish    = true
          )(fakeRequest.withEmailInSession(TestJourneys.PfSa.journeyAfterSucceedDebitWebPayment._id, EmailAddress("joe_bloggs@gmail.com")))
          result shouldBe expectedResult
        }
      }
    }

    "sendEmail" - {

      val jsonBody = Json.parse(

        """
        {
          "to" : [ "blah@blah.com" ],
          "templateId" : "payment_successful",
          "parameters" : {
            "taxType" : "Self Assessment",
            "taxReference" : "ending with 7895K",
            "paymentReference" : "Some-transaction-ref",
            "amountPaid" : "12.34",
            "commission" : "1.23",
            "totalPaid" : "13.57"
          },
          "force" : false
        }
        """
      )

      "should send an email successfully" in {
        val result = systemUnderTest.sendEmail(
          journey      = TestJourneys.PfSa.journeyAfterSucceedCreditWebPayment,
          emailAddress = EmailAddress("blah@blah.com"),
          isEnglish    = true
        )(HeaderCarrier(), fakeRequest)
        whenReady(result)(_ => succeed)
        EmailStub.verifyEmailWasSent(jsonBody)
      }
    }

  }

}
