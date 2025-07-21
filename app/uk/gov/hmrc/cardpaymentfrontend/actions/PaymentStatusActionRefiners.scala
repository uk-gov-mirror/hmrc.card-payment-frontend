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

package uk.gov.hmrc.cardpaymentfrontend.actions

import payapi.corcommon.model.PaymentStatuses
import play.api.Logging
import play.api.mvc.{ActionRefiner, Result, Results}
import uk.gov.hmrc.cardpaymentfrontend.config.ErrorHandler

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentStatusActionRefiners @Inject() (
    errorHandler: ErrorHandler
)(implicit ec: ExecutionContext) extends Logging {

  def paymentStatusActionRefiner: ActionRefiner[JourneyRequest, JourneyRequest] = new ActionRefiner[JourneyRequest, JourneyRequest] {
    //we don't want to be calling payment status again if we know that the journey is already in finished state
    override protected[actions] def refine[A](journeyRequest: JourneyRequest[A]): Future[Either[Result, JourneyRequest[A]]] = {
      journeyRequest.journey.status match {
        case PaymentStatuses.Sent => Future.successful(Right(journeyRequest))
        case PaymentStatuses.Successful => redirectToPaymentComplete(journeyRequest)
        case PaymentStatuses.Failed => redirectToPaymentFailed(journeyRequest)
        case PaymentStatuses.Cancelled => redirectToPaymentCancelled(journeyRequest)
        case PaymentStatuses.Created | PaymentStatuses.Validated | PaymentStatuses.SoftDecline => errorOut(journeyRequest)
      }
    }

    override protected def executionContext: ExecutionContext = ec
  }

  def iframePageActionRefiner: ActionRefiner[JourneyRequest, JourneyRequest] = new ActionRefiner[JourneyRequest, JourneyRequest] {
    //we don't want to be calling show iframe again if we know that the journey is already in finished state
    override protected[actions] def refine[A](journeyRequest: JourneyRequest[A]): Future[Either[Result, JourneyRequest[A]]] = {
      journeyRequest.journey.status match {
        case PaymentStatuses.Created | PaymentStatuses.Sent          => Future.successful(Right(journeyRequest))
        case PaymentStatuses.Successful                              => redirectToPaymentComplete(journeyRequest)
        case PaymentStatuses.Failed                                  => redirectToPaymentFailed(journeyRequest)
        case PaymentStatuses.Cancelled                               => redirectToPaymentCancelled(journeyRequest)
        case PaymentStatuses.Validated | PaymentStatuses.SoftDecline => errorOut(journeyRequest)
      }
    }

    override protected def executionContext: ExecutionContext = ec
  }

  private def errorOut[A]: JourneyRequest[A] => Future[Left[Result, Nothing]] = journeyRequest =>
    logAndReturnF(journeyRequest, Results.BadRequest(errorHandler.technicalDifficulties()(journeyRequest)))

  private def redirectToPaymentComplete[A]: JourneyRequest[A] => Future[Left[Result, Nothing]] = journeyRequest =>
    logAndReturnF(journeyRequest, Results.Redirect(uk.gov.hmrc.cardpaymentfrontend.controllers.routes.PaymentCompleteController.renderPage))

  private def redirectToPaymentCancelled[A]: JourneyRequest[A] => Future[Left[Result, Nothing]] = journeyRequest =>
    logAndReturnF(journeyRequest, Results.Redirect(uk.gov.hmrc.cardpaymentfrontend.controllers.routes.PaymentCancelledController.renderPage))

  private def redirectToPaymentFailed[A]: JourneyRequest[A] => Future[Left[Result, Nothing]] = journeyRequest =>
    logAndReturnF(journeyRequest, Results.Redirect(uk.gov.hmrc.cardpaymentfrontend.controllers.routes.PaymentFailedController.renderPage))

  private def logAndReturnF[A](journeyRequest: JourneyRequest[A], f: => Result): Future[Left[Result, Nothing]] = {
    logger.warn(s"Trying to call ${journeyRequest.request.path} endpoint when journey is in state [${journeyRequest.journey.status.entryName}], but it should be in state [Sent]")
    Future.successful(Left(f))
  }

}
