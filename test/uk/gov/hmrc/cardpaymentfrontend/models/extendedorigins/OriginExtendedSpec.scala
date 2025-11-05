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

import org.scalatest.AppendedClues.convertToClueful
import payapi.corcommon.model.Origin
import payapi.corcommon.model.Origins._
import uk.gov.hmrc.cardpaymentfrontend.testsupport.{TestHelpers, UnitSpec}

class OriginExtendedSpec extends UnitSpec {

  "implicit class OriginExtended" - {
    "isAWebChatOrigin should return" - {
      val originsSeq = Seq[(Origin, Boolean)](
        WcSa -> true,
        WcCt -> true,
        WcVat -> true,
        WcClass1aNi -> true,
        WcSimpleAssessment -> true,
        WcXref -> true,
        WcEpayeLpp -> true,
        WcEpayeNi -> true,
        WcEpayeLateCis -> true,
        WcEpayeSeta -> true,
        WcClass2Ni -> true,
        PfSa -> false,
        BtaSa -> false,
        PtaSa -> false,
        ItSa -> false,
        PfAlcoholDuty -> false,
        AlcoholDuty -> false,
        PfCt -> false,
        BtaCt -> false,
        PfEpayeNi -> false,
        PfEpayeLpp -> false,
        PfEpayeLateCis -> false,
        PfEpayeP11d -> false,
        PfEpayeSeta -> false,
        PfVat -> false,
        BtaVat -> false,
        VcVatReturn -> false,
        VcVatOther -> false,
        Ppt -> false,
        PfPpt -> false,
        BtaEpayeBill -> false,
        BtaEpayePenalty -> false,
        BtaEpayeInterest -> false,
        BtaEpayeGeneral -> false,
        BtaClass1aNi -> false,
        PfAmls -> false,
        Amls -> false,
        CapitalGainsTax -> false,
        EconomicCrimeLevy -> false,
        PfEconomicCrimeLevy -> false,
        PfSdlt -> false,
        VatC2c -> false,
        PfVatC2c -> false,
        PfChildBenefitRepayments -> false,
        BtaSdil -> false,
        PfSdil -> false,
        PfSimpleAssessment -> false,
        PtaSimpleAssessment -> false,
        PfP800 -> false,
        PtaP800 -> false,
        PfCds -> false,
        PfOther -> false,
        PfClass2Ni -> false,
        PfInsurancePremium -> false,
        PfPsAdmin -> false,
        AppSa -> false,
        BcPngr -> false,
        Parcels -> false,
        DdVat -> false,
        DdSdil -> false,
        PfCdsCash -> false,
        PfSpiritDrinks -> false,
        PfInheritanceTax -> false,
        Mib -> false,
        PfClass3Ni -> false,
        PfWineAndCider -> false,
        PfBioFuels -> false,
        PfAirPass -> false,
        PfMgd -> false,
        PfBeerDuty -> false,
        PfGamingOrBingoDuty -> false,
        PfGbPbRgDuty -> false,
        PfLandfillTax -> false,
        PfAggregatesLevy -> false,
        PfClimateChangeLevy -> false,
        AppSimpleAssessment -> false,
        PfTpes -> false,
        PfJobRetentionScheme -> false,
        JrsJobRetentionScheme -> false,
        PfImportedVehicles -> false,
        NiEuVatOss -> false,
        PfNiEuVatOss -> false,
        NiEuVatIoss -> false,
        PfNiEuVatIoss -> false,
        PfAted -> false,
        PfCdsDeferment -> false,
        PfTrust -> false,
        PtaClass3Ni -> false,
        `3psSa` -> false,
        `3psVat` -> false,
        PfPillar2 -> false,
        Pillar2 -> false
      )
      originsSeq.foreach {
        case (origin, isAWebChatOrigin) =>
          s"isAWebChatOrigin: ${isAWebChatOrigin.toString} for origin ${origin.toString}" in {
            ExtendedOrigin.OriginExtended(origin).isAWebChatOrigin shouldBe isAWebChatOrigin
          }
      }
      "all origins should be covered" in {
        TestHelpers.implementedOrigins ++ TestHelpers.unimplementedOrigins should contain theSameElementsAs originsSeq.map(_._1) withClue "** add new origin to seq above **"
      }
    }

    "originSupportsWelsh should return" - {
      val originsSeq = Seq[(Origin, Boolean)](
        PfPillar2 -> false,
        Pillar2 -> false,
        PfCds -> false,
        PfCdsCash -> false,
        PfCdsDeferment -> false,
        Parcels -> false,
        NiEuVatOss -> false,
        PfNiEuVatOss -> false,
        NiEuVatIoss -> false,
        PfNiEuVatIoss -> false,
        WcSa -> true,
        WcCt -> true,
        WcVat -> true,
        WcClass1aNi -> true,
        WcSimpleAssessment -> true,
        WcXref -> true,
        WcEpayeLpp -> true,
        WcEpayeNi -> true,
        WcEpayeLateCis -> true,
        WcEpayeSeta -> true,
        WcClass2Ni -> true,
        PfSa -> true,
        BtaSa -> true,
        PtaSa -> true,
        ItSa -> true,
        PfAlcoholDuty -> true,
        AlcoholDuty -> true,
        PfCt -> true,
        BtaCt -> true,
        PfEpayeNi -> true,
        PfEpayeLpp -> true,
        PfEpayeLateCis -> true,
        PfEpayeP11d -> true,
        PfEpayeSeta -> true,
        PfVat -> true,
        BtaVat -> true,
        VcVatReturn -> true,
        VcVatOther -> true,
        Ppt -> true,
        PfPpt -> true,
        BtaEpayeBill -> true,
        BtaEpayePenalty -> true,
        BtaEpayeInterest -> true,
        BtaEpayeGeneral -> true,
        BtaClass1aNi -> true,
        PfAmls -> true,
        Amls -> true,
        CapitalGainsTax -> true,
        EconomicCrimeLevy -> true,
        PfEconomicCrimeLevy -> true,
        PfSdlt -> true,
        VatC2c -> true,
        PfVatC2c -> true,
        PfChildBenefitRepayments -> true,
        BtaSdil -> true,
        PfSdil -> true,
        PfSimpleAssessment -> true,
        PtaSimpleAssessment -> true,
        PfP800 -> true,
        PtaP800 -> true,
        PfOther -> true,
        PfClass2Ni -> true,
        PfInsurancePremium -> true,
        PfPsAdmin -> true,
        AppSa -> true,
        BcPngr -> true,
        DdVat -> true,
        DdSdil -> true,
        PfSpiritDrinks -> true,
        PfInheritanceTax -> true,
        Mib -> true,
        PfClass3Ni -> true,
        PfWineAndCider -> true,
        PfBioFuels -> true,
        PfAirPass -> true,
        PfMgd -> true,
        PfBeerDuty -> true,
        PfGamingOrBingoDuty -> true,
        PfGbPbRgDuty -> true,
        PfLandfillTax -> true,
        PfAggregatesLevy -> true,
        PfClimateChangeLevy -> true,
        AppSimpleAssessment -> true,
        PfTpes -> true,
        PfJobRetentionScheme -> true,
        JrsJobRetentionScheme -> true,
        PfImportedVehicles -> true,
        PfAted -> true,
        PfTrust -> true,
        PtaClass3Ni -> true,
        `3psSa` -> true,
        `3psVat` -> true
      )
      originsSeq.foreach {
        case (origin, welshIsSupported) =>
          s"welsh supported: ${welshIsSupported.toString} for origin ${origin.toString}" in {
            ExtendedOrigin.OriginExtended(origin).originSupportsWelsh shouldBe welshIsSupported
          }
      }
      "all origins should be covered" in {
        TestHelpers.implementedOrigins ++ TestHelpers.unimplementedOrigins should contain theSameElementsAs originsSeq.map(_._1) withClue "** add new origin to seq above **"
      }
    }
  }

}
