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

package com.strad.free.redis.client
import com.strad.free.redis.command.{ Command, Hset, RedisLong, RedisStr }
import com.strad.free.redis.parser.{ Error, RedisParser, RedisResponse }
import fastparse.all._
import freestyle._
import freestyle.implicits._
import java.io._
import java.net.{ InetAddress, InetSocketAddress, Socket }
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

case class Connection(s: Socket, writer: PrintWriter, reader: BufferedReader)

@free
trait Instruction {
  def send(c: Connection, cmd: Command): FS[RedisResponse]
  def respond(c: Connection): FS[RedisResponse]
  def open(server: String, port: Int): FS[Connection]
  def close(c: Connection): FS[Unit]
}

object ErrorOrObj {
  sealed trait Error extends Product with Serializable
  case class ParserError(s: String) extends Error
  case class ConnectionError(s: String) extends Error
  type ErrorOr[A] = Either[Error, A]
}

@module
trait RedisApp {
  val redis: Instruction
}

object Main extends App {
  import scala.concurrent.Future
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val redisInstructionHander =
    new Instruction.Handler[Future] {
      def send(c: Connection, cmd: Command): Future[RedisResponse] = {
        Future {
          val sendCmd = cmd.commandStr
          c.writer.println(sendCmd)
          val response = c.reader.readLine() + "\r\n"
          val parsedResult = RedisParser.redisResp.parse(response)
          parsedResult.get.value
        }

      }
      def respond(c: Connection): Future[RedisResponse] = {
        Future {
          val resp = c.reader.readLine() + "\r\n"
          val parsedResult = RedisParser.redisResp.parse(resp)
          parsedResult.get.value
        }
      }
      def open(server: String, port: Int): Future[Connection] = {
        Future {
          val c = new Socket(server, port)
          val outStream = c.getOutputStream
          val out = new PrintWriter(outStream, true)
          val inStream = new InputStreamReader(c.getInputStream)
          val in = new BufferedReader(inStream)
          Connection(c, out, in)
        }
      }
      def close(c: Connection): Future[Unit] = {
        Future {
          c.writer.flush
          c.writer.close
          c.reader.close()
          c.s.close
        }
      }
    }

  val hset = Hset(RedisStr("key"), "myfield", RedisLong(32L))
  def program[F[_]](implicit A: RedisApp[F]) = {
    import A._
    import cats.implicits._
    for {
      c <- redis.open("localhost", 6379)
      c2 <- redis.send(c, hset)
      c3 <- redis.close(c)
    } yield c2
  }
  import cats.implicits._
  import scala.concurrent.duration.Duration
  import scala.concurrent.Await

  val futureValue = program[RedisApp.Op].interpret[Future]
  val res = Await.result(futureValue, Duration.Inf)
  println(res)
}
