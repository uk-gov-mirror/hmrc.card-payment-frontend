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

import payapi.cardpaymentjourney.model.journey.SessionId
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.cardpaymentfrontend.actions.{Actions, JourneyRequest}
import uk.gov.hmrc.cardpaymentfrontend.config.AppConfig
import uk.gov.hmrc.cardpaymentfrontend.models.cardpayment.CardPaymentFinishPaymentResponses
import uk.gov.hmrc.cardpaymentfrontend.requests.RequestSupport
import uk.gov.hmrc.cardpaymentfrontend.services.CardPaymentService
import uk.gov.hmrc.cardpaymentfrontend.views.html.errors.TechnicalDifficultiesPage
import uk.gov.hmrc.cardpaymentfrontend.views.html.iframe.{IframeContainerPage, RedirectToParentPage}
import uk.gov.hmrc.http.{HttpResponse, SessionKeys}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrlPolicy.Id
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, RedirectUrl, RedirectUrlPolicy}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class PaymentStatusController @Inject() (
    actions:                   Actions,
    appConfig:                 AppConfig,
    cardPaymentService:        CardPaymentService,
    mcc:                       MessagesControllerComponents,
    requestSupport:            RequestSupport,
    iframeContainer:           IframeContainerPage,
    redirectToParent:          RedirectToParentPage,
    technicalDifficultiesPage: TechnicalDifficultiesPage
)(implicit executionContext: ExecutionContext) extends FrontendController(mcc) with Logging {

  import requestSupport._

  private val redirectUrlPolicy: RedirectUrlPolicy[Id] = AbsoluteWithHostnameFromAllowlist(appConfig.iframeHostNameAllowList)

  def showIframe(iframeUrl: RedirectUrl): Action[AnyContent] = actions.iframeAction { implicit journeyRequest =>
    iframeUrl
      .getEither[Id](redirectUrlPolicy)
      .fold[Result](
        _ => BadRequest("Bad url provided that doesn't match the redirect policy. Check allow list if this is not expected."),
        safeRedirectUrlOnAllowList => Ok(iframeContainer(safeRedirectUrlOnAllowList.url))
      )
  }

  def returnToHmrc(encryptedJourneyId: String): Action[AnyContent] = actions.default { implicit request =>
    Ok(redirectToParent(encryptedJourneyId))
  }

  def paymentStatus(encryptedJourneyId: String): Action[AnyContent] = actions.paymentStatusAction(encryptedJourneyId).async { implicit journeyRequest =>
    val transactionRefFromJourney: Option[String] = journeyRequest.journey.order.map(_.transactionReference.value)
    val sessionIdForJourney: SessionId = journeyRequest.journey.sessionId.getOrElse(throw new RuntimeException("SessionId is missing from journey, this should never happen!"))

    if (journeyRequest.request.headers.get(SessionKeys.sessionId).exists(_ != sessionIdForJourney.value)) {
      logger.warn("sessionId from request is different from sessionId in journey, has the cookie been nuked by the browser?")
    }

    val maybeCardPaymentResultF = for {
      authAndCaptureResult <- cardPaymentService.finishPayment(
        transactionRefFromJourney.getOrElse(throw new RuntimeException("Could not find transaction ref, therefore we can't auth and settle.")),
        journeyRequest.journeyId.value,
        requestSupport.usableLanguage
      )
    } yield authAndCaptureResult

    maybeCardPaymentResultF.flatMap {
      case Some(cardPaymentResult) => cardPaymentResult.cardPaymentResult match {
        case CardPaymentFinishPaymentResponses.Successful => Future.successful(Redirect(routes.PaymentCompleteController.renderPage).addingToSession(SessionKeys.sessionId -> sessionIdForJourney.value))
        case CardPaymentFinishPaymentResponses.Failed     => Future.successful(Redirect(routes.PaymentFailedController.renderPage).addingToSession(SessionKeys.sessionId -> sessionIdForJourney.value))
        case CardPaymentFinishPaymentResponses.Cancelled  => Future.successful(Redirect(routes.PaymentCancelledController.renderPage).addingToSession(SessionKeys.sessionId -> sessionIdForJourney.value))
      }
      case None => tryAndCancelPayment(Some("No cardPaymentResult returned, maybe invalid json returned?"))
    }.recoverWith {
      case exception =>
        logger.error("something went wrong with auth and capture, attempting to cancel payment.")
        tryAndCancelPayment(Some(exception.getMessage))
    }
  }

  private def tryAndCancelPayment(cancelReason: Option[String])(implicit journeyRequest: JourneyRequest[_], messages: Messages): Future[Result] = {
    cardPaymentService.cancelPayment().map { httpResponse: HttpResponse =>
      httpResponse.status match {
        case 200 =>
          logger.warn(s"Successfully cancelled the transaction, but now erroring gracefully because of: [ ${cancelReason.toString} ]")
          InternalServerError(technicalDifficultiesPage()(journeyRequest, messages))
        case _ =>
          logger.warn(s"Something went wrong trying to cancel the transaction. transactionReference: [ ${journeyRequest.journey.order.map(_.transactionReference.value).toString} ]")
          InternalServerError(technicalDifficultiesPage()(journeyRequest, messages))
      }
    }
  }
}
