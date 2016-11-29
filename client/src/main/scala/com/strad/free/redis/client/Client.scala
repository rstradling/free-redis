package com.strad.free.redis.client

import cats.free.Free
import cats.{ Id, ~> }
import com.strad.free.redis.command.{ BuildCommand, Command, Hset, RedisCommand, RedisLong, RedisStr }
import com.strad.free.redis.parser.{ RedisParser, RedisResponse }
import fastparse.all._
import java.io._
import java.net.{ InetAddress, InetSocketAddress, Socket }
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

object Connection {
  case class Connection(s: Socket, writer: PrintWriter, reader: BufferedReader)

  sealed trait Instruction[A]
  case class Send(c: Connection, cmd: Command) extends Instruction[RedisResponse]
  case class Respond(c: Connection) extends Instruction[RedisResponse]
  case class Open(server: String, port: Int) extends Instruction[Connection]

  def send(c: Connection, cmd: Command): Free[Instruction, RedisResponse] = Free.liftF(Send(c, cmd))
  def respond(c: Connection): Free[Instruction, RedisResponse] = Free.liftF(Respond(c))
  def open(server: String, port: Int): Free[Instruction, Connection] = Free.liftF(Open(server, port))

}

trait SocketInterface {
  def send[B](c: Connection.Connection, cmd: Command)(implicit ev: BuildCommand[B]): RedisResponse
  def respond(c: Connection.Connection): RedisResponse
  def open(server: String, port: Int): Connection.Connection
}

object Impl extends SocketInterface {
  def send[B <: Command](c: Connection.Connection, cmd: Command)(implicit ev: BuildCommand[B]): RedisResponse = {
    c.writer.println(RedisCommand.commandStr(cmd)(ev))
    val response = c.reader.readLine()
    val parsedResult = RedisParser.redisResp.parse(response)
    println(s"response = $parsedResult")
    parsedResult.get.value

  }
  def respond(c: Connection.Connection): RedisResponse = {
    val resp = c.reader.readLine()
    println(s"Response = $resp")
    val parsedResult = RedisParser.redisResp.parse(resp)
    parsedResult.get.value
  }
  def open(server: String, port: Int): Connection.Connection = {
    val c = new Socket(server, port)
    val outStream = c.getOutputStream
    val out = new PrintWriter(outStream, true)
    val inStream = new InputStreamReader(c.getInputStream)
    val in = new BufferedReader(inStream)
    println(c)
    Connection.Connection(c, out, in)
  }
}

object ConnInterpreter extends (Connection.Instruction ~> Id) {
  override def apply[A, B <: Command](fa: Connection.Instruction[A])(implicit ev: BuildCommand[B]): Id[A] = fa match {
    case Connection.Send(c, s) => Impl.send(c, s)(ev)
    case Connection.Respond(c) => Impl.respond(c)
    case Connection.Open(server, port) => Impl.open(server, port)
  }
}

object Main {
  def run(): Unit = {

    val hset = Hset(RedisStr("key"), "myfield", RedisLong(32L))
    val p: Free[Connection.Instruction, Unit] =
      for {
        c <- Connection.open("localhost", 6379)
        c2 <- Connection.send(c, hset)
      } yield ()
    p.foldMap(ConnInterpreter)
  }
}
