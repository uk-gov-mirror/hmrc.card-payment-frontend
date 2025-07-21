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

package uk.gov.hmrc.cardpaymentfrontend.views

import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.cardpaymentfrontend.controllers.FeesController
import uk.gov.hmrc.cardpaymentfrontend.testsupport.ItSpec
import uk.gov.hmrc.cardpaymentfrontend.testsupport.TestOps.FakeRequestOps
import uk.gov.hmrc.cardpaymentfrontend.testsupport.stubs.PayApiStub
import uk.gov.hmrc.cardpaymentfrontend.testsupport.testdata.TestJourneys
import scala.jdk.CollectionConverters.CollectionHasAsScala

class AccessibilityStatementSpec extends ItSpec {

  private val feesController: FeesController = app.injector.instanceOf[FeesController]

  "Accessibility Statement Link is correct" in {

    val fakeRequest = FakeRequest("GET", "/card-fees").withSessionId()

    PayApiStub.stubForFindBySessionId2xx(TestJourneys.PfSa.journeyBeforeBeginWebPayment)
    val result = feesController.renderPage(fakeRequest)
    val document = Jsoup.parse(contentAsString(result))
    println(document)
    val accessibilityItem = document.select("ul.govuk-footer__inline-list li.govuk-footer__inline-list-item a").asScala.find(_.text().trim == "Accessibility statement")
    accessibilityItem.flatMap(item => Option(item.attr("href"))) shouldBe Some("http://localhost:12346/accessibility-statement/pay?referrerUrl=%2Fcard-fees")
  }

}
