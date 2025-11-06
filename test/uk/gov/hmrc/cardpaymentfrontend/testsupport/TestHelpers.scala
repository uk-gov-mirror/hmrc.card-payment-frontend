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

package uk.gov.hmrc.cardpaymentfrontend.testsupport

import org.scalatest.AppendedClues.convertToClueful
import payapi.cardpaymentjourney.model.journey._
import payapi.corcommon.model.Origins._
import payapi.corcommon.model.{Origin, Origins}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys

class TestHelpers extends UnitSpec {
  "all origins should be covered in implemented and unimplemented origins" in {
    TestHelpers.implementedOrigins ++ TestHelpers.unimplementedOrigins should contain theSameElementsAs
      Origins.values withClue s"\n\n *** Missing origin from one of the lists: [ ${Origins.values.diff(TestHelpers.implementedOrigins ++ TestHelpers.unimplementedOrigins).mkString(", ")} ]\n\n"
  }
}

object TestHelpers {

  val implementedOrigins: Seq[Origin] = Seq[Origin](
    PfSa,
    BtaSa,
    PtaSa,
    ItSa,
    WcSa,
    PfAlcoholDuty,
    AlcoholDuty,
    PfCt,
    BtaCt,
    WcCt,
    PfEpayeNi,
    PfEpayeLpp,
    PfEpayeLateCis,
    PfEpayeP11d,
    WcClass1aNi,
    PfEpayeSeta,
    PfVat,
    BtaVat,
    WcVat,
    VcVatReturn,
    VcVatOther,
    Ppt,
    PfPpt,
    BtaEpayeBill,
    BtaEpayePenalty,
    BtaEpayeInterest,
    BtaEpayeGeneral,
    BtaClass1aNi,
    PfAmls,
    Amls,
    CapitalGainsTax,
    EconomicCrimeLevy,
    PfEconomicCrimeLevy,
    PfSdlt,
    VatC2c,
    PfVatC2c,
    WcSimpleAssessment,
    WcXref,
    WcEpayeLpp,
    WcEpayeNi,
    WcEpayeLateCis,
    WcEpayeSeta,
    PfChildBenefitRepayments,
    BtaSdil,
    PfSdil,
    PfSimpleAssessment,
    PtaSimpleAssessment,
    PfP800,
    PtaP800,
    PfJobRetentionScheme,
    JrsJobRetentionScheme,
    NiEuVatOss,
    PfNiEuVatOss,
    NiEuVatIoss,
    PfNiEuVatIoss,
    PfCds,
    AppSa,
    AppSimpleAssessment,
    Mib,
    BcPngr,
    PfTpes,
    PfMgd,
    PfGbPbRgDuty,
    PfTrust,
    PfOther,
    PfPsAdmin,
    WcChildBenefitRepayments
  )

  val unimplementedOrigins: Seq[Origin] = Seq[Origin](
    PfClass2Ni,
    PfInsurancePremium,
    Parcels,
    DdVat,
    DdSdil,
    PfCdsCash,
    PfSpiritDrinks,
    PfInheritanceTax,
    PfClass3Ni,
    PfWineAndCider,
    PfBioFuels,
    PfAirPass,
    PfBeerDuty,
    PfGamingOrBingoDuty,
    PfLandfillTax,
    PfAggregatesLevy,
    PfClimateChangeLevy,
    PfImportedVehicles,
    PfAted,
    PfCdsDeferment,
    PtaClass3Ni,
    `3psSa`,
    `3psVat`,
    PfPillar2,
    Pillar2
  )

