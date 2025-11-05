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

import com.google.inject.{Inject, Singleton}
import payapi.cardpaymentjourney.model.journey.{Journey, JsdPfVat, JsdWcVat}
import payapi.corcommon.model.Origins
import payapi.corcommon.model.Origins._
import uk.gov.hmrc.cardpaymentfrontend.models.cardpayment.{ClientId, ClientIds}
import uk.gov.hmrc.cardpaymentfrontend.models.{Language, Languages}

@Singleton
class ClientIdService @Inject() {

  /**
   * Note these have been tested prior to other onboardings.
   * Do not change these unless you are 100% sure, it WILL cause a live incident.
   */
  def determineClientId(journey: Journey[_], language: Language): ClientId = {
    journey.origin match {

      case PfSa | BtaSa | PtaSa | AppSa | ItSa | `3psSa` | WcSa => language match {
        case Languages.English => ClientIds.SAEE
        case Languages.Welsh   => ClientIds.SAEC
      }

      case BtaVat | VcVatReturn | VcVatOther | `3psVat` => language match {
        case Languages.English => ClientIds.VAEE
        case Languages.Welsh   => ClientIds.VAEC
      }

      //yes, for some reason pfVat has weirdness...
      case PfVat => journey.journeySpecificData match {
        case JsdPfVat(_, Some(_)) => language match {
          case Languages.English => ClientIds.MIEE
          case Languages.Welsh   => ClientIds.MIEC
        }
        case _ => language match {
          case Languages.English => ClientIds.VAEE
          case Languages.Welsh   => ClientIds.VAEC
        }
      }

      //yes, copying said weirdness from pfvat...
      case WcVat => journey.journeySpecificData match {
        case JsdWcVat(_, Some(_), _) => language match {
          case Languages.English => ClientIds.MIEE
          case Languages.Welsh   => ClientIds.MIEC
        }
        case _ => language match {
          case Languages.English => ClientIds.VAEE
          case Languages.Welsh   => ClientIds.VAEC
        }
      }

      case PfCt | BtaCt | WcCt => language match {
        case Languages.English => ClientIds.COEE
        case Languages.Welsh   => ClientIds.COEC
      }

      case PfEpayeNi | PfEpayeP11d | WcEpayeNi | WcClass1aNi
        | BtaEpayeBill | BtaEpayeGeneral | BtaClass1aNi => language match {
        case Languages.English => ClientIds.PAEE
        case Languages.Welsh   => ClientIds.PAEC
      }

      case Amls | AppSimpleAssessment | BtaEpayePenalty | BtaEpayeInterest | PfAmls | PfEpayeLpp | PfEpayeSeta
        | PfEpayeLateCis | WcEpayeLateCis | PfJobRetentionScheme | JrsJobRetentionScheme | PfOther | PfPsAdmin
        | BtaSdil | PfMgd | PfGamingOrBingoDuty | PfGbPbRgDuty | PfSdil | PfSimpleAssessment | WcSimpleAssessment
        | PfTpes | PfPpt | PfTrust | EconomicCrimeLevy | PfEconomicCrimeLevy | WcXref | WcEpayeLpp | WcEpayeSeta =>
        language match {
          case Languages.English => ClientIds.MIEE
          case Languages.Welsh   => ClientIds.MIEC
        }

      case PfSdlt => language match {
        case Languages.English => ClientIds.SDEE
        case Languages.Welsh   => ClientIds.SDEC
      }

      case Origins.PfCds | PfCdsCash | PfCdsDeferment => ClientIds.CDEE

      case PfP800 | PtaP800 | Ppt | PtaSimpleAssessment | CapitalGainsTax | AlcoholDuty | PfAlcoholDuty => language match {
        case Languages.English => ClientIds.ETEE
        case Languages.Welsh   => ClientIds.ETEC
      }

      case Mib => ClientIds.MBPE

      case BcPngr => language match {
        case Languages.English => ClientIds.PSEE
        case Languages.Welsh   => ClientIds.PSEC
      }

      case PfClass3Ni | PtaClass3Ni => language match {
        case Languages.English => ClientIds.NICE
        case Languages.Welsh   => ClientIds.NICC
      }

      case VatC2c | PfVatC2c => language match {
        case Languages.English => ClientIds.PLPE
        case Languages.Welsh   => ClientIds.PLPC
      }

      case PfChildBenefitRepayments => language match {
        case Languages.English => ClientIds.CBEE
        case Languages.Welsh   => ClientIds.CBEC
      }

      case NiEuVatOss | PfNiEuVatOss | NiEuVatIoss | PfNiEuVatIoss => ClientIds.OSEE

      case o @ (PfClass2Ni | PfInsurancePremium | Parcels | DdVat | DdSdil | PfSpiritDrinks | PfInheritanceTax | PfWineAndCider
        | PfBioFuels | PfAirPass | PfBeerDuty | PfLandfillTax | PfAggregatesLevy | PfClimateChangeLevy | PfImportedVehicles
        | PfAted | PfPillar2 | Pillar2 | WcClass2Ni) => throw new MatchError(s"Trying to find a client id for an unsupported origin: ${o.entryName}")

    }

  }

}
