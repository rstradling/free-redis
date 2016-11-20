package com.strad.free.redis.parser

import fastparse.all._
import org.scalatest._

class RedisParserSpec extends FlatSpec with Matchers {
  "RedisParser when given a bulkString" should "parse correctly" in {
    val str = "$3\r\nJOY\r\n"
    val parser = RedisParser.bulk.parse(str)
    parser shouldBe Parsed.Success(BulkString("JOY"), str.length)
  }
  it should "parse when given an empty bulkString" in {
    val str = "$0\r\n\r\n"
    val parser = RedisParser.bulk.parse(str)
    parser shouldBe Parsed.Success(BulkEmpty, str.length)
  }
  it should "parse when given a null bulkString" in {
    val str = "$-1\r\n"
    val parser = RedisParser.bulk.parse(str)
    parser shouldBe Parsed.Success(BulkNull, str.length)
  }

  "Redisparser when given an Integer string" should "parse correctly" in {
    val str = ":55\r\n"
    val parser = RedisParser.integer.parse(str)
    parser shouldBe Parsed.Success(LongResp(55), str.length)
  }
  "RedisParser when given an OK string" should "parse correctly" in {
    val str = "+OK\r\n"
    val parser = RedisParser.simpleString.parse(str)
    parser shouldBe Parsed.Success(SimpleString("OK"), str.length)
  }
  it should "parse correctly empty OK string" in {
    val str = "+\r\n"
    val parser = RedisParser.simpleString.parse(str)
    parser shouldBe Parsed.Success(SimpleString(""), str.length)
  }
  "RedisParser when given an Error string" should "parse correctly" in {
    val str = "-Error message\r\n"
    val parser = RedisParser.error.parse(str)
    parser shouldBe Parsed.Success(Error("Error message"), str.length)
  }
  it should "parse correctly empty Errror string" in {
    val str = "-\r\n"
    val parser = RedisParser.error.parse(str)
    parser shouldBe Parsed.Success(Error(""), str.length)
  }
  "RedisParser redisResp" should "be able to determine error" in {
    val str = "-Error message\r\n"
    val parser = RedisParser.redisResp.parse(str)
    parser shouldBe Parsed.Success(Error("Error message"), str.length)
  }
  "RedisParser redisResp" should "be able to determine simple string" in {
    val str = "+OK\r\n"
    val parser = RedisParser.redisResp.parse(str)
    parser shouldBe Parsed.Success(SimpleString("OK"), str.length)
  }
  "RedisParser redisResp" should "be able to determine an integer" in {
    val str = ":234\r\n"
    val parser = RedisParser.redisResp.parse(str)
    parser shouldBe Parsed.Success(LongResp(234), str.length)
  }
  "RedisParser redisResp" should "be able to handle a BulkString" in {
    val str = "$3\r\nJOY\r\n"
    val parser = RedisParser.redisResp.parse(str)
    parser shouldBe Parsed.Success(BulkString("JOY"), str.length)
  }

}
