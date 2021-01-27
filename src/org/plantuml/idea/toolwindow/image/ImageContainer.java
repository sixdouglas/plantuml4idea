package org.plantuml.idea.toolwindow.image;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DataProvider;
import org.plantuml.idea.rendering.ImageItem;
import org.plantuml.idea.rendering.RenderRequest;

import java.awt.*;
import java.util.List;

public interface ImageContainer extends DataProvider, Disposable {
    public static final DataKey<Component> CONTEXT_COMPONENT = DataKey.create("ImageContainer");

    int getPage();

    RenderRequest getRenderRequest();

    ImageItem getImageItem();

    Image getPngImage();

    void highlight(List<String> list);
}
