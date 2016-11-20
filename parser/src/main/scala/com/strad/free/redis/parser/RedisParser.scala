package com.strad.free.redis.parser

import fastparse.all._

sealed trait RedisResponses
case class SimpleString(s: String) extends RedisResponses
case class Error(s: String) extends RedisResponses

object RedisParser {
  def EOL = P("\r" ~ "\n")
  def errorSentinel = P("-")
  def simpleSentinel = P("+")
  def text = P(CharsWhile(_ != '\r').rep.!)

  def simpleString = simpleSentinel ~ text.map(s => SimpleString(s)) ~ EOL
  def error = errorSentinel ~ text.map(s => Error(s)) ~ EOL

  def redisResp = P(simpleString | error)

}
