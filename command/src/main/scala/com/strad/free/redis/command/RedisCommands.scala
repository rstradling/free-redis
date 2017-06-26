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
  def command: String
}
sealed trait Hash extends Product with Serializable
case class Hset(key: RedisType, field: String, value: RedisType)
    extends Hash
    with Command {
  override val command: String = "HSET"
}
case class Hget(key: RedisType, field: String) extends Hash with Command {
  override val command: String = "HGET"
}

object Hset {
  def apply(key: String, field: String, value: String): Command = {
    Hset(RedisStr(key), field, RedisStr(value))
  }
  def apply(key: String, field: String, value: Long): Command = {
    Hset(RedisStr(key), field, RedisLong(value))
  }
}

trait BuildCommand[A] {
  def commandStr(cmd: A): String
}

object RedisCommand {
  def numArray(count: Long): String = {
    "*" + count.toString + "\r\n"
  }
  def command(cmd: Command): String = {
    "$" + cmd.command.length + "\r\n" + cmd.command + "\r\n"
  }

  def commandStr[A](a: A)(implicit ev: BuildCommand[A]): String = {
    ev.commandStr(a)
  }

  implicit val cmd = new BuildCommand[Command] {
    def commandStr(cmd: Command): String = {
      cmd match {
        case item @ Hset(k, f, v) => hashSetCommandImpl.commandStr(item)
        case item @ Hget(k, f) => hashGetCommandImpl.commandStr(item)
      }
    }
  }

  implicit val hashSetCommandImpl = new BuildCommand[Hset] {
    def commandStr(cmd: Hset): String = {
      val arrayNum = numArray(4)
      val c = command(cmd)
      val k = cmd.key.toRedisStr
      val f = RedisStr(cmd.field).toRedisStr
      val v = cmd.value.toRedisStr
      val ret = s"${arrayNum}${c}${k}${f}${v}"
      ret
    }
  }

  implicit val hashGetCommandImpl = new BuildCommand[Hget] {
    def commandStr(cmd: Hget): String = {
      s"${numArray(3)}${command(cmd)}${cmd.key.toRedisStr}${RedisStr(cmd.field).toRedisStr}"
    }
  }
}
