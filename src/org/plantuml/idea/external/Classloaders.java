package org.plantuml.idea.external;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.plantuml.idea.lang.settings.PlantUmlSettings;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Classloaders {

    private static final Logger LOG = Logger.getInstance(Classloaders.class);

    private static ClassLoader bundled;
    private static String customPlantumlJarPath;
    private static ClassLoader custom;


    private static ClassLoader getClassloader() {
        PlantUmlSettings settings = PlantUmlSettings.getInstance();
        String customPlantumlJarPath = settings.getCustomPlantumlJarPath();
        if (settings.isUseBundled() || StringUtils.isBlank(customPlantumlJarPath)) {
            return getBundled();
        } else {
            return Classloaders.getCustomClassloader(customPlantumlJarPath);
        }
    }

    public static ClassLoader getBundled() {
        if (bundled == null) {
            File pluginHome = getPluginHome();

            ArrayList<File> jarFiles = new ArrayList<>();
            File[] jars = pluginHome.listFiles((dir, name) -> !name.equals("plantuml4idea.jar"));
            if (jars != null) {
                if (!isDev() && jars.length < 2) {
                    throw new RuntimeException("Invalid installation. Install the whole zip file! Should find at least 2 jars, but found only: " + Arrays.toString(jars));
                }
                jarFiles.addAll(Arrays.asList(jars));
            }
            if (isDev()) {
                File file = new File("lib/plantuml");
                if (!file.exists()) {
                    throw new RuntimeException(file.getAbsolutePath());
                }
                File[] files = file.listFiles();
                if (files != null) {
                    jarFiles.addAll(Arrays.asList(files));
                }
            }


            bundled = classLoader(jarFiles);
        }
        return bundled;
    }

    public static ClassLoader getCustomClassloader(String customPlantumlJarPath) {
        if (Objects.equals(Classloaders.customPlantumlJarPath, customPlantumlJarPath) && custom != null) {
            return custom;
        }

        List<File> jars = new ArrayList<>();
        try {
            Classloaders.customPlantumlJarPath = customPlantumlJarPath;
            jars.add(new File(customPlantumlJarPath));
            jars.add(new File(getPluginHome(), "adapter.jar"));
            custom = classLoader(jars);
            return custom;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static ClassLoader classLoader(List<File> jarFiles) {
        URL[] urls = new URL[jarFiles.size()];
        for (int i = 0; i < jarFiles.size(); i++) {
            File jarFile = jarFiles.get(i);
            if (!jarFile.exists()) {
                throw new IllegalStateException("Plugin jar file not found: " + jarFile.getAbsolutePath());
            }
            try {
                urls[i] = jarFile.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            LOG.info("Creating classloader for " + Arrays.toString(urls));
            //parent loads bundled plantuml, it would conflict with default parent first classloader
            return new ParentLastURLClassLoader(Classloaders.class.getClassLoader(), urls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static File getPluginHome() {
        File pluginHome = null;
        if (isDev()) {
            pluginHome = new File("lib/");
        } else {
            pluginHome = new File(PathManager.getPluginsPath(), "plantuml4idea/lib/");
            File preInstalled = new File(PathManager.getPreInstalledPluginsPath(), "plantuml4idea/lib/");
            if (!pluginHome.exists() && preInstalled.exists()) {
                pluginHome = preInstalled;
            }
        }
        return pluginHome;
    }

    private static boolean isDev() {
        return ApplicationManager.getApplication() == null || ApplicationManager.getApplication().isUnitTestMode();
    }

    @NotNull
    static PlantUmlFacade getAdapter() {
        return getAdapter(getClassloader());
    }

    @NotNull
    public static PlantUmlFacade getAdapter(ClassLoader classloader) {
        try {
            return (PlantUmlFacade) Class.forName("org.plantuml.idea.adapter.FacadeImpl", true, classloader).getConstructor().newInstance();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    static PlantUmlFacade getPlantUmlRendererUtil() {
        return getAdapter();
    }

    static PlantUmlFacade getAnnotator() {
        return getAdapter();
    }
}