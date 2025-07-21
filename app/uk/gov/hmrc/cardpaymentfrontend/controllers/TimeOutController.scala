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

package uk.gov.hmrc.cardpaymentfrontend.controllers

import com.google.inject.Singleton
import payapi.cardpaymentjourney.model.journey.Url
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.cardpaymentfrontend.actions.Actions
import uk.gov.hmrc.cardpaymentfrontend.config.AppConfig
import uk.gov.hmrc.cardpaymentfrontend.views.html.{DeleteAnswersPage, ForceDeleteAnswersPage}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject

@Singleton
final class TimeOutController @Inject() (
    actions:                Actions,
    appConfig:              AppConfig,
    mcc:                    MessagesControllerComponents,
    deleteAnswersPage:      DeleteAnswersPage,
    forceDeleteAnswersPage: ForceDeleteAnswersPage
) extends FrontendController(mcc) with I18nSupport {

  def showDeleteAnswersPage: Action[AnyContent] =
    actions.journeyAction { implicit request =>
      Ok(deleteAnswersPage()).withNewSession
    }

  def showForceDeleteAnswersLoggedOutPage: Action[AnyContent] =
    actions.default { implicit request =>
      val redirectUrl = Some(Url(s"${appConfig.payFrontendBaseUrl}"))
      Ok(forceDeleteAnswersPage(loggedIn = false, redirectUrl)).withNewSession
    }

  def showForceDeleteAnswersLoggedInPage: Action[AnyContent] =
    actions.default { implicit request =>
      val continueUrl: String = "%2Ffeedback%2Fpay-online"
      val redirectUrl = Some(Url(s"${appConfig.signOutUrl}?continue=$continueUrl"))
      Ok(forceDeleteAnswersPage(loggedIn = true, redirectUrl)).withNewSession
    }

}
