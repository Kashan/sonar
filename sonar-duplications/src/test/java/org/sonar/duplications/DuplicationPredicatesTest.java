/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.duplications;

import com.google.common.base.Predicate;
import org.junit.Test;
import org.sonar.duplications.index.CloneGroup;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DuplicationPredicatesTest {

  @Test
  public void testNumberOfUnitsNotLessThan() {
    Predicate<CloneGroup> predicate = DuplicationPredicates.numberOfUnitsNotLessThan(5);
    assertThat(predicate.apply(CloneGroup.builder().setLengthInUnits(6).build()), is(true));
    assertThat(predicate.apply(CloneGroup.builder().setLengthInUnits(5).build()), is(true));
    assertThat(predicate.apply(CloneGroup.builder().setLengthInUnits(4).build()), is(false));
  }

}
