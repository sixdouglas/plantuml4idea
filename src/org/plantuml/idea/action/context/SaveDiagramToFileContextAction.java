package org.plantuml.idea.action.context;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import org.plantuml.idea.action.save.AbstractSaveDiagramAction;
import org.plantuml.idea.toolwindow.image.ImageContainerPng;

public class SaveDiagramToFileContextAction extends AbstractSaveDiagramAction {

    public SaveDiagramToFileContextAction() {
        super("Save Current Diagram", "Save Current Diagram", AllIcons.Actions.Menu_saveall);
    }

    @Override
    protected int getPageNumber(AnActionEvent e) {
        ImageContainerPng data = (ImageContainerPng) e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
        return data.getPage();
    }

}
