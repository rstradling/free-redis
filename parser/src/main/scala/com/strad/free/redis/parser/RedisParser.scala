package com.strad.free.redis.parser

import fastparse.all._

sealed trait RedisResponses
case class SimpleString(s: String) extends RedisResponses
case class Error(s: String) extends RedisResponses
case class LongResp(i: Long) extends RedisResponses

object RedisParser {
  def EOL = P("\r" ~ "\n")
  def intSentinel = P(":")
  def errorSentinel = P("-")
  def simpleSentinel = P("+")
  def textCanBeEmpty = P(CharsWhile(_ != '\r').rep.!)
  def textNotEmpty = P(CharsWhile(_ != '\r').!)

  def simpleString = simpleSentinel ~ textCanBeEmpty.map(s => SimpleString(s)) ~ EOL
  def error = errorSentinel ~ textCanBeEmpty.map(s => Error(s)) ~ EOL
  def integer = intSentinel ~ textNotEmpty.map(s => LongResp(s.toLong)) ~ EOL

  def redisResp = P(simpleString | integer | error)

}
