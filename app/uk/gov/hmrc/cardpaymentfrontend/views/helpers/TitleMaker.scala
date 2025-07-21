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

package uk.gov.hmrc.cardpaymentfrontend.views.helpers

import payapi.corcommon.model.Origin
import play.api.data.Form
import play.api.i18n.Messages

object TitleMaker {

  def journeyTitleMaker(h1Key: Option[String], origin: Option[Origin], maybeForm: Option[Form[_]] = None)(implicit messages: Messages): String = {
    val originEntryName: Option[String] = origin.map(_.entryName)
    makeTitle(h1Key.getOrElse(""), originEntryName.getOrElse("generic"), maybeForm.exists(_.hasErrors))
  }

  private def makeTitle(h1Key: String, origin: String, error: Boolean)(implicit messages: Messages): String = {
    val title = s"""${messages(h1Key)} - ${Messages(s"service-name.${origin}")} - GOV.UK"""
    if (error) s"""${Messages("error.title-prefix")} $title""" else title
  }

}
