package com.strad.free.redis.parser

import fastparse.all._

sealed trait RedisResponses
case class SimpleString(s: String) extends RedisResponses
case class Error(s: String) extends RedisResponses
case class LongResp(i: Long) extends RedisResponses
trait BulkResponses extends RedisResponses
case class BulkString(data: String) extends BulkResponses
object BulkEmpty extends BulkResponses
object BulkNull extends BulkResponses

object RedisParser {
  def EOL = P("\r" ~ "\n")
  def intSentinel = P(":")
  def errorSentinel = P("-")
  def simpleSentinel = P("+")
  def bulkSentinel = P("$")
  def textCanBeEmpty = P(CharsWhile(_ != '\r').rep.!)
  def textNotEmpty = P(CharsWhile(_ != '\r').!)

  def simpleString = simpleSentinel ~ textCanBeEmpty.map(s => SimpleString(s)) ~ EOL
  def error = errorSentinel ~ textCanBeEmpty.map(s => Error(s)) ~ EOL
  def integer = intSentinel ~ textNotEmpty.map(s => LongResp(s.toLong)) ~ EOL
  def numBytes = P(CharIn('0' to '9').rep(1).!).map(_.toInt)

  def emptyBulk = P("$0\r\n\r\n").!.map(x => BulkEmpty)
  def nullBulk = P("$-1\r\n").!.map(x => BulkNull)
  def bulkNonEmptyHeader = P(bulkSentinel ~ numBytes ~ EOL).flatMap(count => AnyChars(count).!.map(y => BulkString(y)) ~ EOL)
  def bulk = P(emptyBulk | nullBulk | bulkNonEmptyHeader).map((b: BulkResponses) => b)

  def redisResp = P(simpleString | bulk | integer | error)

}
