package com.strad.free.redis.command

import org.scalatest._

class RedisCommandSpec extends FlatSpec with Matchers {
  import com.strad.free.redis.command.RedisCommand._
  "hashSet" should "return the proper redis string" in {
    val hset = Hset(RedisStr("key"), "myfield", RedisLong(32L))
    RedisCommand.commandStr(hset) shouldBe "*4\r\n$4\r\nHSET\r\n$3\r\nkey\r\n$7\r\nmyfield\r\n$2\r\n32\r\n"
  }
  "hashGet" should "return the proper redis string" in {
    val hget = Hget(RedisStr("key"), "myfield")
    RedisCommand.commandStr(hget) shouldBe "*3\r\n$4\r\nHGET\r\n$3\r\nkey\r\n$7\r\nmyfield\r\n"
  }
}
