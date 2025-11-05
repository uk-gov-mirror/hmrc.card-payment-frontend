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

package uk.gov.hmrc.cardpaymentfrontend.models.extendedorigins

import payapi.cardpaymentjourney.model.journey.JourneySpecificData
import payapi.corcommon.model.Origins._
import payapi.corcommon.model.{Origin, Reference}
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Call}
import uk.gov.hmrc.cardpaymentfrontend.actions.JourneyRequest
import uk.gov.hmrc.cardpaymentfrontend.models.openbanking.OriginSpecificSessionData
import uk.gov.hmrc.cardpaymentfrontend.models._
import uk.gov.hmrc.cardpaymentfrontend.session.JourneySessionSupport._

import java.time.LocalDate

trait ExtendedOrigin {

  def serviceNameMessageKey: String
  def taxNameMessageKey: String

  def amount(request: JourneyRequest[AnyContent]): String = s"£${request.journey.getAmountInPence.inPoundsRoundedFormatted}"

  def reference(request: JourneyRequest[AnyContent]): String = {
    request.journey.journeySpecificData.reference match {
      case Some(Reference(ref)) => ref
      case None                 => throw new RuntimeException("missing reference, this should never ever happen!")
    }
  }

  //denotes which links/payment methods to show on the card-fees page.
  def cardFeesPagePaymentMethods: Set[PaymentMethod]
  // This denotes which payment methods are available for the given Origin/TaxRegime
  def paymentMethods(): Set[PaymentMethod]

  def checkYourAnswersPaymentDateRow(journeyRequest: JourneyRequest[AnyContent])(payFrontendBaseUrl: String): Option[CheckYourAnswersRow] = {
    if (showFuturePayment(journeyRequest)) {
      Some(CheckYourAnswersRow(
        titleMessageKey = "check-your-details.payment-date",
        value           = Seq("check-your-details.payment-date.today"),
        changeLink      = Some(Link(
          href       = Call("GET", s"$payFrontendBaseUrl/change-when-do-you-want-to-pay?toPayFrontendConfirmation=true"),
          linkId     = "check-your-details-payment-date-change-link",
          messageKey = "check-your-details.change"
        ))
      ))
    } else {
      None
    }

  }

  // If dueDate is not today, we do not show the payment date row as FDP is not supported by card payments
  def showFuturePayment(journeyRequest: JourneyRequest[AnyContent]): Boolean = {
    journeyRequest.journey.journeySpecificData.dueDate.fold(false)(LocalDate.now().isBefore)
  }

  protected def changeReferenceUrl(payFrontendBaseUrl: String): String = s"$payFrontendBaseUrl/pay-by-card-change-reference-number"

  //hint: the checkYourAnswersReferenceRow should only include a change link when the journey is not prepopulated, i.e., user has manually entered their reference.
  def checkYourAnswersReferenceRow(journeyRequest: JourneyRequest[AnyContent])(payFrontendBaseUrl: String): Option[CheckYourAnswersRow]

  def checkYourAnswersAdditionalReferenceRow(journeyRequest: JourneyRequest[AnyContent])(payFrontendBaseUrl: String)(implicit messages: Messages): Option[Seq[CheckYourAnswersRow]] = None

  def checkYourAnswersAmountSummaryRow(journeyRequest: JourneyRequest[AnyContent])(payFrontendBaseUrl: String): Option[CheckYourAnswersRow] = Some(CheckYourAnswersRow(
    titleMessageKey = "check-your-details.total-to-pay",
    value           = Seq(amount(journeyRequest)),
    changeLink      = Some(Link(
      href       = Call("GET", s"$payFrontendBaseUrl/change-amount?showSummary=false&stayOnPayFrontend=false"),
      linkId     = "check-your-details-amount-change-link",
      messageKey = "check-your-details.change"
    ))
  ))

