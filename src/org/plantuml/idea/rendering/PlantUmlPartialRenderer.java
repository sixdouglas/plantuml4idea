package org.plantuml.idea.rendering;

import com.intellij.openapi.diagnostic.Logger;
import net.sourceforge.plantuml.*;
import net.sourceforge.plantuml.core.UmlSource;
import net.sourceforge.plantuml.error.PSystemError;
import net.sourceforge.plantuml.error.PSystemErrorV2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.plantuml.idea.rendering.PlantUmlRendererUtil.getTitles;
import static org.plantuml.idea.rendering.PlantUmlRendererUtil.zoomDiagram;
import static org.plantuml.idea.util.Utils.newSourceStringReader;

public class PlantUmlPartialRenderer extends PlantUmlNormalRenderer {
    private static final Logger logger = Logger.getInstance(PlantUmlPartialRenderer.class);


    @NotNull
    public RenderResult partialRender(RenderRequest renderRequest, @Nullable RenderCacheItem cachedItem, long start, String[] sourceSplit) {
        try {
            FileFormatOption formatOption = new FileFormatOption(renderRequest.getFormat().getFormat());

            RenderResult renderResult = new RenderResult(RenderingType.PARTIAL, sourceSplit.length);
            for (int page = 0; page < sourceSplit.length; page++) {
                processPage(renderRequest, cachedItem, sourceSplit[page], formatOption, renderResult, page);
            }

            logger.debug("partial rendering done ", System.currentTimeMillis() - start, "ms");
            return renderResult;
        } catch (PartialRenderingException e) {
            logger.debug(e);
            return renderError(renderRequest, e);
        }
    }
    @NotNull
    protected RenderResult renderError(RenderRequest renderRequest, PartialRenderingException e) {
        RenderResult renderResult = new RenderResult(RenderingType.PARTIAL, 1);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            LineLocationImpl lineLocation = new LineLocationImpl("", null);
            StringLocated o = new StringLocated("", lineLocation);
            List<StringLocated> data = Collections.singletonList(o);
            UmlSource source = new UmlSource(data, false);

            ErrorUml singleError = new ErrorUml(ErrorUmlType.EXECUTION_ERROR, e.getMessage(), lineLocation);
            PSystemError pSystemError = new PSystemErrorV2(source, data, singleError);
            pSystemError.exportDiagram(os, 0, new FileFormatOption(FileFormat.PNG));
        } catch (IOException e1) {
            logger.warn(e1);
            throw e;
        }
        renderResult.addRenderedImage(new ImageItem(renderRequest.getBaseDir(), renderRequest.getFormat(), renderRequest.getSource(), null, 0, "(Error)", os.toByteArray(), null, RenderingType.PARTIAL, null, null));
        return renderResult;
    }

    public void processPage(RenderRequest renderRequest, @Nullable RenderCacheItem cachedItem, String s, FileFormatOption formatOption, RenderResult renderResult, int page) {
        long partialPageProcessingStart = System.currentTimeMillis();
        String partialSource = "@startuml\n" + s + "\n@enduml";

        boolean obsolete = cachedItem == null
                || renderRequest.requestedRefreshOrIncludesChanged()
                || RenderingType.PARTIAL.renderingTypeChanged(cachedItem)
                || renderRequest.getZoom() != cachedItem.getZoom()
                || !partialSource.equals(cachedItem.getImagesItemPageSource(page));

        boolean pageSelected = renderRequest.getPage() == -1 || renderRequest.getPage() == page;
        boolean shouldRender = pageSelected && (obsolete || !cachedItem.hasImage(page));

        if (shouldRender) {
            renderResult.addRenderedImage(renderImage(renderRequest, page, formatOption, partialSource));
        } else if (obsolete) {
            renderResult.addUpdatedTitle(updateTitle(renderRequest, page, partialSource));
        } else {
            logger.debug("page ", page, " cached");
            renderResult.addCachedImage(cachedItem.getImageItem(page));
        }
        logger.debug("processing of page ", page, " done in ", System.currentTimeMillis() - partialPageProcessingStart, "ms");
    }

    private ImageItem updateTitle(RenderRequest renderRequest, int page, String partialSource) {
        long start = System.currentTimeMillis();
        logger.debug("updating title, page ", page);

        SourceStringReader reader = newSourceStringReader(partialSource, renderRequest.isUseSettings());
        String title = getTitle(reader);
        ImageItem imageItem = new ImageItem(renderRequest.getBaseDir(), renderRequest.getFormat(), renderRequest.getSource(), partialSource, page, TITLE_ONLY, null, null, RenderingType.PARTIAL, title, null);

        logger.debug("updateTitle " + (System.currentTimeMillis() - start));

        return imageItem;
    }

    private String getTitle(SourceStringReader reader) {
        DiagramInfo.Titles titles = getTitles(1, reader.getBlocks());
        if (titles.size() > 1) {
            throw new PartialRenderingException();
        }
        return titles.get(0);
    }


    private ImageItem renderImage(RenderRequest renderRequest, int page, FileFormatOption formatOption, String partialSource) {
        logger.debug("rendering partially, page ", page);
        SourceStringReader reader = newSourceStringReader(partialSource, renderRequest.isUseSettings());
        DiagramInfo info = zoomDiagram(reader, renderRequest.getZoom());
        Integer totalPages = info.getTotalPages();
        DiagramInfo.Titles titles = info.getTitles();

        if (totalPages > 1) {
            throw new PartialRenderingException();
        }
        if (titles.size() > 1) {
            logger.warn("too many titles " + titles + ", partialSource=" + partialSource);
        }
        try {
            ImageItem item = generateImageItem(renderRequest, renderRequest.getSource(), partialSource, reader, formatOption, 0, page, RenderingType.PARTIAL, titles.get(0), info.getFilename());
            return new ImageItem(page, item, renderRequest.getFormat());
        } catch (RenderingCancelledException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


}
