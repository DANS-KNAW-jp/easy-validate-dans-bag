/**
 * Copyright (C) 2018 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.validatebag.rules

import better.files._
import nl.knaw.dans.easy.validatebag.TestSupportFixture
import nl.knaw.dans.easy.validatebag.validation.RuleViolationException
import nl.knaw.dans.lib.error.CompositeException

import scala.util.Failure

class CheckBagSpec extends TestSupportFixture {
  private def expectUnreadable(unreadables: File*)(file: File): Boolean = {
    val readable = !unreadables.contains(file) && file.isReadable
    debug(s"Returning readable = $readable for $file")
    readable
  }

  "checkBag" should "fail if bag directory is not found" in {
    checkBag(bagsDir / "non-existent", 0) should matchPattern {
      case Failure(_: IllegalArgumentException) =>
    }
  }

  it should "fail if the bag directory is unreadable" in {
    val minimal = bagsDir / "minimal"
    val result = checkBag(minimal, 0)(expectUnreadable(minimal))
    result shouldBe a[Failure[_]]
    inside(result) {
      case Failure(iae: IllegalArgumentException) =>
        iae.getMessage should include("non-readable")
    }
  }

  it should "fail if there is a non-readable file in the bag directory" in {
    val minimal = bagsDir / "minimal"
    val leegTxt = minimal / "data/leeg.txt"
    inside(checkBag(minimal, 0)(expectUnreadable(leegTxt))) {
      case Failure(iae: IllegalArgumentException) =>
        iae.getMessage should include("non-readable")
    }
  }

  it should "fail if bag does not contain bag-info.txt" in {
    inside(checkBag(bagsDir / "missing-bag-info.txt", 0)) {
      case Failure(CompositeException(List(rve: RuleViolationException))) =>
        // This also checks that there is only one rule violation.
        debug(s"Error message: ${ rve.getMessage }")
        rve.getMessage should include("Mandatory file 'bag-info.txt'")
    }
  }
}
