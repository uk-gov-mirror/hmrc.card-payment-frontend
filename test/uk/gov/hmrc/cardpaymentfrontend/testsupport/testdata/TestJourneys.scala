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

package uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata

import payapi.cardpaymentjourney.model.journey._
import payapi.corcommon.model.cgt.CgtAccountReference
import payapi.corcommon.model.mods.{AmendmentReference, CustomsDutyAmount, VatAmount}
import payapi.corcommon.model.p800.P800ChargeRef
import payapi.corcommon.model.pngr.PngrChargeReference
import payapi.corcommon.model.taxes.ad.{AlcoholDutyChargeReference, AlcoholDutyReference}
import payapi.corcommon.model.taxes.amls.AmlsPaymentReference
import payapi.corcommon.model.taxes.ct._
import payapi.corcommon.model.taxes.epaye._
import payapi.corcommon.model.taxes.mib.MibReference
import payapi.corcommon.model.taxes.nino.Nino
import payapi.corcommon.model.taxes.other._
import payapi.corcommon.model.taxes.p302.{P302ChargeRef, P302Ref}
import payapi.corcommon.model.taxes.p800.P800Ref
import payapi.corcommon.model.taxes.pngr.{AmountPaidPreviously, PngrItem, PngrTaxBreakdown, TotalPaidNow}
import payapi.corcommon.model.taxes.ioss.Ioss
import payapi.corcommon.model.taxes.ppt.PptReference
import payapi.corcommon.model.taxes.sa.SaUtr
import payapi.corcommon.model.taxes.sdlt.Utrn
import payapi.corcommon.model.taxes.trusts.TrustReference
import payapi.corcommon.model.taxes.vat.{CalendarPeriod, VatChargeReference, Vrn}
import payapi.corcommon.model.taxes.vatc2c.VatC2cReference
import payapi.corcommon.model.times.period.TaxQuarter.AprilJuly
import payapi.corcommon.model.times.period.TaxYear
import payapi.corcommon.model.webchat.WcEpayeNiReference
import payapi.corcommon.model.{AmountInPence, JourneyId, PaymentStatuses}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestDataUtils._
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestPayApiData.{testCalendarPeriod, testQuarterlyTaxPeriod, testSubYearlyPeriod, testYearlyPeriod}

import java.time.{LocalDate, LocalDateTime}

sealed trait JourneyStatuses[jsd <: JourneySpecificData] {
  def journeyBeforeBeginWebPayment: Journey[jsd]
  def journeyAfterBeginWebPayment: Journey[jsd] = intoSentWithOrder[jsd](journeyBeforeBeginWebPayment, sentOrder)
  def journeyAfterSucceedDebitWebPayment: Journey[jsd] = intoSuccessWithOrder[jsd](journeyAfterBeginWebPayment, debitCardOrder)
  def journeyAfterSucceedCreditWebPayment: Journey[jsd] = intoSuccessWithOrder[jsd](journeyAfterBeginWebPayment, creditCardOrder)
  def journeyAfterFailWebPayment: Journey[jsd] = intoFailed[jsd](journeyAfterBeginWebPayment, failedOrder)
  def journeyAfterCancelWebPayment: Journey[jsd] = intoCancelled[jsd](journeyAfterBeginWebPayment, cancelledOrder)
}

object TestJourneys {

