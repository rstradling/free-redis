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
  case class Close(c: Connection) extends Instruction[Unit]

  def send(c: Connection, cmd: Command): Free[Instruction, RedisResponse] = Free.liftF(Send(c, cmd))
  def respond(c: Connection): Free[Instruction, RedisResponse] = Free.liftF(Respond(c))
  def open(server: String, port: Int): Free[Instruction, Connection] = Free.liftF(Open(server, port))
  def close(c: Connection): Free[Instruction, Unit] = Free.liftF(Close(c))

}

trait SocketInterface {
  def send[A: BuildCommand](c: Connection.Connection, cmd: A): RedisResponse
  def respond(c: Connection.Connection): RedisResponse
  def open(server: String, port: Int): Connection.Connection
  def close(c: Connection.Connection): Unit
}

object Impl extends SocketInterface {
  def send[A: BuildCommand](c: Connection.Connection, cmd: A): RedisResponse = {
    val sendCmd = implicitly[BuildCommand[A]].commandStr(cmd)
    c.writer.println(sendCmd)
    val response = c.reader.readLine() + "\r\n"
    val parsedResult = RedisParser.redisResp.parse(response)
    parsedResult.get.value

  }
  def respond(c: Connection.Connection): RedisResponse = {
    val resp = c.reader.readLine() + "\r\n"
    val parsedResult = RedisParser.redisResp.parse(resp)
    parsedResult.get.value
  }
  def open(server: String, port: Int): Connection.Connection = {
    val c = new Socket(server, port)
    val outStream = c.getOutputStream
    val out = new PrintWriter(outStream, true)
    val inStream = new InputStreamReader(c.getInputStream)
    val in = new BufferedReader(inStream)
    Connection.Connection(c, out, in)
  }
  def close(c: Connection.Connection): Unit = {
    c.writer.flush
    c.writer.close
    c.reader.close()
    c.s.close
  }
}

object ConnInterpreter extends (Connection.Instruction ~> Id) {
  import com.strad.free.redis.command.RedisCommand.cmd
  override def apply[A](fa: Connection.Instruction[A]): Id[A] = fa match {
    case Connection.Send(c, s) => Impl.send(c, s)
    case Connection.Respond(c) => Impl.respond(c)
    case Connection.Open(server, port) => Impl.open(server, port)
    case Connection.Close(c) => Impl.close(c)
  }
}

object Main extends App {
  def run(): RedisResponse = {

    val hset = Hset(RedisStr("key"), "myfield", RedisLong(32L))
    val p: Free[Connection.Instruction, RedisResponse] =
      for {
        c <- Connection.open("localhost", 6379)
        c2 <- Connection.send(c, hset)
        c3 <- Connection.close(c)
      } yield c2
    p.foldMap(ConnInterpreter)
  }
  val res = run()
  println(res)
}
