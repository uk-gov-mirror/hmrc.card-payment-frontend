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

import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.TableFor2
import org.scalatest.prop.Tables.Table
import payapi.cardpaymentjourney.model.journey.{Journey, JourneySpecificData}
import payapi.corcommon.model.Origins
import uk.gov.hmrc.cardpaymentfrontend.models.Languages
import uk.gov.hmrc.cardpaymentfrontend.models.cardpayment.{ClientId, ClientIds}
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestHelpers.implementedOrigins
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys
import uk.gov.hmrc.cardpaymentfrontend.testsupport.{ItSpec, TestHelpers}

class ClientIdServiceSpec extends ItSpec {

  private val systemUnderTest = app.injector.instanceOf[ClientIdService]

  "ClientIdService" - {

    val scenariosEn: TableFor2[Journey[JourneySpecificData], ClientId] = Table(
      ("test journey", "expected client id"),
      (TestJourneys.PfSa.journeyBeforeBeginWebPayment, ClientIds.SAEE),
      (TestJourneys.BtaSa.journeyBeforeBeginWebPayment, ClientIds.SAEE),
      (TestJourneys.PtaSa.journeyBeforeBeginWebPayment, ClientIds.SAEE),
      (TestJourneys.ItSa.journeyBeforeBeginWebPayment, ClientIds.SAEE),
      (TestJourneys.WcSa.journeyBeforeBeginWebPayment, ClientIds.SAEE),
      (TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment, ClientIds.ETEE),
      (TestJourneys.AlcoholDuty.journeyBeforeBeginWebPayment, ClientIds.ETEE),
      (TestJourneys.BtaCt.journeyBeforeBeginWebPayment, ClientIds.COEE),
      (TestJourneys.PfCt.journeyBeforeBeginWebPayment, ClientIds.COEE),
      (TestJourneys.WcCt.journeyBeforeBeginWebPayment, ClientIds.COEE),
      (TestJourneys.PfEpayeNi.journeyBeforeBeginWebPayment, ClientIds.PAEE),
      (TestJourneys.PfEpayeLpp.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfEpayeP11d.journeyBeforeBeginWebPayment, ClientIds.PAEE),
      (TestJourneys.PfEpayeLateCis.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfEpayeSeta.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfVat.journeyBeforeBeginWebPayment, ClientIds.VAEE),
      (TestJourneys.PfVatWithChargeReference.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.BtaVat.journeyBeforeBeginWebPayment, ClientIds.VAEE),
      (TestJourneys.WcVat.journeyBeforeBeginWebPayment, ClientIds.VAEE),
      (TestJourneys.WcVatWithChargeReference.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment, ClientIds.VAEE),
      (TestJourneys.VcVatOther.journeyBeforeBeginWebPayment, ClientIds.VAEE),
      (TestJourneys.Ppt.journeyBeforeBeginWebPayment, ClientIds.ETEE),
      (TestJourneys.PfPpt.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment, ClientIds.PAEE),
      (TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment, ClientIds.PAEE),
      (TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment, ClientIds.PAEE),
      (TestJourneys.Amls.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfAmls.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfSdlt.journeyBeforeBeginWebPayment, ClientIds.SDEE),
      (TestJourneys.CapitalGainsTax.journeyBeforeBeginWebPayment, ClientIds.ETEE),
      (TestJourneys.EconomicCrimeLevy.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfEconomicCrimeLevy.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.VatC2c.journeyBeforeBeginWebPayment, ClientIds.PLPE),
      (TestJourneys.PfVatC2c.journeyBeforeBeginWebPayment, ClientIds.PLPE),
      (TestJourneys.WcSimpleAssessment.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.WcXref.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment, ClientIds.PAEE),
      (TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment, ClientIds.PAEE),
      (TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment, ClientIds.CBEE),
      (TestJourneys.BtaSdil.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfSdil.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.NiEuVatOss.journeyBeforeBeginWebPayment, ClientIds.OSEE),
      (TestJourneys.PfNiEuVatOss.journeyBeforeBeginWebPayment, ClientIds.OSEE),
      (TestJourneys.NiEuVatIoss.journeyBeforeBeginWebPayment, ClientIds.OSEE),
      (TestJourneys.PfNiEuVatIoss.journeyBeforeBeginWebPayment, ClientIds.OSEE),
      (TestJourneys.PfP800.journeyBeforeBeginWebPayment, ClientIds.ETEE),
      (TestJourneys.PtaP800.journeyBeforeBeginWebPayment, ClientIds.ETEE),
      (TestJourneys.PfSimpleAssessment.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PtaSimpleAssessment.journeyBeforeBeginWebPayment, ClientIds.ETEE),
      (TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfCds.journeyBeforeBeginWebPayment, ClientIds.CDEE),
      (TestJourneys.AppSa.journeyBeforeBeginWebPayment, ClientIds.SAEE),
      (TestJourneys.AppSimpleAssessment.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.Mib.journeyBeforeBeginWebPayment, ClientIds.MBPE),
      (TestJourneys.BcPngr.journeyBeforeBeginWebPayment, ClientIds.PSEE),
      (TestJourneys.PfTpes.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfMgd.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfGbPbRgDuty.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfTrust.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfPsAdmin.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.PfOther.journeyBeforeBeginWebPayment, ClientIds.MIEE),
      (TestJourneys.WcChildBenefitRepayments.journeyBeforeBeginWebPayment, ClientIds.CBEE)
    )

    forAll(scenariosEn) {
      case (journey, clientId) =>
        s"should return the correct client id: ${clientId.prodCode} for ${journey.origin.entryName} when language is english" in {
          systemUnderTest.determineClientId(journey, Languages.English) shouldBe clientId withClue s"check scenario for origin ${journey.origin.toString}"
        }
    }

    implementedOrigins.foreach { origin =>
      s"for journey with origin ${origin.entryName}, test scenario should exist in English" in {
        scenariosEn.exists { scenario =>
          scenario._1.origin == origin
        } shouldBe true withClue s"Missing test scenario for origin ${origin.entryName}"
      }
    }

    val scenariosCy: TableFor2[Journey[JourneySpecificData], ClientId] = Table(
      ("test journey", "expected client id"),
      (TestJourneys.PfSa.journeyBeforeBeginWebPayment, ClientIds.SAEC),
      (TestJourneys.BtaSa.journeyBeforeBeginWebPayment, ClientIds.SAEC),
      (TestJourneys.PtaSa.journeyBeforeBeginWebPayment, ClientIds.SAEC),
      (TestJourneys.ItSa.journeyBeforeBeginWebPayment, ClientIds.SAEC),
      (TestJourneys.WcSa.journeyBeforeBeginWebPayment, ClientIds.SAEC),
      (TestJourneys.PfAlcoholDuty.journeyBeforeBeginWebPayment, ClientIds.ETEC),
      (TestJourneys.AlcoholDuty.journeyBeforeBeginWebPayment, ClientIds.ETEC),
      (TestJourneys.BtaCt.journeyBeforeBeginWebPayment, ClientIds.COEC),
      (TestJourneys.PfCt.journeyBeforeBeginWebPayment, ClientIds.COEC),
      (TestJourneys.WcCt.journeyBeforeBeginWebPayment, ClientIds.COEC),
      (TestJourneys.PfEpayeNi.journeyBeforeBeginWebPayment, ClientIds.PAEC),
      (TestJourneys.PfEpayeLpp.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfEpayeP11d.journeyBeforeBeginWebPayment, ClientIds.PAEC),
      (TestJourneys.PfEpayeLateCis.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfEpayeSeta.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfVat.journeyBeforeBeginWebPayment, ClientIds.VAEC),
      (TestJourneys.PfVatWithChargeReference.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.BtaVat.journeyBeforeBeginWebPayment, ClientIds.VAEC),
      (TestJourneys.WcVat.journeyBeforeBeginWebPayment, ClientIds.VAEC),
      (TestJourneys.WcVatWithChargeReference.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.VcVatReturn.journeyBeforeBeginWebPayment, ClientIds.VAEC),
      (TestJourneys.VcVatOther.journeyBeforeBeginWebPayment, ClientIds.VAEC),
      (TestJourneys.Ppt.journeyBeforeBeginWebPayment, ClientIds.ETEC),
      (TestJourneys.PfPpt.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.BtaEpayeBill.journeyBeforeBeginWebPayment, ClientIds.PAEC),
      (TestJourneys.BtaEpayeGeneral.journeyBeforeBeginWebPayment, ClientIds.PAEC),
      (TestJourneys.BtaEpayePenalty.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.BtaEpayeInterest.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.BtaClass1aNi.journeyBeforeBeginWebPayment, ClientIds.PAEC),
      (TestJourneys.Amls.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfAmls.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfSdlt.journeyBeforeBeginWebPayment, ClientIds.SDEC),
      (TestJourneys.CapitalGainsTax.journeyBeforeBeginWebPayment, ClientIds.ETEC),
      (TestJourneys.EconomicCrimeLevy.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfEconomicCrimeLevy.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.VatC2c.journeyBeforeBeginWebPayment, ClientIds.PLPC),
      (TestJourneys.PfVatC2c.journeyBeforeBeginWebPayment, ClientIds.PLPC),
      (TestJourneys.WcSimpleAssessment.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.WcXref.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.WcEpayeLpp.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.WcClass1aNi.journeyBeforeBeginWebPayment, ClientIds.PAEC),
      (TestJourneys.WcEpayeNi.journeyBeforeBeginWebPayment, ClientIds.PAEC),
      (TestJourneys.WcEpayeLateCis.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.WcEpayeSeta.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfChildBenefitRepayments.journeyBeforeBeginWebPayment, ClientIds.CBEC),
      (TestJourneys.BtaSdil.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfSdil.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.NiEuVatOss.journeyBeforeBeginWebPayment, ClientIds.OSEE),
      (TestJourneys.PfNiEuVatOss.journeyBeforeBeginWebPayment, ClientIds.OSEE),
      (TestJourneys.NiEuVatIoss.journeyBeforeBeginWebPayment, ClientIds.OSEE),
      (TestJourneys.PfNiEuVatIoss.journeyBeforeBeginWebPayment, ClientIds.OSEE),
      (TestJourneys.PfP800.journeyBeforeBeginWebPayment, ClientIds.ETEC),
      (TestJourneys.PtaP800.journeyBeforeBeginWebPayment, ClientIds.ETEC),
      (TestJourneys.PfSimpleAssessment.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PtaSimpleAssessment.journeyBeforeBeginWebPayment, ClientIds.ETEC),
      (TestJourneys.PfJobRetentionScheme.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.JrsJobRetentionScheme.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.AppSa.journeyBeforeBeginWebPayment, ClientIds.SAEC),
      (TestJourneys.AppSimpleAssessment.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.Mib.journeyBeforeBeginWebPayment, ClientIds.MBPE), // only one client id for mib/mods, but supports welsh? We are where we are.
      (TestJourneys.BcPngr.journeyBeforeBeginWebPayment, ClientIds.PSEC),
      (TestJourneys.PfTpes.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfMgd.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfGbPbRgDuty.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfTrust.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfPsAdmin.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.PfOther.journeyBeforeBeginWebPayment, ClientIds.MIEC),
      (TestJourneys.WcChildBenefitRepayments.journeyBeforeBeginWebPayment, ClientIds.CBEC)
    )

    forAll(scenariosCy) {
      case (journey, clientId) =>
        s"should return the correct client id: ${clientId.prodCode} for ${journey.origin.entryName} when language is welsh" in {
          systemUnderTest.determineClientId(journey, Languages.Welsh) shouldBe clientId withClue s"check scenario for origin ${journey.origin.toString}"
        }
    }

    implementedOrigins
      .filterNot(_ === Origins.PfCds) // cds doesn't have welsh
      .foreach { origin =>
        s"for journey with origin ${origin.entryName}, test scenario should exist in welsh" in {
          scenariosCy.exists { scenario =>
            scenario._1.origin == origin
          } shouldBe true withClue s"Missing test scenario for origin ${origin.entryName}"
        }
      }
  }

  "sanity check for implemented origins" in {
    TestHelpers.implementedOrigins.size shouldBe 67 withClue "** This dummy test is here to remind you to update the tests above. Bump up the expected number when an origin is added to implemented origins **"
  }

}