  object PfSa extends JourneyStatuses[JsdPfSa] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfSa] = Journey[JsdPfSa](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfSa(utr = Some(SaUtr("1234567895"))),
      chosenWayToPay       = None
    )
  }

  object BtaSa extends JourneyStatuses[JsdBtaSa] {
    val journeyBeforeBeginWebPayment: Journey[JsdBtaSa] = Journey[JsdBtaSa](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-bta.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      futureDatedPayment   = None,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdBtaSa(utr                  = SaUtr("1234567895"), defaultAmountInPence = AmountInPence(1234), dueDate = Some(LocalDate.of(2028, 12, 12))),
      chosenWayToPay       = None
    )
  }

  object PtaSa extends JourneyStatuses[JsdPtaSa] {
    val journeyBeforeBeginWebPayment: Journey[JsdPtaSa] = Journey[JsdPtaSa](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-pta.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      futureDatedPayment   = None,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPtaSa(utr                  = SaUtr("1234567895"), defaultAmountInPence = Some(AmountInPence(1234)), dueDate = Some(LocalDate.of(2028, 12, 12))),
      chosenWayToPay       = None
    )
  }

  object ItSa extends JourneyStatuses[JsdItSa] {
    val journeyBeforeBeginWebPayment: Journey[JsdItSa] = Journey[JsdItSa](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-itsa.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdItSa(utr     = SaUtr("1234567895"), dueDate = Some(LocalDate.of(2028, 12, 12))),
      chosenWayToPay       = None
    )
  }

  object WcSa extends JourneyStatuses[JsdWcSa] {
    val journeyBeforeBeginWebPayment: Journey[JsdWcSa] = Journey[JsdWcSa](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      futureDatedPayment   = None,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdWcSa(saUtr                = SaUtr("1234567895"), defaultAmountInPence = AmountInPence(1234)),
      chosenWayToPay       = None
    )
  }

  object PfAlcoholDuty extends JourneyStatuses[JsdPfAlcoholDuty] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfAlcoholDuty] = Journey[JsdPfAlcoholDuty](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfAlcoholDuty(alcoholDutyReference = Some(AlcoholDutyReference("XMADP0123456789"))),
      chosenWayToPay       = None
    )
  }

  object AlcoholDuty extends JourneyStatuses[JsdAlcoholDuty] {
    val journeyBeforeBeginWebPayment: Journey[JsdAlcoholDuty] = Journey[JsdAlcoholDuty](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-alcoholduty.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdAlcoholDuty(
        alcoholDutyReference       = AlcoholDutyReference("XMADP0123456789"),
        alcoholDutyChargeReference = Some(AlcoholDutyChargeReference("XE1234567890123")),
        defaultAmountInPence       = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object BtaCt extends JourneyStatuses[JsdBtaCt] {
    val journeyBeforeBeginWebPayment: Journey[JsdBtaCt] = Journey[JsdBtaCt](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-btact.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdBtaCt(
        utr                  = CtUtr("1097172564"),
        ctPeriod             = Some(CtPeriod(1)),
        ctChargeType         = Some(CtChargeTypes.A),
        defaultAmountInPence = AmountInPence(1234),
        dueDate              = Some(LocalDate.of(2028, 12, 12)),
        ctLivePeriods        = Some(List(CtLivePeriod(1, LocalDate.of(2028, 11, 12), LocalDate.of(2028, 12, 12))))
      ),
      chosenWayToPay       = None
    )
  }

  object PfCt extends JourneyStatuses[JsdPfCt] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfCt] = Journey[JsdPfCt](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfCt(
        utr          = Some(CtUtr("1097172564")),
        ctPeriod     = Some(CtPeriod(1)),
        ctChargeType = Some(CtChargeTypes.A)
      ),
      chosenWayToPay       = None
    )
  }

  object WcCt extends JourneyStatuses[JsdWcCt] {
    val journeyBeforeBeginWebPayment: Journey[JsdWcCt] = Journey[JsdWcCt](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdWcCt(
        ctPayslipReference   = CtPayslipReference(CtUtr("1097172564"), CtPeriod(1), CtChargeTypes.A),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object PfEpayeNi extends JourneyStatuses[JsdPfEpayeNi] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfEpayeNi] = Journey[JsdPfEpayeNi](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfEpayeNi(
        Some(AccountsOfficeReference("123PH45678900")),
        Some(QuarterlyEpayeTaxPeriod(AprilJuly, TaxYear(2025)))
      ),
      chosenWayToPay       = None
    )
  }

  object PfEpayeP11d extends JourneyStatuses[JsdPfEpayeP11d] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfEpayeP11d] = Journey[JsdPfEpayeP11d](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfEpayeP11d(
        Some(AccountsOfficeReference("123PH45678900")),
        Some(YearlyEpayeTaxPeriod(TaxYear(2025)))
      ),
      chosenWayToPay       = None
    )
  }

  object PfEpayeLpp extends JourneyStatuses[JsdPfEpayeLpp] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfEpayeLpp] = Journey[JsdPfEpayeLpp](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfEpayeLpp(prn = Some(XRef("XE123456789012"))),
      chosenWayToPay       = None
    )
  }

  object PfEpayeLateCis extends JourneyStatuses[JsdPfEpayeLateCis] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfEpayeLateCis] = Journey[JsdPfEpayeLateCis](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfEpayeLateCis(prn = Some(XRef14Char("XE123456789012"))),
      chosenWayToPay       = None
    )
  }

  object PfEpayeSeta extends JourneyStatuses[JsdPfEpayeSeta] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfEpayeSeta] = Journey[JsdPfEpayeSeta](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfEpayeSeta(psaNumber = Some(PsaNumber("XA123456789012"))),
      chosenWayToPay       = None
    )
  }

  object PfVat extends JourneyStatuses[JsdPfVat] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfVat] = Journey[JsdPfVat](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfVat(
        vrn       = Some(Vrn("999964805")),
        chargeRef = None
      ),
      chosenWayToPay       = None
    )
  }

  object PfVatWithChargeReference extends JourneyStatuses[JsdPfVat] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfVat] = Journey[JsdPfVat](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfVat(
        vrn       = None,
        chargeRef = Some(XRef14Char("XE123456789012"))
      ),
      chosenWayToPay       = None
    )
  }

  object WcVat extends JourneyStatuses[JsdWcVat] {
    val journeyBeforeBeginWebPayment: Journey[JsdWcVat] = Journey[JsdWcVat](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdWcVat(
        vrn                  = Some(Vrn("999964805")),
        chargeReference      = None,
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object WcVatWithChargeReference extends JourneyStatuses[JsdWcVat] {
    val journeyBeforeBeginWebPayment: Journey[JsdWcVat] = Journey[JsdWcVat](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdWcVat(
        vrn                  = None,
        chargeReference      = Some(XRef14Char("XE123456789012")),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object BtaVat extends JourneyStatuses[JsdBtaVat] {
    val journeyBeforeBeginWebPayment: Journey[JsdBtaVat] = Journey[JsdBtaVat](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-btavat.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdBtaVat(vrn                  = Vrn("999964805"), defaultAmountInPence = AmountInPence(1234), dueDate = Some(LocalDate.of(2028, 12, 12))),
      chosenWayToPay       = None
    )
  }

  object VcVatReturn extends JourneyStatuses[JsdVcVatReturn] {
    val journeyBeforeBeginWebPayment: Journey[JsdVcVatReturn] = Journey[JsdVcVatReturn](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-vcvatreturn.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdVcVatReturn(
        vrn                  = Vrn("999964805"),
        defaultAmountInPence = AmountInPence(1234),
        dueDate              = Some(LocalDate.of(2028, 12, 12)),
        accountingPeriod     = Some(CalendarPeriod(11, 2027))
      ),
      chosenWayToPay       = None
    )
  }

  object VcVatOther extends JourneyStatuses[JsdVcVatOther] {
    val journeyBeforeBeginWebPayment: Journey[JsdVcVatOther] = Journey[JsdVcVatOther](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-vcvatreturn.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdVcVatOther(
        vrn                  = Vrn("999964805"),
        defaultAmountInPence = AmountInPence(1234),
        dueDate              = Some(LocalDate.of(2028, 12, 12)),
        accountingPeriod     = Some(CalendarPeriod(11, 2027)),
        chargeReference      = VatChargeReference("999964805")
      ),
      chosenWayToPay       = None
    )
  }

  object Ppt extends JourneyStatuses[JsdPpt] {
    val journeyBeforeBeginWebPayment: Journey[JsdPpt] = Journey[JsdPpt](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-ppt.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPpt(
        pptReference         = PptReference("XAPPT0000012345"),
        defaultAmountInPence = AmountInPence(1234),
        dueDate              = Some(LocalDate.of(2028, 12, 12))
      ),
      chosenWayToPay       = None
    )
  }

  object PfPpt extends JourneyStatuses[JsdPfPpt] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfPpt] = Journey[JsdPfPpt](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfPpt(pptReference = Some(PptReference("XAPPT0000012345"))),
      chosenWayToPay       = None
    )
  }

  object BtaEpayeBill extends JourneyStatuses[JsdBtaEpayeBill] {
    val journeyBeforeBeginWebPayment: Journey[JsdBtaEpayeBill] = Journey[JsdBtaEpayeBill](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-btaepayebill.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdBtaEpayeBill(
        accountsOfficeReference = AccountsOfficeReference("123PH45678900"),
        period                  = testSubYearlyPeriod,
        defaultAmountInPence    = AmountInPence(1234),
        dueDate                 = Some(LocalDate.of(2028, 12, 12))
      ),
      chosenWayToPay       = None
    )
  }

  object BtaEpayePenalty extends JourneyStatuses[JsdBtaEpayePenalty] {
    val journeyBeforeBeginWebPayment: Journey[JsdBtaEpayePenalty] = Journey[JsdBtaEpayePenalty](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-btaepayebill.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdBtaEpayePenalty(
        epayePenaltyReference   = EpayePenaltyReference("123PH45678900"),
        accountsOfficeReference = AccountsOfficeReference("123PH45678900"),
        period                  = testSubYearlyPeriod,
        defaultAmountInPence    = AmountInPence(1234),
        dueDate                 = Some(LocalDate.of(2028, 12, 12))
      ),
      chosenWayToPay       = None
    )
  }

  object BtaEpayeInterest extends JourneyStatuses[JsdBtaEpayeInterest] {
    val journeyBeforeBeginWebPayment: Journey[JsdBtaEpayeInterest] = Journey[JsdBtaEpayeInterest](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-btaepayebill.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdBtaEpayeInterest(
        xRef                    = XRef("XE123456789012"),
        accountsOfficeReference = AccountsOfficeReference("123PH45678900"),
        period                  = testSubYearlyPeriod,
        defaultAmountInPence    = AmountInPence(1234),
        dueDate                 = Some(LocalDate.of(2028, 12, 12))
      ),
      chosenWayToPay       = None
    )
  }

  object BtaEpayeGeneral extends JourneyStatuses[JsdBtaEpayeGeneral] {
    val journeyBeforeBeginWebPayment: Journey[JsdBtaEpayeGeneral] = Journey[JsdBtaEpayeGeneral](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-btaepayebill.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdBtaEpayeGeneral(
        accountsOfficeReference = AccountsOfficeReference("123PH45678900"),
        dueDate                 = Some(LocalDate.of(2028, 12, 12)),
        period                  = Some(testSubYearlyPeriod)
      ),
      chosenWayToPay       = None
    )
  }

  object BtaClass1aNi extends JourneyStatuses[JsdBtaClass1aNi] {
    val journeyBeforeBeginWebPayment: Journey[JsdBtaClass1aNi] = Journey[JsdBtaClass1aNi](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-btaepayebill.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdBtaClass1aNi(
        accountsOfficeReference = AccountsOfficeReference("123PH45678900"),
        period                  = testYearlyPeriod,
        defaultAmountInPence    = AmountInPence(1234),
        dueDate                 = Some(LocalDate.of(2028, 12, 12))
      ),
      chosenWayToPay       = None
    )
  }

  object Amls extends JourneyStatuses[JsdAmls] {
    val journeyBeforeBeginWebPayment: Journey[JsdAmls] = Journey[JsdAmls](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-amls.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdAmls(
        amlsPaymentReference = AmlsPaymentReference("XE123456789012"),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object PfAmls extends JourneyStatuses[JsdPfAmls] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfAmls] = Journey[JsdPfAmls](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfAmls(
        amlsPaymentReference = Some(AmlsPaymentReference("XE123456789012"))
      ),
      chosenWayToPay       = None
    )
  }

  object PfSdlt extends JourneyStatuses[JsdPfSdlt] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfSdlt] = Journey[JsdPfSdlt](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfSdlt(utrn = Some(Utrn("123456789MA"))),
      chosenWayToPay       = None
    )
  }

  object CapitalGainsTax extends JourneyStatuses[JsdCapitalGainsTax] {
    val journeyBeforeBeginWebPayment: Journey[JsdCapitalGainsTax] = Journey[JsdCapitalGainsTax](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-capitalgainstax.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdCapitalGainsTax(
        cgtReference         = CgtAccountReference("XVCGTP001000290"),
        cgtChargeReference   = None,
        defaultAmountInPence = AmountInPence(1234),
        dueDate              = None
      ),
      chosenWayToPay       = None
    )
  }

  object EconomicCrimeLevy extends JourneyStatuses[JsdEconomicCrimeLevy] {
    val journeyBeforeBeginWebPayment: Journey[JsdEconomicCrimeLevy] = Journey[JsdEconomicCrimeLevy](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-amls.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdEconomicCrimeLevy(
        chargeReference      = EconomicCrimeLevyReturnNumber("XE123456789012"),
        defaultAmountInPence = AmountInPence(1234),
        dueDate              = Some(LocalDate.of(2028, 12, 12))
      ),
      chosenWayToPay       = None
    )
  }

  object PfEconomicCrimeLevy extends JourneyStatuses[JsdPfEconomicCrimeLevy] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfEconomicCrimeLevy] = Journey[JsdPfEconomicCrimeLevy](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfEconomicCrimeLevy(
        chargeReference = Some(EconomicCrimeLevyReturnNumber("XE123456789012"))
      ),
      chosenWayToPay       = None
    )
  }

  object VatC2c extends JourneyStatuses[JsdVatC2c] {
    val journeyBeforeBeginWebPayment: Journey[JsdVatC2c] = Journey[JsdVatC2c](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdVatC2c(
        vatC2cReference      = VatC2cReference("XVC1A2B3C4D5E6F"),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object PfVatC2c extends JourneyStatuses[JsdPfVatC2c] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfVatC2c] = Journey[JsdPfVatC2c](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfVatC2c(
        vatC2cReference = Some(VatC2cReference("XVC1A2B3C4D5E6F"))
      ),
      chosenWayToPay       = None
    )
  }

  object WcSimpleAssessment extends JourneyStatuses[JsdWcSimpleAssessment] {
    val journeyBeforeBeginWebPayment: Journey[JsdWcSimpleAssessment] = Journey[JsdWcSimpleAssessment](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdWcSimpleAssessment(
        simpleAssessmentReference = XRef14Char("XE123456789012"),
        defaultAmountInPence      = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object WcClass1aNi extends JourneyStatuses[JsdWcClass1aNi] {
    val journeyBeforeBeginWebPayment: Journey[JsdWcClass1aNi] = Journey[JsdWcClass1aNi](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdWcClass1aNi(
        paymentReference     = WcClass1aNiReference("123PH456789002713"),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object WcXref extends JourneyStatuses[JsdWcXref] {
    val journeyBeforeBeginWebPayment: Journey[JsdWcXref] = Journey[JsdWcXref](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdWcXref(
        chargeReference      = XRef("XE123456789012"),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object WcEpayeLpp extends JourneyStatuses[JsdWcEpayeLpp] {
    val journeyBeforeBeginWebPayment: Journey[JsdWcEpayeLpp] = Journey[JsdWcEpayeLpp](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdWcEpayeLpp(
        chargeReference      = XRef("XE123456789012"),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object WcEpayeNi extends JourneyStatuses[JsdWcEpayeNi] {
    val journeyBeforeBeginWebPayment: Journey[JsdWcEpayeNi] = Journey[JsdWcEpayeNi](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdWcEpayeNi(
        paymentReference     = WcEpayeNiReference("123PH456789002501"),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object WcEpayeLateCis extends JourneyStatuses[JsdWcEpayeLateCis] {
    val journeyBeforeBeginWebPayment: Journey[JsdWcEpayeLateCis] = Journey[JsdWcEpayeLateCis](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdWcEpayeLateCis(
        chargeReference      = XRef14Char("XE123456789012"),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object PfChildBenefitRepayments extends JourneyStatuses[JsdPfChildBenefitRepayments] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfChildBenefitRepayments] = Journey[JsdPfChildBenefitRepayments](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfChildBenefitRepayments(
        yReference = Some(YRef("YA123456789123"))
      ),
      chosenWayToPay       = None
    )
  }

  object WcChildBenefitRepayments extends JourneyStatuses[JsdWcChildBenefitRepayments] {
    val journeyBeforeBeginWebPayment: Journey[JsdWcChildBenefitRepayments] = Journey[JsdWcChildBenefitRepayments](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdWcChildBenefitRepayments(
        yReference           = YRef("YA123456789123"),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object BtaSdil extends JourneyStatuses[JsdBtaSdil] {
    val journeyBeforeBeginWebPayment: Journey[JsdBtaSdil] = Journey[JsdBtaSdil](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-bta.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdBtaSdil(
        defaultAmountInPence = AmountInPence(1234),
        xRef                 = XRef("XE1234567890123"),
        dueDate              = Some(LocalDate.of(2028, 12, 12))
      ),
      chosenWayToPay       = None
    )
  }

  object PfSdil extends JourneyStatuses[JsdPfSdil] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfSdil] = Journey[JsdPfSdil](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfSdil(
        softDrinksIndustryLevyRef = Some(SoftDrinksIndustryLevyRef("XE1234567890123"))
      ),
      chosenWayToPay       = None
    )
    val journeyWithPenaltyReferenceBeforeBeginWebPayment: Journey[JsdPfSdil] = Journey[JsdPfSdil](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfSdil(
        softDrinksIndustryLevyRef = Some(SoftDrinksIndustryLevyRef("XE123456789012"))
      ),
      chosenWayToPay       = None
    )
  }

  object PfP800 extends JourneyStatuses[JsdPfP800] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfP800] = Journey[JsdPfP800](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-pta.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfP800(
        p800Ref       = P800Ref("MA000003AP8002027"),
        p800ChargeRef = None,
        taxYear       = TaxYear(2027)
      ),
      chosenWayToPay       = None
    )
    val journeyWithP800ChargeRefBeforeBeginWebPayment: Journey[JsdPfP800] = Journey[JsdPfP800](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-pta.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfP800(
        p800Ref       = P800Ref("MA000003AP8002027"),
        p800ChargeRef = Some(P800ChargeRef("BC007010065114")),
        taxYear       = TaxYear(2027)
      ),
      chosenWayToPay       = None
    )
  }

  object PtaP800 extends JourneyStatuses[JsdPtaP800] {
    val journeyBeforeBeginWebPayment: Journey[JsdPtaP800] = Journey[JsdPtaP800](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-pta.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPtaP800(
        p800Ref              = P800Ref("MA000003AP8002027"),
        p800ChargeRef        = None,
        taxYear              = TaxYear(2027),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
    val journeyWithP800ChargeRefBeforeBeginWebPayment: Journey[JsdPtaP800] = Journey[JsdPtaP800](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-pta.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPtaP800(
        p800Ref              = P800Ref("MA000003AP8002027"),
        p800ChargeRef        = Some(P800ChargeRef("BC007010065114")),
        taxYear              = TaxYear(2027),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object PfSimpleAssessment extends JourneyStatuses[JsdPfSimpleAssessment] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfSimpleAssessment] = Journey[JsdPfSimpleAssessment](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfSimpleAssessment(
        simpleAssessmentReference = Some(XRef14Char("XE123456789012"))
      ),
      chosenWayToPay       = None
    )
  }

  object PtaSimpleAssessment extends JourneyStatuses[JsdPtaSimpleAssessment] {
    val journeyBeforeBeginWebPayment: Journey[JsdPtaSimpleAssessment] = Journey[JsdPtaSimpleAssessment](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-pta.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPtaSimpleAssessment(
        p302Ref              = P302Ref("MA000003AP3022027"),
        p302ChargeRef        = P302ChargeRef("BC007010065114"),
        taxYear              = TaxYear(2027),
        defaultAmountInPence = AmountInPence(1234),
        dueDate              = Some(LocalDate.of(2028, 12, 12))
      ),
      chosenWayToPay       = None
    )
  }

  object WcEpayeSeta extends JourneyStatuses[JsdWcEpayeSeta] {
    val journeyBeforeBeginWebPayment: Journey[JsdWcEpayeSeta] = Journey[JsdWcEpayeSeta](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdWcEpayeSeta(
        chargeReference      = XRef("XE123456789012"),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object NiEuVatOss extends JourneyStatuses[JsdNiEuVatOss] {
    val journeyBeforeBeginWebPayment: Journey[JsdNiEuVatOss] = Journey[JsdNiEuVatOss](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-ni-eu-vat-oss.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdNiEuVatOss(
        vrn                  = Vrn("101747641"),
        period               = testQuarterlyTaxPeriod,
        defaultAmountInPence = AmountInPence(1234),
        dueDate              = Some(LocalDate.of(2028, 12, 12))
      ),
      chosenWayToPay       = None
    )
  }

  object PfNiEuVatOss extends JourneyStatuses[JsdPfNiEuVatOss] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfNiEuVatOss] = Journey[JsdPfNiEuVatOss](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfNiEuVatOss(
        vrn    = Some(Vrn("101747641")),
        period = Some(testQuarterlyTaxPeriod)
      ),
      chosenWayToPay       = None
    )
  }

  object NiEuVatIoss extends JourneyStatuses[JsdNiEuVatIoss] {
    val journeyBeforeBeginWebPayment: Journey[JsdNiEuVatIoss] = Journey[JsdNiEuVatIoss](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-ni-eu-vat-ioss.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdNiEuVatIoss(
        ioss                 = Ioss("IM1234567890"),
        period               = testCalendarPeriod,
        defaultAmountInPence = AmountInPence(1234),
        dueDate              = Some(LocalDate.of(2028, 12, 12))
      ),
      chosenWayToPay       = None
    )
  }

  object PfNiEuVatIoss extends JourneyStatuses[JsdPfNiEuVatIoss] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfNiEuVatIoss] = Journey[JsdPfNiEuVatIoss](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfNiEuVatIoss(
        ioss   = Some(Ioss("IM1234567890")),
        period = Some(testCalendarPeriod)
      ),
      chosenWayToPay       = None
    )
  }

  object PfJobRetentionScheme extends JourneyStatuses[JsdPfJobRetentionScheme] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfJobRetentionScheme] = Journey[JsdPfJobRetentionScheme](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfJobRetentionScheme(
        prn = Some(JrsRef("XJRS12345678901"))
      ),
      chosenWayToPay       = None
    )
  }

  object JrsJobRetentionScheme extends JourneyStatuses[JsdJrsJobRetentionScheme] {
    val journeyBeforeBeginWebPayment: Journey[JsdJrsJobRetentionScheme] = Journey[JsdJrsJobRetentionScheme](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-bta.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdJrsJobRetentionScheme(
        jrsRef               = JrsRef("XJRS12345678901"),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object AppSa extends JourneyStatuses[JsdAppSa] {
    val journeyBeforeBeginWebPayment: Journey[JsdAppSa] = Journey[JsdAppSa](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-app-sa.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdAppSa(
        utr                  = SaUtr("1234567890"),
        defaultAmountInPence = AmountInPence(1234),
        dueDate              = Some(LocalDate.now())
      ),
      chosenWayToPay       = None
    )
  }

  object AppSimpleAssessment extends JourneyStatuses[JsdAppSimpleAssessment] {
    val journeyBeforeBeginWebPayment: Journey[JsdAppSimpleAssessment] = Journey[JsdAppSimpleAssessment](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-app-sa.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdAppSimpleAssessment(
        nino                 = Nino("AA123456A"),
        p302Ref              = P800Ref("MA000003AP3022023"),
        taxYear              = TaxYear(2023),
        defaultAmountInPence = AmountInPence(1234)
      ),
      chosenWayToPay       = None
    )
  }

  object Mib extends JourneyStatuses[JsdMib] {
    val journeyBeforeBeginWebPayment: Journey[JsdMib] = Journey[JsdMib](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-bta.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdMib(
        mibReference       = MibReference("MIBI1234567891"),
        vatAmountInPence   = VatAmount(123),
        dutyAmountInPence  = CustomsDutyAmount(123),
        amendmentReference = Some(AmendmentReference(123456789))
      ),
      chosenWayToPay       = None
    )
  }

  object BcPngr extends JourneyStatuses[JsdBcPngr] {
    val journeyBeforeBeginWebPayment: Journey[JsdBcPngr] = Journey[JsdBcPngr](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = Some(NavigationOptions(returnUrl = Url("https://www.return-url.com"), backUrl = Url("https://www.back-to-pngr.com"))),
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdBcPngr(
        chargeReference      = PngrChargeReference("XAPR9876543210"),
        taxToPayInPence      = AmountInPence(1234),
        dateOfArrival        = LocalDateTime.parse("2027-11-02T16:28:55.185"),
        passengerName        = "Bob Ross",
        placeOfArrival       = "Heathrow",
        items                = List(
          PngrItem(
            name             = "Booze",
            price            = "124",
            purchaseLocation = "Vire Normandie",
            costInGbp        = "113",
            producedIn       = Some("France"),
            evidenceOfOrigin = Some("receipt")
          ),
          PngrItem(
            name             = "Even more booze",
            price            = "125",
            purchaseLocation = "Vire Normandie",
            costInGbp        = "116",
            producedIn       = Some("France"),
            evidenceOfOrigin = Some("receipt")
          )
        ),
        taxBreakdown         = PngrTaxBreakdown(
          "123",
          "66",
          "5"
        ),
        amountPaidPreviously = Some(AmountPaidPreviously("55")),
        totalPaidNow         = Some(TotalPaidNow("1234"))
      ),
      chosenWayToPay       = None
    )
  }

  object PfCds extends JourneyStatuses[JsdPfCds] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfCds] = Journey[JsdPfCds](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfCds(
        cdsRef      = Some(TestPayApiData.testCdsRef),
        year4Digits = 2027
      ),
      chosenWayToPay       = None
    )
  }

  object PfTpes extends JourneyStatuses[JsdPfTpes] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfTpes] = Journey[JsdPfTpes](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfTpes(
        xRef = Some(XRef("XE123456789012"))
      ),
      chosenWayToPay       = None
    )
  }

  object PfMgd extends JourneyStatuses[JsdPfMgd] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfMgd] = Journey[JsdPfMgd](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfMgd(
        prn = Some(XRef14Char("XE123456789012"))
      ),
      chosenWayToPay       = None
    )
  }

  object PfGbPbRgDuty extends JourneyStatuses[JsdPfGbPbRgDuty] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfGbPbRgDuty] = Journey[JsdPfGbPbRgDuty](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfGbPbRgDuty(
        prn = Some(XRef14Char("XE123456789012"))
      ),
      chosenWayToPay       = None
    )
  }

  object PfTrust extends JourneyStatuses[JsdPfTrust] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfTrust] = Journey[JsdPfTrust](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfTrust(
        trustsReference = Some(TrustReference("XE123456789012"))
      ),
      chosenWayToPay       = None
    )
  }

  object PfOther extends JourneyStatuses[JsdPfOther] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfOther] = Journey[JsdPfOther](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfOther(
        otherRef = Some(XRef("XE123456789012"))
      ),
      chosenWayToPay       = None
    )
  }

  object PfPsAdmin extends JourneyStatuses[JsdPfPsAdmin] {
    val journeyBeforeBeginWebPayment: Journey[JsdPfPsAdmin] = Journey[JsdPfPsAdmin](
      _id                  = JourneyId(TestPayApiData.decryptedJourneyId),
      sessionId            = Some(SessionId("TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854")),
      amountInPence        = Some(AmountInPence(1234)),
      emailTemplateOptions = None,
      navigation           = None,
      order                = None,
      status               = PaymentStatuses.Created,
      createdOn            = LocalDateTime.parse("2027-11-02T16:28:55.185"),
      journeySpecificData  = JsdPfPsAdmin(
        xRef = Some(XRef("XE123456789012"))
      ),
      chosenWayToPay       = None
    )
  }

}
