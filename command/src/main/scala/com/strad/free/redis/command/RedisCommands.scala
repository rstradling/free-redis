package com.strad.free.redis.command

sealed trait RedisType {
  def toRedisStr: String
}
case class RedisLong(value: Long) extends RedisType {
  def toRedisStr: String = {
    s":${value}\r\n"
  }
}
case class RedisStr(value: String) extends RedisType {
  def toRedisStr: String = {
    "$" + value.length + "\r\n" + value + "\r\n"
  }
}

sealed trait Command {
  def command: String
}
sealed trait Hash extends Command
case class Hset(key: RedisType, field: String, value: RedisType) extends Hash {
  override val command: String = "HSET"
}
case class Hget(key: RedisType, field: String) extends Hash {
  override val command: String = "HGET"
}

trait BuildCommand[A <: Command] {
  def commandStr(cmd: A): String
}

object RedisCommand {
  def numArray(count: Long): String = {
    "*" + count.toString + "\r\n"
  }
  def command(cmd: Command): String = {
    "$" + cmd.command.length + "\r\n" + cmd.command + "\r\n"
  }

  def commandStr[A <: Command](a: A)(implicit ev: BuildCommand[A]): String = {
    ev.commandStr(a)
  }

  implicit val hashSetCommandImpl = new BuildCommand[Hset] {
    def commandStr(cmd: Hset): String = {
      val arrayNum = numArray(3)
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
      s"${numArray(2)}${command(cmd)}${cmd.key.toRedisStr}${RedisStr(cmd.field).toRedisStr}"
    }
  }
}
