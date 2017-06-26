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

import org.scalatest._

class RedisCommandSpec extends FlatSpec with Matchers {
  "hashSet" should "return the proper redis string" in {
    val hset = Hset(RedisStr("key"), "myfield", RedisLong(32L))
    hset.commandStr shouldBe "*4\r\n$4\r\nHSET\r\n$3\r\nkey\r\n$7\r\nmyfield\r\n$2\r\n32\r\n"
  }
  "hashGet" should "return the proper redis string" in {
    val hget = Hget(RedisStr("key"), "myfield")
    hget.commandStr shouldBe "*3\r\n$4\r\nHGET\r\n$3\r\nkey\r\n$7\r\nmyfield\r\n"
  }
}
