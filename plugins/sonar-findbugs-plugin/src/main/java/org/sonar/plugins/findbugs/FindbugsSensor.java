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
package org.sonar.plugins.findbugs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;

import java.io.File;
import java.util.List;

public class FindbugsSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(FindbugsSensor.class);

  private RulesProfile profile;
  private RuleFinder ruleFinder;
  private FindbugsExecutor executor;

  public FindbugsSensor(RulesProfile profile, RuleFinder ruleFinder, FindbugsExecutor executor) {
    this.profile = profile;
    this.ruleFinder = ruleFinder;
    this.executor = executor;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.getFileSystem().hasJavaSourceFiles()
        && !profile.getActiveRulesByRepository(FindbugsConstants.REPOSITORY_KEY).isEmpty();
  }

  public void analyse(Project project, SensorContext context) {
    if (project.getReuseExistingRulesConfig()) {
      LOG.warn("Reusing existing Findbugs configuration not supported any more.");
    }
    File report = getFindbugsReportFile(project);
    if (report == null) {
      report = executor.execute();
    }
    FindbugsXmlReportParser reportParser = new FindbugsXmlReportParser(report);
    List<FindbugsXmlReportParser.XmlBugInstance> bugInstances = reportParser.getBugInstances();

    for (FindbugsXmlReportParser.XmlBugInstance bugInstance : bugInstances) {
      FindbugsXmlReportParser.XmlSourceLineAnnotation sourceLine = bugInstance.getPrimarySourceLine();
      if (sourceLine == null) {
        LOG.warn("No source line for " + bugInstance.getType());
        continue;
      }

      Rule rule = ruleFinder.findByKey(FindbugsConstants.REPOSITORY_KEY, bugInstance.getType());
      if (rule == null) {
        // ignore violations from report, if rule not activated in Sonar
        LOG.warn("Findbugs rule '{}' not active in Sonar.", bugInstance.getType());
        continue;
      }

      JavaFile resource = new JavaFile(sourceLine.getSonarJavaFileKey());
      if (context.getResource(resource) != null) {
        Violation violation = Violation.create(rule, resource)
            .setLineId(sourceLine.getStart())
            .setMessage(bugInstance.getLongMessage());
        context.saveViolation(violation);
      }
    }
  }

  protected final File getFindbugsReportFile(Project project) {
    if (project.getConfiguration().getString(CoreProperties.FINDBUGS_REPORT_PATH) != null) {
      return new File(project.getConfiguration().getString(CoreProperties.FINDBUGS_REPORT_PATH));
    }
    return null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
