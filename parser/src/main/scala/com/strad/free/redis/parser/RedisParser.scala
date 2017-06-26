/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package com.strad.free.redis.parser

import fastparse.all._

sealed trait RedisResponse
case class SimpleString(s: String) extends RedisResponse
case class Error(s: String) extends RedisResponse
case class LongResp(i: Long) extends RedisResponse

trait BulkResponse extends RedisResponse
case class BulkString(data: String) extends BulkResponse
object BulkEmpty extends BulkResponse
object BulkNull extends BulkResponse

trait ArrayResponse extends RedisResponse
// TODO: Model this correctly as it really should be an Array of ArrayResponse
case class ArrayData(data: List[BulkResponse]) extends ArrayResponse
case class ArrayLong(data: List[LongResp]) extends ArrayResponse
case class ArrayError(data: List[Error]) extends ArrayResponse
case class ArraySimpleStr(data: List[SimpleString]) extends ArrayResponse

object ArrayEmpty extends ArrayResponse
object ArrayNull extends ArrayResponse

object RedisParser {
  def EOL = P("\r" ~ "\n")
  def intSentinel = P(":")
  def errorSentinel = P("-")
  def simpleSentinel = P("+")
  def bulkSentinel = P("$")
  def arraySentinel = P("*")

  def textCanBeEmpty = P(CharsWhile(_ != '\r').rep.!)
  def textNotEmpty = P(CharsWhile(_ != '\r').!)

  // Simple string and error
  def simpleString =
    simpleSentinel ~ textCanBeEmpty.map(s => SimpleString(s)) ~ EOL
  def error = errorSentinel ~ textCanBeEmpty.map(s => Error(s)) ~ EOL
  def integer = intSentinel ~ textNotEmpty.map(s => LongResp(s.toLong)) ~ EOL

  // Bulk
  def numBytes = P(CharIn('0' to '9').rep(1).!).map(_.toInt)
  def emptyBulk = P("$0\r\n\r\n").!.map(x => BulkEmpty)
  def nullBulk = P("$-1\r\n").!.map(x => BulkNull)
  def bulkNonEmptyHeader =
    P(bulkSentinel ~ numBytes ~ EOL).flatMap(count =>
      AnyChars(count).!.map(y => BulkString(y)) ~ EOL)
  def bulk =
    P(emptyBulk | nullBulk | bulkNonEmptyHeader).map((b: BulkResponse) => b)

  // TODO: Handle nested arrays
  // TODO: Handle simple strings and integers
  def emptyArray = P("*0\r\n").!.map(x => ArrayEmpty)
  def nullArray = P("*-1\r\n").!.map(x => ArrayNull)
  def arrayHeader =
    P(arraySentinel ~ numBytes ~ EOL).flatMap(
      count =>
        bulk.rep(exactly = count).map(x => ArrayData(x.toList)) |
          integer.rep(exactly = count).map(x => ArrayLong(x.toList)) |
          error.rep(exactly = count).map(x => ArrayError(x.toList)) |
          simpleString.rep(exactly = count).map(x => ArraySimpleStr(x.toList))
    )
  def array =
    P(emptyArray | nullArray | arrayHeader).map((b: ArrayResponse) => b)

  // Top level parse
  def redisResp = P(simpleString | bulk | integer | error | array)

}