  def checkYourAnswersEmailAddressRow(journeyRequest: JourneyRequest[AnyContent]): Option[CheckYourAnswersRow] = {
    val maybeEmail: Option[EmailAddress] = journeyRequest.readFromSession[EmailAddress](journeyRequest.journeyId, Keys.email)
    maybeEmail.filter(!_.value.isBlank)
      .map { email =>
        CheckYourAnswersRow(
          titleMessageKey = "check-your-details.email-address",
          value           = Seq(email.value),
          changeLink      = Some(Link(
            href       = uk.gov.hmrc.cardpaymentfrontend.controllers.routes.EmailAddressController.renderPage,
            linkId     = "check-your-details-email-address-change-link",
            messageKey = "check-your-details.change"
          ))
        )
      }
  }

  // TODO: Update tests to not include country - check doesn't show country
  def checkYourAnswersCardBillingAddressRow(journeyRequest: JourneyRequest[AnyContent]): Option[CheckYourAnswersRow] = {
    val addressFromSession: Option[Address] = journeyRequest.readFromSession[Address](journeyRequest.journeyId, Keys.address)
    val addressValues: Option[Seq[String]] = {
      for {
        line1 <- addressFromSession.map(_.line1)
        line2 <- addressFromSession.map(_.line2)
        city <- addressFromSession.map(_.city)
        county <- addressFromSession.map(_.county)
        postcode <- addressFromSession.map(_.postcode)
      } yield Seq(line1, line2.getOrElse(""), city.getOrElse(""), county.getOrElse(""), postcode.getOrElse(""))
    }.map(_.filter(_.nonEmpty))

    addressValues.map { address: Seq[String] =>
      CheckYourAnswersRow(
        titleMessageKey = "check-your-details.card-billing-address",
        value           = address,
        changeLink      = Some(Link(
          href       = uk.gov.hmrc.cardpaymentfrontend.controllers.routes.AddressController.renderPage,
          linkId     = "check-your-details-card-billing-address-change-link",
          messageKey = "check-your-details.change"
        ))
      )
    }
  }

  def openBankingOriginSpecificSessionData: JourneySpecificData => Option[OriginSpecificSessionData]

  //email related content
  def emailTaxTypeMessageKey: String

  //payments survey stuff
  def surveyAuditName: String
  def surveyReturnHref: String
  def surveyReturnMessageKey: String
  def surveyIsWelshSupported: Boolean
  def surveyBannerTitle: String

}

