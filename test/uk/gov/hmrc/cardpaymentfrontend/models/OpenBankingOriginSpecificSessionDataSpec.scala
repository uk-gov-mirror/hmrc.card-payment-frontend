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

package uk.gov.hmrc.cardpaymentfrontend.models

import org.scalatest.AppendedClues.convertToClueful
import org.scalatest.Assertion
import payapi.corcommon.model.cgt.CgtAccountReference
import payapi.corcommon.model.taxes.ad.{AlcoholDutyChargeReference, AlcoholDutyReference}
import payapi.corcommon.model.taxes.amls.AmlsPaymentReference
import payapi.corcommon.model.taxes.cds.CdsRef
import payapi.corcommon.model.taxes.ct.{CtChargeTypes, CtPeriod, CtUtr}
import payapi.corcommon.model.taxes.epaye._
import payapi.corcommon.model.taxes.other._
import payapi.corcommon.model.taxes.p302.{P302ChargeRef, P302Ref}
import payapi.corcommon.model.taxes.ioss.Ioss
import payapi.corcommon.model.taxes.p800.P800Ref
import payapi.corcommon.model.taxes.ppt.PptReference
import payapi.corcommon.model.taxes.sa.SaUtr
import payapi.corcommon.model.taxes.sdlt.Utrn
import payapi.corcommon.model.taxes.vat.{CalendarPeriod, VatChargeReference, Vrn}
import payapi.corcommon.model.taxes.trusts.TrustReference
import payapi.corcommon.model.taxes.vatc2c.VatC2cReference
import payapi.corcommon.model.times.period.CalendarQuarter.OctoberToDecember
import payapi.corcommon.model.times.period.TaxQuarter.AprilJuly
import payapi.corcommon.model.times.period.{CalendarQuarterlyPeriod, TaxMonth, TaxYear}
import payapi.corcommon.model.webchat.WcEpayeNiReference
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.cardpaymentfrontend.models.extendedorigins._
import uk.gov.hmrc.cardpaymentfrontend.models.openbanking._
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys
import uk.gov.hmrc.cardpaymentfrontend.testsupport.{TestHelpers, UnitSpec}

class OpenBankingOriginSpecificSessionDataSpec extends UnitSpec {

  private def testOsd(
      originSpecificSessionData: Option[OriginSpecificSessionData],
      expectedOsd:               OriginSpecificSessionData,
      expectedReference:         String,
      expectedSearchTag:         String
  ): Assertion = {
    originSpecificSessionData shouldBe Some(expectedOsd) withClue "osd didn't match"
    originSpecificSessionData.map(_.paymentReference.value) shouldBe Some(expectedReference) withClue "wrong paymentReference"
    originSpecificSessionData.map(_.searchTag.value) shouldBe Some(expectedSearchTag) withClue "wrong searchTag"
  }

  private def roundTripJsonTest(maybeThing: Option[OriginSpecificSessionData], expectedJson: JsValue): Assertion = {
    maybeThing.fold(throw new RuntimeException("expected some but was none")) { thing =>
      Json.toJson(thing) shouldBe expectedJson withClue "failed to write to json"
      Json.fromJson[OriginSpecificSessionData](expectedJson).asEither shouldBe Right(thing) withClue "failed to read from json"
    }
  }

