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

import play.api.Logging
import play.api.mvc.{ActionRefiner, Result, Results}
import uk.gov.hmrc.cardpaymentfrontend.config.ErrorHandler

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyFinishedActionRefiner @Inject() (errorHandler: ErrorHandler)(implicit ec: ExecutionContext) extends ActionRefiner[JourneyRequest, JourneyRequest] with Logging {

  override protected[actions] def refine[A](request: JourneyRequest[A]): Future[Either[Result, JourneyRequest[A]]] = {
    if (request.journey.status.isTerminalState) Future.successful(Right(request))
    else {
      logger.warn(s"Journey is not in valid state: ${request.journey.status.entryName}, expected terminal state.")
      errorHandler
        .notFoundTemplate(request)
        .map(html => Left(Results.Gone(html)))
    }
  }

  override protected def executionContext: ExecutionContext = ec
}
