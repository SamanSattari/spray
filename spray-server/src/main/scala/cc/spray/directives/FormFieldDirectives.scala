/*
 * Copyright (C) 2011 Mathias Doenitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.spray
package directives

import http._
import typeconversion._

private[spray] trait FormFieldDirectives {
  this: BasicDirectives =>

  private type FM[A] = FieldMatcher[A]         
  
  /**
   * Returns a Route that rejects the request if a form field with the given name cannot be found.
   * If it can be found the parameters value is extracted and passed as argument to the inner Route. 
   */
  def formField[A](fieldMatcher: FM[A]): SprayRoute1[A] = filter1[A] { ctx =>
    val FieldName = fieldMatcher.name
    fieldMatcher(ctx.request.content) match {
      case Right(value) => Pass(value)
      case Left(ContentExpected) => Reject(MissingFormFieldRejection(FieldName))
      case Left(MalformedContent(errorMsg)) => Reject(MalformedFormFieldRejection(errorMsg, Some(FieldName)))
      case Left(UnsupportedContentType(supported)) => Reject(UnsupportedRequestContentTypeRejection(supported))
    }
  }

  /**
   * Returns a Route that rejects the request if a form field with the given name cannot be found.
   * If it can be found the parameters value is extracted and passed as argument to the inner Route.
   */
  def formFields[A](a: FM[A]): SprayRoute1[A] = formField(a)

  /**
   * Returns a Route that rejects the request if the form fields with the given names cannot be found.
   * If it can be found the field values are extracted and passed as arguments to the inner Route.
   */
  def formFields[A, B](a: FM[A], b: FM[B]): SprayRoute2[A, B] = {
    formField(a) & formField(b)
  }  

  /**
   * Returns a Route that rejects the request if the form fields with the given names cannot be found.
   * If it can be found the field values are extracted and passed as arguments to the inner Route.
   */
  def formFields[A, B, C](a: FM[A], b: FM[B], c: FM[C]): SprayRoute3[A, B, C] = {
    formFields(a, b) & formField(c)
  }

  /**
   * Returns a Route that rejects the request if the form fields with the given names cannot be found.
   * If it can be found the field values are extracted and passed as arguments to the inner Route.
   */
  def formFields[A, B, C, D](a: FM[A], b: FM[B], c: FM[C], d: FM[D]): SprayRoute4[A, B, C, D] = {
    formFields(a, b, c) & formField(d)
  }

  /**
   * Returns a Route that rejects the request if the form fields with the given names cannot be found.
   * If it can be found the field values are extracted and passed as arguments to the inner Route.
   */
  def formFields[A, B, C, D, E](a: FM[A], b: FM[B], c: FM[C], d: FM[D], e: FM[E]): SprayRoute5[A, B, C, D, E] = {
    formFields(a, b, c, d) & formField(e)
  }
  
  /**
   * Returns a Route that rejects the request if the form fields with the given names cannot be found.
   * If it can be found the field values are extracted and passed as arguments to the inner Route.
   */
  def formFields[A, B, C, D, E, F](a: FM[A], b: FM[B], c: FM[C], d: FM[D], e: FM[E],
                                   f: FM[F]): SprayRoute6[A, B, C, D, E, F] = {
    formFields(a, b, c, d, e) & formField(f)
  }
  
  /**
   * Returns a Route that rejects the request if the form fields with the given names cannot be found.
   * If it can be found the field values are extracted and passed as arguments to the inner Route.
   */
  def formFields[A, B, C, D, E, F, G](a: FM[A], b: FM[B], c: FM[C], d: FM[D], e: FM[E],
                                      f: FM[F], g: FM[G]): SprayRoute7[A, B, C, D, E, F, G] = {
    formFields(a, b, c, d, e, f) & formField(g)
  }

  /**
   * Returns a Route that rejects the request if the form fields with the given names cannot be found.
   * If it can be found the field values are extracted and passed as arguments to the inner Route.
   */
  def formFields[A, B, C, D, E, F, G, H](a: FM[A], b: FM[B], c: FM[C], d: FM[D], e: FM[E],
                                         f: FM[F], g: FM[G], h: FM[H]): SprayRoute8[A, B, C, D, E, F, G, H] = {
    formFields(a, b, c, d, e, f, g) & formField(h)
  }

  /**
   * Returns a Route that rejects the request if the form fields with the given names cannot be found.
   * If it can be found the field values are extracted and passed as arguments to the inner Route.
   */
  def formFields[A, B, C, D, E, F, G, H, I](a: FM[A], b: FM[B], c: FM[C], d: FM[D], e: FM[E],
                                            f: FM[F], g: FM[G], h: FM[H], i: FM[I]): SprayRoute9[A, B, C, D, E, F, G, H, I] = {
    formFields(a, b, c, d, e, f, g, h) & formField(i)
  }

  implicit def fromSymbol(name: Symbol)(implicit converter: FormFieldConverter[String]) = fromString(name.name)
  implicit def fromString(name: String)(implicit converter: FormFieldConverter[String]) = new FieldMatcher(name)
}

class FieldMatcher[A :FormFieldConverter](val name: String) extends Unmarshaller[A] {

  def apply(content: Option[HttpContent]) = content.formField(name).right.flatMap(_.as[A])

  def ? = as[Option[A]]

  def as[B :FormFieldConverter] = new FieldMatcher[B](name)

  def ? [B](default: B)(implicit convB: FormFieldConverter[B]) = new FieldMatcher[B](name)(
    new FormFieldConverter[B] {
      lazy val urlEncodedFieldConverter = convB.urlEncodedFieldConverter.map(transform)
      lazy val multipartFieldConverter = convB.multipartFieldConverter.map(transform)
      def transform[S](ds: Deserializer[S, B]) = new Deserializer[S, B] {
        def apply(source: S) = ds(source).left.flatMap {
          case ContentExpected => Right(default)
          case error => Left(error)
        }
      }
    }
  )
}