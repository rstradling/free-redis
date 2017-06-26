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

package com.strad.free.redis.command

sealed trait RedisType {
  def toRedisStr: String
}
case class RedisLong(value: Long) extends RedisType {
  def toRedisStr: String = {
    "$" + value.toString.length + "\r\n" + value + "\r\n"
  }
}
case class RedisStr(value: String) extends RedisType {
  def toRedisStr: String = {
    "$" + value.length + "\r\n" + value + "\r\n"
  }
}

sealed trait Command extends Product with Serializable {
  def numArray(count: Long): String = {
    "*" + count.toString + "\r\n"
  }
  def command(cmd: Command): String = {
    "$" + cmd.command.length + "\r\n" + cmd.command + "\r\n"
  }
  def command: String
  def commandStr: String
}

sealed trait Hash extends Product with Serializable
case class Hset(key: RedisType, field: String, value: RedisType)
    extends Hash
    with Command {
  override val command: String = "HSET"
  def commandStr: String = {
    val arrayNum = numArray(4)
    val c = command(this)
    val k = this.key.toRedisStr
    val f = RedisStr(this.field).toRedisStr
    val v = this.value.toRedisStr
    val ret = s"${arrayNum}${c}${k}${f}${v}"
    ret
  }

}
case class Hget(key: RedisType, field: String) extends Hash with Command {
  override val command: String = "HGET"
  def commandStr: String = {
    s"${numArray(3)}${command(this)}${this.key.toRedisStr}${RedisStr(this.field).toRedisStr}"
  }
}

object Hset {
  def apply(key: String, field: String, value: String): Command = {
    Hset(RedisStr(key), field, RedisStr(value))
  }
  def apply(key: String, field: String, value: Long): Command = {
    Hset(RedisStr(key), field, RedisLong(value))
  }
}