object ExtendedOrigin {
  implicit class OriginExtended(origin: Origin) {
    def lift: ExtendedOrigin = origin match {
      case PfSa                     => ExtendedPfSa
      case PfVat                    => ExtendedPfVat
      case PfCt                     => ExtendedPfCt
      case PfEpayeNi                => ExtendedPfEpayeNi
      case PfEpayeLpp               => ExtendedPfEpayeLpp
      case PfEpayeSeta              => ExtendedPfEpayeSeta
      case PfEpayeLateCis           => ExtendedPfEpayeLateCis
      case PfEpayeP11d              => ExtendedPfEpayeP11d
      case PfSdlt                   => ExtendedPfSdlt
      case PfOther                  => ExtendedPfOther
      case PfCds                    => ExtendedPfCds
      case PfP800                   => ExtendedPfP800
      case PtaP800                  => ExtendedPtaP800
      case PfClass2Ni               => DefaultExtendedOrigin
      case PfInsurancePremium       => DefaultExtendedOrigin
      case PfPsAdmin                => ExtendedPfPsAdmin
      case BtaSa                    => ExtendedBtaSa
      case AppSa                    => ExtendedAppSa
      case BtaVat                   => ExtendedBtaVat
      case BtaEpayeBill             => ExtendedBtaEpayeBill
      case BtaEpayePenalty          => ExtendedBtaEpayePenalty
      case BtaEpayeInterest         => ExtendedBtaEpayeInterest
      case BtaEpayeGeneral          => ExtendedBtaEpayeGeneral
      case BtaClass1aNi             => ExtendedBtaClass1aNi
      case BtaCt                    => ExtendedBtaCt
      case BtaSdil                  => ExtendedBtaSdil
      case BcPngr                   => ExtendedBcPngr
      case Parcels                  => DefaultExtendedOrigin
      case DdVat                    => DefaultExtendedOrigin
      case DdSdil                   => DefaultExtendedOrigin
      case VcVatReturn              => ExtendedVcVatReturn
      case VcVatOther               => ExtendedVcVatOther
      case ItSa                     => ExtendedItSa
      case Amls                     => ExtendedAmls
      case Ppt                      => ExtendedPpt
      case PfCdsCash                => DefaultExtendedOrigin
      case PfPpt                    => ExtendedPfPpt
      case PfSpiritDrinks           => DefaultExtendedOrigin
      case PfInheritanceTax         => DefaultExtendedOrigin
      case Mib                      => ExtendedMib
      case PfClass3Ni               => DefaultExtendedOrigin
      case PtaSa                    => ExtendedPtaSa
      case PfWineAndCider           => DefaultExtendedOrigin
      case PfBioFuels               => DefaultExtendedOrigin
      case PfAirPass                => DefaultExtendedOrigin
      case PfMgd                    => ExtendedPfMgd
      case PfBeerDuty               => DefaultExtendedOrigin
      case PfGamingOrBingoDuty      => DefaultExtendedOrigin
      case PfGbPbRgDuty             => ExtendedPfGbPbRgDuty
      case PfLandfillTax            => DefaultExtendedOrigin
      case PfSdil                   => ExtendedPfSdil
      case PfAggregatesLevy         => DefaultExtendedOrigin
      case PfClimateChangeLevy      => DefaultExtendedOrigin
      case PfSimpleAssessment       => ExtendedPfSimpleAssessment
      case PtaSimpleAssessment      => ExtendedPtaSimpleAssessment
      case AppSimpleAssessment      => ExtendedAppSimpleAssessment
      case PfTpes                   => ExtendedPfTpes
      case CapitalGainsTax          => ExtendedCapitalGainsTax
      case EconomicCrimeLevy        => ExtendedEconomicCrimeLevy
      case PfEconomicCrimeLevy      => ExtendedPfEconomicCrimeLevy
      case PfJobRetentionScheme     => ExtendedPfJobRetentionScheme
      case JrsJobRetentionScheme    => ExtendedJrsJobRetentionScheme
      case PfImportedVehicles       => DefaultExtendedOrigin
      case PfChildBenefitRepayments => ExtendedPfChildBenefitRepayments
      case NiEuVatOss               => ExtendedNiEuVatOss
      case PfNiEuVatOss             => ExtendedPfNiEuVatOss
      case NiEuVatIoss              => ExtendedNiEuVatIoss
      case PfNiEuVatIoss            => ExtendedPfNiEuVatIoss
      case PfAmls                   => ExtendedPfAmls
      case PfAted                   => DefaultExtendedOrigin
      case PfCdsDeferment           => DefaultExtendedOrigin
      case PfTrust                  => ExtendedPfTrust
      case PtaClass3Ni              => DefaultExtendedOrigin
      case PfAlcoholDuty            => ExtendedPfAlcoholDuty
      case AlcoholDuty              => ExtendedAlcoholDuty
      case VatC2c                   => ExtendedVatC2c
      case PfVatC2c                 => ExtendedPfVatC2c
      case `3psSa`                  => DefaultExtendedOrigin
      case `3psVat`                 => DefaultExtendedOrigin
      case PfPillar2                => DefaultExtendedOrigin
      case Pillar2                  => DefaultExtendedOrigin
      case WcSa                     => ExtendedWcSa
      case WcCt                     => ExtendedWcCt
      case WcVat                    => ExtendedWcVat
      case WcSimpleAssessment       => ExtendedWcSimpleAssessment
      case WcClass1aNi              => ExtendedWcClass1aNi
      case WcXref                   => ExtendedWcXref
      case WcEpayeLpp               => ExtendedWcEpayeLpp
      case WcEpayeNi                => ExtendedWcEpayeNi
      case WcEpayeLateCis           => ExtendedWcEpayeLateCis
      case WcEpayeSeta              => ExtendedWcEpayeSeta
      case WcClass2Ni               => DefaultExtendedOrigin

    }

    def isAWebChatOrigin: Boolean = origin match {
      case PfSa | PfVat | PfCt | PfEpayeNi | PfEpayeLpp | PfEpayeSeta |
        PfEpayeLateCis | PfEpayeP11d | PfSdlt | PfCds | PfOther | PfP800 |
        PtaP800 | PfClass2Ni | PfInsurancePremium | PfPsAdmin | BtaSa | AppSa |
        BtaVat | BtaEpayeBill | BtaEpayePenalty | BtaEpayeInterest | BtaEpayeGeneral |
        BtaClass1aNi | BtaCt | BtaSdil | BcPngr | Parcels | DdVat |
        DdSdil | VcVatReturn | VcVatOther | ItSa | Amls | Ppt |
        PfCdsCash | PfPpt | PfSpiritDrinks | PfInheritanceTax | Mib |
        PfClass3Ni | PtaSa | PfWineAndCider | PfBioFuels | PfAirPass | PfMgd |
        PfBeerDuty | PfGamingOrBingoDuty | PfGbPbRgDuty | PfLandfillTax | PfSdil |
        PfAggregatesLevy | PfClimateChangeLevy | PfSimpleAssessment | PtaSimpleAssessment |
        AppSimpleAssessment | PfTpes | CapitalGainsTax | EconomicCrimeLevy |
        PfEconomicCrimeLevy | PfJobRetentionScheme | JrsJobRetentionScheme | PfImportedVehicles |
        PfChildBenefitRepayments | NiEuVatOss | PfNiEuVatOss | NiEuVatIoss | PfNiEuVatIoss |
        PfAmls | PfAted | PfCdsDeferment | PfTrust | PtaClass3Ni | AlcoholDuty |
        PfAlcoholDuty | VatC2c | PfVatC2c | `3psSa` | `3psVat` | Pillar2 |
        PfPillar2 => false
      case WcSa | WcCt | WcVat | WcSimpleAssessment | WcXref | WcEpayeLpp
        | WcClass1aNi | WcEpayeNi | WcEpayeLateCis | WcEpayeSeta | WcClass2Ni => true
    }

    def originSupportsWelsh: Boolean = origin match {
      case PfSa | PfVat | PfCt | PfEpayeNi | PfEpayeLpp | PfEpayeSeta |
        PfEpayeLateCis | PfEpayeP11d | PfSdlt | PfOther | PfP800 |
        PtaP800 | PfClass2Ni | PfInsurancePremium | PfPsAdmin | BtaSa | AppSa |
        BtaVat | BtaEpayeBill | BtaEpayePenalty | BtaEpayeInterest | BtaEpayeGeneral |
        BtaClass1aNi | BtaCt | BtaSdil | BcPngr | DdVat |
        DdSdil | VcVatReturn | VcVatOther | ItSa | Amls | Ppt |
        PfPpt | PfSpiritDrinks | PfInheritanceTax | Mib |
        PfClass3Ni | PtaSa | PfWineAndCider | PfBioFuels | PfAirPass | PfMgd |
        PfBeerDuty | PfGamingOrBingoDuty | PfGbPbRgDuty | PfLandfillTax | PfSdil |
        PfAggregatesLevy | PfClimateChangeLevy | PfSimpleAssessment | PtaSimpleAssessment |
        AppSimpleAssessment | PfTpes | CapitalGainsTax | EconomicCrimeLevy |
        PfEconomicCrimeLevy | PfJobRetentionScheme | JrsJobRetentionScheme | PfImportedVehicles |
        PfChildBenefitRepayments | PfAmls | PfAted | PfTrust | PtaClass3Ni |
        AlcoholDuty | PfAlcoholDuty | VatC2c | PfVatC2c | `3psSa` | `3psVat` |
        WcSa | WcCt | WcVat | WcSimpleAssessment | WcXref | WcEpayeLpp |
        WcClass1aNi | WcEpayeNi | WcEpayeLateCis | WcEpayeSeta | WcClass2Ni => true
      case PfCds | PfCdsCash | PfCdsDeferment | NiEuVatOss | NiEuVatIoss |
        PfNiEuVatOss | PfNiEuVatIoss | Pillar2 | PfPillar2 | Parcels => false
    }
  }
}
