package com.karateca.jstoolbox.torelated;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiManager;
import com.karateca.jstoolbox.MyAction;
import com.karateca.jstoolbox.config.JsToolboxSettings;

import java.util.Arrays;
import java.util.List;

/**
 * @author Andres Dominguez.
 */
abstract class GoToRelatedAction extends MyAction {

  abstract List<String> getDestinationSuffixList();

  List<String> fileSuffixList;
  List<String> viewSuffixList;
  List<String> testSuffixList;

  @Override
  public void actionPerformed(AnActionEvent e) {
    // Disable if dependencies are not met.
    if (!canEnableAction(e)) {
      return;
    }

    readConfig();

    String fileName = getCurrentFileName(e);

    // Is it at destination? Go back to file.
    if (isInDestinationFile(fileName)) {
      goToFiles(e, getDestinationMatch(fileName), fileSuffixList);
    } else if (isInCodeFile(fileName)) {
      // If I'm in file then go to destination.
      goToFiles(e, getCodeFileMatch(fileName), getDestinationSuffixList());
    } else if (isViewFile(fileName)) {
      // Go from view to test.
      goToFiles(e, getViewSuffixMatch(fileName), testSuffixList);
    } else if (isTestFile(fileName)) {
      // Go from test to view.
      goToFiles(e, getTestSuffixMatch(fileName), viewSuffixList);
    }
  }

  private void readConfig() {
    JsToolboxSettings settings = new JsToolboxSettings();

    fileSuffixList = Arrays.asList(settings.getFileSuffix().split(","));
    viewSuffixList = Arrays.asList(settings.getViewSuffix().split(","));
    testSuffixList = Arrays.asList(settings.getTestSuffix().split(","));
  }

  String getDestinationMatch(String fileName) {
    return findMatch(fileName, getDestinationSuffixList());
  }

  String getCodeFileMatch(String fileName) {
    return findMatch(fileName, fileSuffixList);
  }

  String getTestSuffixMatch(String fileName) {
    return findMatch(fileName, testSuffixList);
  }

  String getViewSuffixMatch(String fileName) {
    return findMatch(fileName, viewSuffixList);
  }

  boolean isInCodeFile(String fileName) {
    return !isTestFile(fileName) && !isViewFile(fileName);
  }

  boolean isViewFile(String fileName) {
    return endsWithAnyOf(fileName, viewSuffixList);
  }

  boolean isTestFile(String fileName) {
    return endsWithAnyOf(fileName, testSuffixList);
  }

  boolean isInDestinationFile(String fileName) {
    return endsWithAnyOf(fileName, getDestinationSuffixList());
  }

  private String findMatch(String fileName, List<String> suffixList) {
    for (String suffix : suffixList) {
      if (fileName.endsWith(suffix)) {
        return suffix;
      }
    }
    return null;
  }

  private boolean endsWithAnyOf(String fileName, List<String> destinationSuffix) {
    for (String suffix : destinationSuffix) {
      if (fileName.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  void goToFiles(AnActionEvent e, String fromSuffix, List<String> toSuffixes) {
    String fileName = getCurrentFileName(e);

    for (String suffix : toSuffixes) {
      String goToFileName = fileName.replace(fromSuffix, suffix);

      openFileInEditor(goToFileName, e.getProject());
    }
  }

  void openFileInEditor(String findFileName, Project project) {
    ContentIterator fileIterator = new FindRelatedFileIterator(findFileName, PsiManager.getInstance(
        project));

    ProjectRootManager.getInstance(project).getFileIndex().iterateContent(fileIterator);
  }
}