  "Each extended origin should have a way to generate OriginSpecificSessionData for open banking" - {

    "PfSaSessionData" in {
      val testJson = Json.parse("""{"saUtr":"1234567895","origin":"PfSa"}""")
      val osd = ExtendedPfSa.openBankingOriginSpecificSessionData(TestJourneys.PfSa.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfSaSessionData(SaUtr("1234567895"), None), "1234567895K", "1234567895")
      roundTripJsonTest(osd, testJson)
    }

    "BtaSa" in {
      val testJson = Json.parse("""{"saUtr":"1234567895","origin":"BtaSa"}""")
      val osd = ExtendedBtaSa.openBankingOriginSpecificSessionData(TestJourneys.BtaSa.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, BtaSaSessionData(SaUtr("1234567895"), None), "1234567895K", "1234567895")
      roundTripJsonTest(osd, testJson)
    }

    "PtaSa" in {
      val testJson = Json.parse("""{"saUtr":"1234567895","origin":"PtaSa"}""")
      val osd = ExtendedPtaSa.openBankingOriginSpecificSessionData(TestJourneys.PtaSa.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PtaSaSessionData(SaUtr("1234567895"), None), "1234567895K", "1234567895")
      roundTripJsonTest(osd, testJson)
    }

    "ItSa" in {
      val testJson = Json.parse("""{"saUtr":"1234567895","origin":"ItSa"}""")
      val osd = ExtendedItSa.openBankingOriginSpecificSessionData(TestJourneys.ItSa.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, ItSaSessionData(SaUtr("1234567895"), None), "1234567895K", "1234567895")
      roundTripJsonTest(osd, testJson)
    }

    "WcSa" in {
      val testJson = Json.parse("""{"saUtr":"1234567895","origin":"WcSa"}""")
      val osd = ExtendedWcSa.openBankingOriginSpecificSessionData(TestJourneys.WcSa.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, WcSaSessionData(SaUtr("1234567895"), None), "1234567895K", "1234567895")
      roundTripJsonTest(osd, testJson)
    }

    "PfAlcoholDuty" in {
      val testJson = Json.parse("""{"alcoholDutyReference":"XMADP0123456789","origin":"PfAlcoholDuty"}""")
      val osd = ExtendedPfAlcoholDuty.openBankingOriginSpecificSessionData(TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfAlcoholDutySessionData(AlcoholDutyReference("XMADP0123456789"), None), "XMADP0123456789", "XMADP0123456789")
      roundTripJsonTest(osd, testJson)
    }

    "AlcoholDuty" in {
      val testJson = Json.parse("""{"alcoholDutyReference":"XMADP0123456789","alcoholDutyChargeReference":"XE1234567890123","origin":"AlcoholDuty"}""")
      val osd = ExtendedAlcoholDuty.openBankingOriginSpecificSessionData(TestJourneys.AlcoholDuty.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, AlcoholDutySessionData(AlcoholDutyReference("XMADP0123456789"), Some(AlcoholDutyChargeReference("XE1234567890123")), None), "XE1234567890123", "XMADP0123456789")
      roundTripJsonTest(osd, testJson)
    }

    "PfCt" in {
      val testJson = Json.parse("""{"utr":"1097172564","ctPeriod":1,"ctChargeType":"A","origin":"PfCt"}""")
      val osd = ExtendedPfCt.openBankingOriginSpecificSessionData(TestJourneys.PfCt.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfCtSessionData(CtUtr("1097172564"), CtPeriod(1), CtChargeTypes.A, None), "1097172564A00101A", "1097172564")
      roundTripJsonTest(osd, testJson)
    }

    "BtaCt" in {
      val testJson = Json.parse("""{"utr":"1097172564","ctPeriod":1,"ctChargeType":"A","origin":"BtaCt"}""")
      val osd = ExtendedBtaCt.openBankingOriginSpecificSessionData(TestJourneys.BtaCt.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, BtaCtSessionData(CtUtr("1097172564"), CtPeriod(1), CtChargeTypes.A, None), "1097172564A00101A", "1097172564")
      roundTripJsonTest(osd, testJson)
    }

    "WcCt" in {
      val testJson = Json.parse("""{"utr":"1097172564","ctPeriod":1,"ctChargeType":"A","origin":"WcCt"}""")
      val osd = ExtendedWcCt.openBankingOriginSpecificSessionData(TestJourneys.WcCt.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, WcCtSessionData(CtUtr("1097172564"), CtPeriod(1), CtChargeTypes.A, None), "1097172564A00101A", "1097172564")
      roundTripJsonTest(osd, testJson)
    }

    "PfEpayeNi" in {
      val testJson = Json.parse("""{"accountsOfficeReference":"123PH45678900","period":{"taxQuarter":"AprilJuly","taxYear":2025},"origin":"PfEpayeNi"}""")
      val osd = ExtendedPfEpayeNi.openBankingOriginSpecificSessionData(TestJourneys.PfEpayeNi.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfEpayeNiSessionData(AccountsOfficeReference("123PH45678900"), QuarterlyEpayeTaxPeriod(AprilJuly, TaxYear(2025)), None), "123PH456789002503", "123PH45678900")
      roundTripJsonTest(osd, testJson)
    }

    "PfEpayeLpp" in {
      val testJson = Json.parse("""{"payeInterestXRef":"XE123456789012","origin":"PfEpayeLpp"}""")
      val osd = ExtendedPfEpayeLpp.openBankingOriginSpecificSessionData(TestJourneys.PfEpayeLpp.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfEpayeLppSessionData(XRef("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PfEpayeLateCis" in {
      val testJson = Json.parse("""{"payeInterestXRef":"XE123456789012","origin":"PfEpayeLateCis"}""")
      val osd = ExtendedPfEpayeLateCis.openBankingOriginSpecificSessionData(TestJourneys.PfEpayeLateCis.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfEpayeLateCisSessionData(XRef14Char("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PfEpayeP11d" in {
      val testJson = Json.parse("""{"accountsOfficeReference":"123PH45678900","period":{"taxYear":2025},"origin":"PfEpayeP11d"}""")
      val osd = ExtendedPfEpayeP11d.openBankingOriginSpecificSessionData(TestJourneys.PfEpayeP11d.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfEpayeP11dSessionData(AccountsOfficeReference("123PH45678900"), YearlyEpayeTaxPeriod(TaxYear(2025)), None), "123PH456789002513", "123PH45678900")
      roundTripJsonTest(osd, testJson)
    }

    "PfEpayeSeta" in {
      val testJson = Json.parse("""{"psaNumber":"XA123456789012","origin":"PfEpayeSeta"}""")
      val osd = ExtendedPfEpayeSeta.openBankingOriginSpecificSessionData(TestJourneys.PfEpayeSeta.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfEpayeSetaSessionData(PsaNumber("XA123456789012"), None), "XA123456789012", "XA123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PfVat with vrn" in {
      val testJson = Json.parse("""{"vrn":"999964805","origin":"PfVat"}""")
      val osd = ExtendedPfVat.openBankingOriginSpecificSessionData(TestJourneys.PfVat.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfVatSessionData(Some(Vrn("999964805")), None, None), "999964805", "999964805")
      roundTripJsonTest(osd, testJson)
    }

    "PfVat with charge reference" in {
      val testJson = Json.parse("""{"chargeRef":"XE123456789012","origin":"PfVat"}""")
      val osd = ExtendedPfVat.openBankingOriginSpecificSessionData(TestJourneys.PfVatWithChargeReference.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfVatSessionData(None, Some(XRef14Char("XE123456789012")), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "WcVat with vrn" in {
      val testJson = Json.parse("""{"vrn":"999964805","origin":"WcVat"}""")
      val osd = ExtendedWcVat.openBankingOriginSpecificSessionData(TestJourneys.WcVat.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, WcVatSessionData(Some(Vrn("999964805")), None, None), "999964805", "999964805")
      roundTripJsonTest(osd, testJson)
    }

    "WcVat with charge reference" in {
      val testJson = Json.parse("""{"chargeRef":"XE123456789012","origin":"WcVat"}""")
      val osd = ExtendedWcVat.openBankingOriginSpecificSessionData(TestJourneys.WcVatWithChargeReference.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, WcVatSessionData(None, Some(XRef14Char("XE123456789012")), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "BtaVat" in {
      val testJson = Json.parse("""{"vrn":"999964805","origin":"BtaVat"}""")
      val osd = ExtendedBtaVat.openBankingOriginSpecificSessionData(TestJourneys.BtaVat.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, BtaVatSessionData(Vrn("999964805"), None), "999964805", "999964805")
      roundTripJsonTest(osd, testJson)
    }

    "VcVatReturn" in {
      val testJson = Json.parse("""{"vrn":"999964805","origin":"VcVatReturn"}""")
      val osd = ExtendedVcVatReturn.openBankingOriginSpecificSessionData(TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, VcVatReturnSessionData(Vrn("999964805"), None), "999964805", "999964805")
      roundTripJsonTest(osd, testJson)
    }

    "VcVatOther" in {
      val testJson = Json.parse("""{"vrn":"999964805","vatChargeReference":"999964805","origin":"VcVatOther"}""")
      val osd = ExtendedVcVatOther.openBankingOriginSpecificSessionData(TestJourneys.VcVatOther.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, VcVatOtherSessionData(Vrn("999964805"), VatChargeReference("999964805"), None), "999964805", "999964805")
      roundTripJsonTest(osd, testJson)
    }

    "Ppt" in {
      val testJson = Json.parse("""{"pptReference":"XAPPT0000012345","origin":"Ppt"}""")
      val osd = ExtendedPpt.openBankingOriginSpecificSessionData(TestJourneys.Ppt.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PptSessionData(PptReference("XAPPT0000012345"), None), "XAPPT0000012345", "XAPPT0000012345")
      roundTripJsonTest(osd, testJson)
    }

    "PfPpt" in {
      val testJson = Json.parse("""{"pptReference":"XAPPT0000012345","origin":"PfPpt"}""")
      val osd = ExtendedPfPpt.openBankingOriginSpecificSessionData(TestJourneys.PfPpt.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfPptSessionData(PptReference("XAPPT0000012345"), None), "XAPPT0000012345", "XAPPT0000012345")
      roundTripJsonTest(osd, testJson)
    }

    "BtaEpayeBill" in {
      val testJson = Json.parse("""{"accountsOfficeReference":"123PH45678900","period":{"taxMonth":"MayJune","taxYear":2027},"origin":"BtaEpayeBill"}""")
      val osd = ExtendedBtaEpayeBill.openBankingOriginSpecificSessionData(TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, BtaEpayeBillSessionData(AccountsOfficeReference("123PH45678900"), MonthlyEpayeTaxPeriod(TaxMonth.MayJune, TaxYear(2027)), None), "123PH456789002702", "123PH45678900")
      roundTripJsonTest(osd, testJson)
    }

    "BtaEpayePenalty" in {
      val testJson = Json.parse("""{"epayePenaltyReference":"123PH45678900","origin":"BtaEpayePenalty"}""")
      val osd = ExtendedBtaEpayePenalty.openBankingOriginSpecificSessionData(TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, BtaEpayePenaltySessionData(EpayePenaltyReference("123PH45678900"), None), "123PH45678900", "123PH45678900")
      roundTripJsonTest(osd, testJson)
    }

    "BtaEpayeInterest" in {
      val testJson = Json.parse("""{"payeInterestXRef":"XE123456789012","origin":"BtaEpayeInterest"}""")
      val osd = ExtendedBtaEpayeInterest.openBankingOriginSpecificSessionData(TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, BtaEpayeInterestSessionData(XRef("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "BtaEpayeGeneral" in {
      val testJson = Json.parse("""{"accountsOfficeReference":"123PH45678900","period":{"taxMonth":"MayJune","taxYear":2027},"origin":"BtaEpayeGeneral"}""")
      val osd = ExtendedBtaEpayeGeneral.openBankingOriginSpecificSessionData(TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, BtaEpayeGeneralSessionData(AccountsOfficeReference("123PH45678900"), MonthlyEpayeTaxPeriod(TaxMonth.MayJune, TaxYear(2027)), None), "123PH456789002702", "123PH45678900")
      roundTripJsonTest(osd, testJson)
    }

    "BtaClass1aNi" in {
      val testJson = Json.parse("""{"accountsOfficeReference":"123PH45678900","period":{"taxYear":2027},"origin":"BtaClass1aNi"}""")
      val osd = ExtendedBtaClass1aNi.openBankingOriginSpecificSessionData(TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, BtaClass1aNiSessionData(AccountsOfficeReference("123PH45678900"), YearlyEpayeTaxPeriod(TaxYear(2027)), None), "123PH456789002713", "123PH45678900")
      roundTripJsonTest(osd, testJson)
    }

    "Amls" in {
      val testJson = Json.parse("""{"amlsPaymentReference":"XE123456789012","origin":"Amls"}""")
      val osd = ExtendedAmls.openBankingOriginSpecificSessionData(TestJourneys.Amls.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, AmlsSessionData(AmlsPaymentReference("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PfAmls" in {
      val testJson = Json.parse("""{"amlsPaymentReference":"XE123456789012","origin":"PfAmls"}""")
      val osd = ExtendedPfAmls.openBankingOriginSpecificSessionData(TestJourneys.PfAmls.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfAmlsSessionData(AmlsPaymentReference("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PfSdlt" in {
      val testJson = Json.parse("""{"utrn":"123456789MA","origin":"PfSdlt"}""")
      val osd = ExtendedPfSdlt.openBankingOriginSpecificSessionData(TestJourneys.PfSdlt.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfSdltSessionData(Utrn("123456789MA")), "123456789MA", "123456789MA")
      roundTripJsonTest(osd, testJson)
    }

    "CapitalGainsTax" in {
      val testJson = Json.parse("""{"cgtReference":"XVCGTP001000290","origin":"CapitalGainsTax"}""")
      val osd = ExtendedCapitalGainsTax.openBankingOriginSpecificSessionData(TestJourneys.CapitalGainsTax.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, CapitalGainsTaxSessionData(CgtAccountReference("XVCGTP001000290")), "XVCGTP001000290", "XVCGTP001000290")
      roundTripJsonTest(osd, testJson)
    }

    "EconomicCrimeLevy" in {
      val testJson = Json.parse("""{"economicCrimeLevyReturnNumber":"XE123456789012","origin":"EconomicCrimeLevy"}""")
      val osd = ExtendedEconomicCrimeLevy.openBankingOriginSpecificSessionData(TestJourneys.EconomicCrimeLevy.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, EconomicCrimeLevySessionData(EconomicCrimeLevyReturnNumber("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PfEconomicCrimeLevy" in {
      val testJson = Json.parse("""{"economicCrimeLevyReturnNumber":"XE123456789012","origin":"PfEconomicCrimeLevy"}""")
      val osd = ExtendedPfEconomicCrimeLevy.openBankingOriginSpecificSessionData(TestJourneys.PfEconomicCrimeLevy.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfEconomicCrimeLevySessionData(EconomicCrimeLevyReturnNumber("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "VatC2c" in {
      val testJson = Json.parse("""{"vatC2cReference":"XVC1A2B3C4D5E6F","origin":"VatC2c"}""")
      val osd = ExtendedVatC2c.openBankingOriginSpecificSessionData(TestJourneys.VatC2c.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, VatC2cSessionData(VatC2cReference("XVC1A2B3C4D5E6F"), None), "XVC1A2B3C4D5E6F", "XVC1A2B3C4D5E6F")
      roundTripJsonTest(osd, testJson)
    }

    "PfVatC2c" in {
      val testJson = Json.parse("""{"vatC2cReference":"XVC1A2B3C4D5E6F","origin":"PfVatC2c"}""")
      val osd = ExtendedPfVatC2c.openBankingOriginSpecificSessionData(TestJourneys.PfVatC2c.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfVatC2cSessionData(VatC2cReference("XVC1A2B3C4D5E6F"), None), "XVC1A2B3C4D5E6F", "XVC1A2B3C4D5E6F")
      roundTripJsonTest(osd, testJson)
    }

    "WcSimpleAssessment" in {
      val testJson = Json.parse("""{"simpleAssessmentReference":"XE123456789012","origin":"WcSimpleAssessment"}""")
      val osd = ExtendedWcSimpleAssessment.openBankingOriginSpecificSessionData(TestJourneys.WcSimpleAssessment.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, WcSimpleAssessmentSessionData(XRef14Char("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "WcClass1aNi" in {
      val testJson = Json.parse("""{"wcClass1aNiReference":"123PH456789002713","origin":"WcClass1aNi"}""")
      val osd = ExtendedWcClass1aNi.openBankingOriginSpecificSessionData(TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, WcClass1aNiSessionData(WcClass1aNiReference("123PH456789002713"), None), "123PH456789002713", "123PH45678900")
      roundTripJsonTest(osd, testJson)
    }

    "WcXref (which is None, since it doesn't support Open banking)" in {
      val osd = ExtendedWcXref.openBankingOriginSpecificSessionData(TestJourneys.WcXref.journeyBeforeBeginWebPayment.journeySpecificData)
      osd shouldBe None
    }

    "WcEpayeLpp" in {
      val testJson = Json.parse("""{"payeInterestXRef":"XE123456789012","origin":"WcEpayeLpp"}""")
      val osd = ExtendedWcEpayeLpp.openBankingOriginSpecificSessionData(TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, WcEpayeLppSessionData(XRef("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "WcEpayeNi" in {
      val testJson = Json.parse("""{"payePaymentReference":"123PH456789002501","origin":"WcEpayeNi"}""")
      val osd = ExtendedWcEpayeNi.openBankingOriginSpecificSessionData(TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, WcEpayeNiSessionData(WcEpayeNiReference("123PH456789002501"), None), "123PH456789002501", "123PH45678900")
      roundTripJsonTest(osd, testJson)
    }

    "WcEpayeLateCis" in {
      val testJson = Json.parse("""{"chargeReference":"XE123456789012","origin":"WcEpayeLateCis"}""")
      val osd = ExtendedWcEpayeLateCis.openBankingOriginSpecificSessionData(TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, WcEpayeLateCisSessionData(XRef14Char("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "WcEpayeSeta" in {
      val testJson = Json.parse("""{"payeSettlementXRef":"XE123456789012","origin":"WcEpayeSeta"}""")
      val osd = ExtendedWcEpayeSeta.openBankingOriginSpecificSessionData(TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, WcEpayeSetaSessionData(XRef("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PfChildBenefitRepayments" in {
      val testJson = Json.parse("""{"yRef":"YA123456789123","origin":"PfChildBenefitRepayments"}""")
      val osd = ExtendedPfChildBenefitRepayments.openBankingOriginSpecificSessionData(TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfChildBenefitSessionData(YRef("YA123456789123"), None), "YA123456789123", "YA123456789123")
      roundTripJsonTest(osd, testJson)
    }

    "WcChildBenefitRepayments" in {
      val testJson = Json.parse("""{"yRef":"YA123456789123","origin":"WcChildBenefitRepayments"}""")
      val osd = ExtendedWcChildBenefitRepayments.openBankingOriginSpecificSessionData(TestJourneys.WcChildBenefitRepayments.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, WcChildBenefitRepaymentsSessionData(YRef("YA123456789123"), None), "YA123456789123", "YA123456789123")
      roundTripJsonTest(osd, testJson)
    }

    "BtaSdil" in {
      val testJson = Json.parse("""{"xRef":"XE1234567890123","origin":"BtaSdil"}""")
      val osd = ExtendedBtaSdil.openBankingOriginSpecificSessionData(TestJourneys.BtaSdil.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, BtaSdilSessionData(XRef("XE1234567890123"), None), "XE1234567890123", "XE1234567890123")
      roundTripJsonTest(osd, testJson)
    }

    "PfSdil" in {
      val testJson = Json.parse("""{"softDrinksIndustryLevyRef":"XE1234567890123","origin":"PfSdil"}""")
      val osd = ExtendedPfSdil.openBankingOriginSpecificSessionData(TestJourneys.PfSdil.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfSdilSessionData(SoftDrinksIndustryLevyRef("XE1234567890123"), None), "XE1234567890123", "XE1234567890123")
      roundTripJsonTest(osd, testJson)
    }

    "PfP800 (which is None, since it doesn't support Open banking)" in {
      val osd = ExtendedPfP800.openBankingOriginSpecificSessionData(TestJourneys.PfP800.journeyBeforeBeginWebPayment.journeySpecificData)
      osd shouldBe None
    }

    "PtaP800 (which is None, since it doesn't support Open banking)" in {
      val osd = ExtendedPtaP800.openBankingOriginSpecificSessionData(TestJourneys.PtaP800.journeyBeforeBeginWebPayment.journeySpecificData)
      osd shouldBe None
    }

    "PfSimpleAssessment" in {
      val testJson = Json.parse("""{"simpleAssessmentReference":"XE123456789012","origin":"PfSimpleAssessment"}""")
      val osd = ExtendedPfSimpleAssessment.openBankingOriginSpecificSessionData(TestJourneys.PfSimpleAssessment.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfSimpleAssessmentSessionData(XRef14Char("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PtaSimpleAssessment" in {
      val testJson = Json.parse("""{"p302Ref":"MA000003AP3022027","p302ChargeRef":"BC007010065114","origin":"PtaSimpleAssessment"}""")
      val osd = ExtendedPtaSimpleAssessment.openBankingOriginSpecificSessionData(TestJourneys.PtaSimpleAssessment.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PtaSimpleAssessmentSessionData(P302Ref("MA000003AP3022027"), P302ChargeRef("BC007010065114"), None), "BC007010065114", "BC007010065114")
      roundTripJsonTest(osd, testJson)
    }

    "PfJobRetentionScheme" in {
      val testJson = Json.parse("""{"jrsRef":"XJRS12345678901","origin":"PfJobRetentionScheme"}""")
      val osd = ExtendedPfJobRetentionScheme.openBankingOriginSpecificSessionData(TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfJobRetentionSchemeSessionData(JrsRef("XJRS12345678901"), None), "XJRS12345678901", "XJRS12345678901")
      roundTripJsonTest(osd, testJson)
    }

    "JrsJobRetentionScheme" in {
      val testJson = Json.parse("""{"jrsRef":"XJRS12345678901","origin":"JrsJobRetentionScheme"}""")
      val osd = ExtendedJrsJobRetentionScheme.openBankingOriginSpecificSessionData(TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, JrsJobRetentionSchemeSessionData(JrsRef("XJRS12345678901"), None), "XJRS12345678901", "XJRS12345678901")
      roundTripJsonTest(osd, testJson)
    }

    "PfCds" in {
      val testJson = Json.parse("""{"cdsRef":"CDSI191234567890","origin":"PfCds"}""")
      val osd = ExtendedPfCds.openBankingOriginSpecificSessionData(TestJourneys.PfCds.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfCdsSessionData(CdsRef("CDSI191234567890"), None), "CDSI191234567890", "CDSI191234567890")
      roundTripJsonTest(osd, testJson)
    }

    "NiEuVatOss" in {
      val testJson = Json.parse("""{"vrn":"101747641","period":{"quarter":"OctoberToDecember","year":2024},"origin":"NiEuVatOss"}""")
      val osd = ExtendedNiEuVatOss.openBankingOriginSpecificSessionData(TestJourneys.NiEuVatOss.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, NiEuVatOssSessionData(Vrn("101747641"), CalendarQuarterlyPeriod(OctoberToDecember, 2024)), "NI101747641Q424", "101747641")
      roundTripJsonTest(osd, testJson)
    }

    "PfNiEuVatOss" in {
      val testJson = Json.parse("""{"vrn":"101747641","period":{"quarter":"OctoberToDecember","year":2024},"origin":"PfNiEuVatOss"}""")
      val osd = ExtendedPfNiEuVatOss.openBankingOriginSpecificSessionData(TestJourneys.PfNiEuVatOss.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfNiEuVatOssSessionData(Vrn("101747641"), CalendarQuarterlyPeriod(OctoberToDecember, 2024)), "NI101747641Q424", "101747641")
      roundTripJsonTest(osd, testJson)
    }

    "NiEuVatIoss" in {
      val testJson = Json.parse("""{"ioss":"IM1234567890","period":{"month":6,"year":2024},"origin":"NiEuVatIoss"}""")
      val osd = ExtendedNiEuVatIoss.openBankingOriginSpecificSessionData(TestJourneys.NiEuVatIoss.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, NiEuVatIossSessionData(Ioss("IM1234567890"), CalendarPeriod(6, 2024)), "IM1234567890M0624", "IM1234567890")
      roundTripJsonTest(osd, testJson)
    }

    "PfNiEuVatIoss" in {
      val testJson = Json.parse("""{"ioss":"IM1234567890","period":{"month":6,"year":2024},"origin":"PfNiEuVatIoss"}""")
      val osd = ExtendedPfNiEuVatIoss.openBankingOriginSpecificSessionData(TestJourneys.PfNiEuVatIoss.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfNiEuVatIossSessionData(Ioss("IM1234567890"), CalendarPeriod(6, 2024)), "IM1234567890M0624", "IM1234567890")
      roundTripJsonTest(osd, testJson)
    }

    "AppSa" in {
      val testJson = Json.parse("""{"saUtr":"1234567890","origin":"AppSa"}""")
      val osd = ExtendedAppSa.openBankingOriginSpecificSessionData(TestJourneys.AppSa.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, AppSaSessionData(SaUtr("1234567890"), None), "1234567890K", "1234567890")
      roundTripJsonTest(osd, testJson)
    }

    "AppSimpleAssessment" in {
      val testJson = Json.parse("""{"p302Ref":"MA000003AP3022023","origin":"AppSimpleAssessment"}""")
      val osd = ExtendedAppSimpleAssessment.openBankingOriginSpecificSessionData(TestJourneys.AppSimpleAssessment.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, AppSimpleAssessmentSessionData(P800Ref("MA000003AP3022023"), None), "MA000003AP3022023", "MA000003AP3022023")
      roundTripJsonTest(osd, testJson)
    }

    "Mib (which is None, since it doesn't support Open banking)" in {
      val osd = ExtendedMib.openBankingOriginSpecificSessionData(TestJourneys.Mib.journeyBeforeBeginWebPayment.journeySpecificData)
      osd shouldBe None
    }

    "BcPngr (which is None, since it doesn't support Open banking)" in {
      val osd = ExtendedMib.openBankingOriginSpecificSessionData(TestJourneys.BcPngr.journeyBeforeBeginWebPayment.journeySpecificData)
      osd shouldBe None
    }

    "PfTpes" in {
      val testJson = Json.parse("""{"xRef":"XE123456789012","origin":"PfTpes"}""")
      val osd = ExtendedPfTpes.openBankingOriginSpecificSessionData(TestJourneys.PfTpes.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfTpesSessionData(XRef("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PfMgd" in {
      val testJson = Json.parse("""{"xRef14Char":"XE123456789012","origin":"PfMgd"}""")
      val osd = ExtendedPfMgd.openBankingOriginSpecificSessionData(TestJourneys.PfMgd.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfMgdSessionData(XRef14Char("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PfGbPbRgDuty" in {
      val testJson = Json.parse("""{"generalBettingXRef":"XE123456789012","origin":"PfGbPbRgDuty"}""")
      val osd = ExtendedPfGbPbRgDuty.openBankingOriginSpecificSessionData(TestJourneys.PfGbPbRgDuty.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfGbPbRgDutySessionData(XRef14Char("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PfTrust" in {
      val testJson = Json.parse("""{"trustReference":"XE123456789012","origin":"PfTrust"}""")
      val osd = ExtendedPfTrust.openBankingOriginSpecificSessionData(TestJourneys.PfTrust.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfTrustSessionData(TrustReference("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PfPsAdmin" in {
      val testJson = Json.parse("""{"xRef":"XE123456789012","origin":"PfPsAdmin"}""")
      val osd = ExtendedPfPsAdmin.openBankingOriginSpecificSessionData(TestJourneys.PfPsAdmin.journeyBeforeBeginWebPayment.journeySpecificData)
      testOsd(osd, PfPsAdminSessionData(XRef("XE123456789012"), None), "XE123456789012", "XE123456789012")
      roundTripJsonTest(osd, testJson)
    }

    "PfOther" in {
      ExtendedPfOther.openBankingOriginSpecificSessionData(TestJourneys.PfOther.journeyBeforeBeginWebPayment.journeySpecificData) shouldBe None
    }

  }

  "sanity check for implemented origins" in {
    TestHelpers.implementedOrigins.size shouldBe 67 withClue "** This dummy test is here to remind you to update the tests above. Bump up the expected number when an origin is added to implemented origins **"
  }

}