  def deriveTestDataFromOrigin[jsd <: JourneySpecificData](origin: Origin) = origin match {
    case PfSa                     => TestJourneys.PfSa
    case BtaSa                    => TestJourneys.BtaSa
    case PtaSa                    => TestJourneys.PtaSa
    case ItSa                     => TestJourneys.ItSa
    case PfVat                    => TestJourneys.PfVat
    case PfCt                     => TestJourneys.PfCt
    case PfEpayeNi                => TestJourneys.PfEpayeNi
    case PfEpayeLpp               => TestJourneys.PfEpayeLpp
    case PfEpayeSeta              => TestJourneys.PfEpayeSeta
    case PfEpayeLateCis           => TestJourneys.PfEpayeLateCis
    case PfEpayeP11d              => TestJourneys.PfEpayeP11d
    case PfSdlt                   => TestJourneys.PfSdlt
    case PfCds                    => TestJourneys.PfCds
    case PfOther                  => TestJourneys.PfOther
    case PfP800                   => TestJourneys.PfP800
    case PtaP800                  => TestJourneys.PtaP800
    case PfClass2Ni               => throw new MatchError("Not implemented yet")
    case PfInsurancePremium       => throw new MatchError("Not implemented yet")
    case PfPsAdmin                => TestJourneys.PfPsAdmin
    case AppSa                    => TestJourneys.AppSa
    case BtaVat                   => TestJourneys.BtaVat
    case BtaEpayeBill             => TestJourneys.BtaEpayeBill
    case BtaEpayePenalty          => TestJourneys.BtaEpayePenalty
    case BtaEpayeInterest         => TestJourneys.BtaEpayeInterest
    case BtaEpayeGeneral          => TestJourneys.BtaEpayeGeneral
    case BtaClass1aNi             => TestJourneys.BtaClass1aNi
    case BtaCt                    => TestJourneys.BtaCt
    case BtaSdil                  => TestJourneys.BtaSdil
    case BcPngr                   => TestJourneys.BcPngr
    case Parcels                  => throw new MatchError("Not implemented yet")
    case DdVat                    => throw new MatchError("Not implemented yet")
    case DdSdil                   => throw new MatchError("Not implemented yet")
    case VcVatReturn              => TestJourneys.VcVatReturn
    case VcVatOther               => TestJourneys.VcVatOther
    case Amls                     => TestJourneys.Amls
    case Ppt                      => TestJourneys.Ppt
    case PfCdsCash                => throw new MatchError("Not implemented yet")
    case PfPpt                    => TestJourneys.PfPpt
    case PfSpiritDrinks           => throw new MatchError("Not implemented yet")
    case PfInheritanceTax         => throw new MatchError("Not implemented yet")
    case Mib                      => TestJourneys.Mib
    case PfClass3Ni               => throw new MatchError("Not implemented yet")
    case PfWineAndCider           => throw new MatchError("Not implemented yet")
    case PfBioFuels               => throw new MatchError("Not implemented yet")
    case PfAirPass                => throw new MatchError("Not implemented yet")
    case PfMgd                    => TestJourneys.PfMgd
    case PfBeerDuty               => throw new MatchError("Not implemented yet")
    case PfGamingOrBingoDuty      => throw new MatchError("Not implemented yet")
    case PfGbPbRgDuty             => TestJourneys.PfGbPbRgDuty
    case PfLandfillTax            => throw new MatchError("Not implemented yet")
    case PfSdil                   => TestJourneys.PfSdil
    case PfAggregatesLevy         => throw new MatchError("Not implemented yet")
    case PfClimateChangeLevy      => throw new MatchError("Not implemented yet")
    case PfSimpleAssessment       => TestJourneys.PfSimpleAssessment
    case PtaSimpleAssessment      => TestJourneys.PtaSimpleAssessment
    case AppSimpleAssessment      => TestJourneys.AppSimpleAssessment
    case WcSimpleAssessment       => TestJourneys.WcSimpleAssessment
    case PfTpes                   => TestJourneys.PfTpes
    case CapitalGainsTax          => TestJourneys.CapitalGainsTax
    case EconomicCrimeLevy        => TestJourneys.EconomicCrimeLevy
    case PfEconomicCrimeLevy      => TestJourneys.PfEconomicCrimeLevy
    case PfJobRetentionScheme     => TestJourneys.PfJobRetentionScheme
    case JrsJobRetentionScheme    => TestJourneys.JrsJobRetentionScheme
    case PfImportedVehicles       => throw new MatchError("Not implemented yet")
    case PfChildBenefitRepayments => TestJourneys.PfChildBenefitRepayments
    case NiEuVatOss               => TestJourneys.NiEuVatOss
    case PfNiEuVatOss             => TestJourneys.PfNiEuVatOss
    case NiEuVatIoss              => TestJourneys.NiEuVatIoss
    case PfNiEuVatIoss            => TestJourneys.PfNiEuVatIoss
    case PfAmls                   => TestJourneys.PfAmls
    case PfAted                   => throw new MatchError("Not implemented yet")
    case PfCdsDeferment           => throw new MatchError("Not implemented yet")
    case PfTrust                  => TestJourneys.PfTrust
    case PtaClass3Ni              => throw new MatchError("Not implemented yet")
    case AlcoholDuty              => TestJourneys.AlcoholDuty
    case PfAlcoholDuty            => TestJourneys.PfAlcoholDuty
    case VatC2c                   => TestJourneys.VatC2c
    case `3psSa`                  => throw new MatchError("Not implemented yet")
    case `3psVat`                 => throw new MatchError("Not implemented yet")
    case PfPillar2                => throw new MatchError("Not implemented yet")
    case PfVatC2c                 => TestJourneys.PfVatC2c
    case Pillar2                  => throw new MatchError("Not implemented yet")
    case WcSa                     => TestJourneys.WcSa
    case WcCt                     => TestJourneys.WcCt
    case WcVat                    => TestJourneys.WcVat
    case WcClass1aNi              => TestJourneys.WcClass1aNi
    case WcXref                   => TestJourneys.WcXref
    case WcEpayeLpp               => TestJourneys.WcEpayeLpp
    case WcEpayeNi                => TestJourneys.WcEpayeNi
    case WcEpayeLateCis           => TestJourneys.WcEpayeLateCis
    case WcEpayeSeta              => TestJourneys.WcEpayeSeta
    case WcChildBenefitRepayments => TestJourneys.WcChildBenefitRepayments
  }

}
